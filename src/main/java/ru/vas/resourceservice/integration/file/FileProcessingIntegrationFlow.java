package ru.vas.resourceservice.integration.file;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.converter.KafkaMessageHeaders;
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
    @Value("${resource-service.flow.file-processing.headers.start-processing-time}")
    private String startTimeHeader;

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
    public IntegrationFlow fileReadingFlow(@Value("${resource-service.registry.file-location}") String blockedDir,
                                           ProcessFileLines processFileLines,
                                           @Value("${resource-service.registry.polling-delay}") Duration delay) {
        return IntegrationFlows
                .from(Files.inboundAdapter(new File(blockedDir), LastModifiedFileComparator.LASTMODIFIED_COMPARATOR)
                                .patternFilter("*.csv")
                                .scanEachPoll(true)
                                .preventDuplicates(true),
                        e -> e.poller(Pollers.fixedDelay(delay)))
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

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    public IntegrationFlow fileProcessingFlow(ProducerFactory<String, BlockedResource> producerFactory, @Value("${spring.kafka.template.default-topic}") String topic) {
        return IntegrationFlows
                .from(inputSplitFileChannel())
                .enrichHeaders(h -> h
                        .headerFunction(startTimeHeader, m -> LocalDateTime.now())
                        .headerFunction(SEQUENCE_SIZE, countOfLines())
                        .headerFunction(CORRELATION_ID, m -> m.getHeaders().getId()))
                .split(Files.splitter(true).charset(Charset.forName("windows-1251")))
                .channel(MessageChannels.executor(this.executor()))
                .<String>filter(p -> p.split(BlockedResource.Delimiters.SEMICOLON.getValue()).length == 6)
                .<String, BlockedResource>transform(BlockedResource::new)
                .handle(Kafka.outboundChannelAdapter(producerFactory)
                        .topic(topic)
                        .sendSuccessChannel(KafkaMessageHeaders.REPLY_CHANNEL)
                        .sendFailureChannel(KafkaMessageHeaders.ERROR_CHANNEL)
                        .<BlockedResource>messageKey(m -> m.getHeaders().getId().toString()))
                .channel(KafkaMessageHeaders.REPLY_CHANNEL)
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
                    File file = message.getHeaders().get(FileHeaders.ORIGINAL_FILE, File.class);
                    String fileMessageGuid = message.getHeaders()
                            .getOrDefault(CORRELATION_ID, message.getHeaders().getId())
                            .toString();
                    File destinationDir = Paths.get(file.getParent() + processedDir).toFile();
                    destinationDir.mkdirs();
                    File destination = new File(destinationDir, getTmpFileName(file.getName(), fileMessageGuid));
                    boolean isSuccess = file.renameTo(destination);
                    final Duration duration = processingTime(message);
                    log.info(String.format("Файл %s в '%s'. FileMessage GUID: %s%s. Обработано строк: %s",
                            isSuccess ? "перемещен" : "не(!) перемещен",
                            processedDir,
                            fileMessageGuid,
                            Objects.nonNull(duration) ? ". Время обработки: " + duration.toMillis() + " мс" : "",
                            message.getHeaders().getOrDefault(SEQUENCE_SIZE, "неизвестно")));
                })
                .get();
    }

    private String getTmpFileName(String sourceFileName, String id) {
        return sourceFileName
                .concat("_")
                .concat(id)
                .concat(".tmp");
    }

    private Duration processingTime(Message<?> message) {
        final LocalDateTime startProcessingTime = message.getHeaders().get(startTimeHeader, LocalDateTime.class);
        return Optional.ofNullable(startProcessingTime)
                .map(startTime -> Duration.between(startTime, LocalDateTime.now()))
                .orElse(null);
    }

    @Bean
    public IntegrationFlow errorFlow() {
        return IntegrationFlows.from(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)
                .handle(m -> log.error("Ошибка в Integration Flow {}", m))
                .get();
    }
}
