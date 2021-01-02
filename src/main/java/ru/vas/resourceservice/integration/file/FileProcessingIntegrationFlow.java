package ru.vas.resourceservice.integration.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.EmitUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.dsl.*;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;
import ru.vas.resourceservice.model.BlockedResource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.springframework.integration.IntegrationMessageHeaderAccessor.CORRELATION_ID;
import static org.springframework.integration.IntegrationMessageHeaderAccessor.SEQUENCE_SIZE;

@Configuration
@EnableIntegration
@Slf4j
public class FileProcessingIntegrationFlow {

    private Executor executor() {
        return Executors.newFixedThreadPool(10);
    }

    @Bean(name = "inputSplitFileChannel")
    MessageChannel inputSplitFileChannel() {
        return MessageChannels.publishSubscribe().get();
    }

    @Bean("renamingChannel")
    MessageChannel renamingChannel() {
        return MessageChannels.publishSubscribe(this.executor()).get();
    }

    @Bean
    public IntegrationFlow fileReadingFlow(@Value("${resource-service.registry.file-location}") String blockedDir, ProcessFileLines processFileLines) {
        return IntegrationFlows
                .from(Files.inboundAdapter(new File(blockedDir))
                                .patternFilter("*.csv"),
                        e -> e.poller(Pollers.fixedDelay(Duration.ofSeconds(1))))
                .log(LoggingHandler.Level.INFO, logStartProcessing())
                .handle(processFileLines, "process")
                .get();
    }

    private Function<Message<File>, Object> logStartProcessing() {
        return message -> String.format(
                "MessageID: '%s'. Обработка файла '%s'",
                message.getHeaders().getId(),
                message.getHeaders().get(FileHeaders.ORIGINAL_FILE, File.class).getAbsolutePath());
    }

    @MessagingGateway
    @Component
    interface ProcessFileLines {
        @Gateway(requestChannel = "inputSplitFileChannel")
        void process(File file);
    }

    @Bean
    public IntegrationFlow fileProcessingFlow(@Value("${resource-service.registry.file-location}") String blockedDir) {
        return IntegrationFlows
                .from(inputSplitFileChannel())
                .enrichHeaders(h -> h
                        .header("startProcessingTime", LocalDateTime.now())
                        .headerFunction(SEQUENCE_SIZE, countOfLines())
                        .headerFunction(CORRELATION_ID, m -> m.getHeaders().getId()))
                .split(Files.splitter(true).charset(Charset.defaultCharset()))
                .channel(MessageChannels.executor(this.executor()))
                .<String>filter(p -> p.split(BlockedResource.Delimiters.VERT_LINE.getValue()).length < 5)
                .<String, BlockedResource>transform(p -> new BlockedResource(Thread.currentThread().toString(), p))
                .handle((p, h) -> {
                    log.info("ETTETETE " + p.toString() + " | " + h.get(CORRELATION_ID));
                    return p;
                })
                .transform(Message.class, m -> m.getHeaders().getId().toString())
                .aggregate(aggregatorSpecConfig())
                .channel(renamingChannel())
                .get();
    }

    private Function<Message<File>, Long> countOfLines() {
        return message -> {
            try (FileReader input = new FileReader(message.getPayload());
                 LineNumberReader count = new LineNumberReader(input)) {
                while (count.skip(Long.MAX_VALUE) > 0) {
                    //skipping
                }
                return (long) (count.getLineNumber() + 1);
            } catch (IOException e) {
                log.error("Ошибка подсчета кол-ва строк в файле", e);
            }
            return 0L;
        };
    }

    private Consumer<AggregatorSpec> aggregatorSpecConfig() {
        return aggregatorSpec -> aggregatorSpec
                .correlationStrategy(message -> message.getHeaders().get(CORRELATION_ID))
                .releaseStrategy(messageGroup -> messageGroup.size() == messageGroup.getSequenceSize())
                .expireGroupsUponCompletion(true)
                .groupTimeout(TimeUnit.SECONDS.toMillis(5))
                .sendPartialResultOnExpiry(true);
    }

    @Bean
    public IntegrationFlow renamingFlow(@Value("${resource-service.registry.processed-location}") String processedDir) {
        return IntegrationFlows.from(renamingChannel())
                .handle(message -> {
                    final LocalDateTime startProcessingTime = message.getHeaders().get("startProcessingTime", LocalDateTime.class);
                    final Duration duration = Optional.ofNullable(startProcessingTime)
                            .map(startTime -> Duration.between(startTime, LocalDateTime.now()))
                            .orElse(null);
                    File file = message.getHeaders().get(FileHeaders.ORIGINAL_FILE, File.class);
                    String fileMessageGuid = message.getHeaders()
                            .getOrDefault(CORRELATION_ID, message.getHeaders().getId())
                            .toString();
                    final String tmpFileName = getTmpFileName(file.getName(), fileMessageGuid);
                    File destinationDir = Paths.get(file.getParent() + processedDir).toFile();
                    destinationDir.mkdirs();
                    File destination = new File(destinationDir, tmpFileName);
                    boolean isSuccess = file.renameTo(destination);
                    log.info(String.format("Файл %s в '%s'. FileMessage GUID: %s%s",
                            isSuccess ? "перемещен" : "не(!) перемещен",
                            processedDir,
                            fileMessageGuid,
                            Objects.nonNull(duration) ? ". Время обработки: " + duration : ""));
                })
                .get();
    }

    private String getTmpFileName(String sourceFileName, String id) {
        return sourceFileName
                .concat("_")
                .concat(id)
                .concat("(")
                .concat(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .concat(").tmp");
    }

    @Bean
    public IntegrationFlow errorFlow() {
        return IntegrationFlows.from(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)
                .handle(m -> log.error("Ошибка в Integration Flow {}", m))
                .get();
    }
}
