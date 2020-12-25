package ru.vas.resourceservice.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.Message;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
@EnableIntegration
@Slf4j
public class FileToObjectFlow {

    @Bean
    public IntegrationFlow fileReadingFlow(@Value("${resource-service.registry.file-location}") String path) {
        return IntegrationFlows
                .from(Files.inboundAdapter(new File(path))
                                .patternFilter("*.csv"),
                        e -> e.poller(Pollers.fixedDelay(Duration.ofSeconds(1))))
                .log(LoggingHandler.Level.DEBUG)
                .transform(Files.toStringTransformer())
                .handle(m -> System.out.println("HANDLE" + m.getHeaders()), endpointSpec -> endpointSpec.advice(removingAndRenamingAdvice()))
                .get();
    }

    @Bean
    public AbstractRequestHandlerAdvice removingAndRenamingAdvice() {
        return new AbstractRequestHandlerAdvice() {
            @Value("${resource-service.registry.success-location}")
            private String successDestination;
            @Value("${resource-service.registry.fail-location}")
            private String failDestination;

            @Override
            protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
                File file = message.getHeaders().get(FileHeaders.ORIGINAL_FILE, File.class);
                final String tmpFileName = getTmpFileName(file.getName(), message);
                message.getHeaders().getId();
                boolean isSuccess = false;
                log.info(String.format("Обработка файла '%s'", file.getAbsolutePath()));
                try {
                    Object result = callback.execute();
                    isSuccess = true;
                    file.renameTo(new File(file.getParent() + successDestination, tmpFileName));
                    return result;
                }
                catch (Exception e) {
                    isSuccess = false;
                    file.renameTo(new File(file.getParent() + failDestination, tmpFileName));
                    throw e;
                }
                finally {
                    log.info(String.format("Файл обработан %s и перемещен в '%s'. Message GUID: %s",
                            isSuccess ? "успешно" : "с ошибкой",
                            isSuccess ? successDestination : failDestination,
                            message.getHeaders().getId()));
                }
            }

            private String getTmpFileName(String sourceFileName, Message<?> message) {
                return sourceFileName
                        .concat("_")
                        .concat(message.getHeaders().getId().toString())
                        .concat("(")
                        .concat(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        .concat(").tmp");
            }
        };
    }
}
