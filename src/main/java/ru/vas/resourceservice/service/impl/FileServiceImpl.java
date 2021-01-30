package ru.vas.resourceservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.file.FileHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import ru.vas.resourceservice.model.FileProcessingInfo;
import ru.vas.resourceservice.service.FileService;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.springframework.integration.IntegrationMessageHeaderAccessor.CORRELATION_ID;

@Service
@Slf4j
public class FileServiceImpl implements FileService {
    @Value("${resource-service.flow.file-processing.headers.start-processing-time}")
    private String startTimeHeader;

    @Override
    public long countOfLines(File file) {
        try (FileReader input = new FileReader(file);
             LineNumberReader count = new LineNumberReader(input)) {
            while (count.skip(Long.MAX_VALUE) > 0) {
                //skipping
            }
            return count.getLineNumber() + 1;
        } catch (IOException e) {
            log.error("Ошибка подсчета кол-ва строк в файле", e);
        }
        return 0;
    }

    @Override
    public FileProcessingInfo renameFile(Message<?> message, String processedDir) {
        File file = message.getHeaders().get(FileHeaders.ORIGINAL_FILE, File.class);
        String fileMessageGuid = message.getHeaders()
                .getOrDefault(CORRELATION_ID, message.getHeaders().getId())
                .toString();
        File destinationDir = Paths.get(file.getParent() + processedDir).toFile();
        destinationDir.mkdirs();
        File destination = new File(destinationDir, getTmpFileName(file.getName(), fileMessageGuid));
        boolean isSuccess = file.renameTo(destination);
        final Duration duration = processingTime(message).orElse(null);
        return FileProcessingInfo.builder()
                .duration(duration)
                .fileMessageGuid(fileMessageGuid)
                .isSuccess(isSuccess)
                .build();
    }

    private String getTmpFileName(String sourceFileName, String id) {
        return sourceFileName
                .concat("_")
                .concat(id)
                .concat(".tmp");
    }

    private Optional<Duration> processingTime(Message<?> message) {
        final LocalDateTime startProcessingTime = message.getHeaders().get(startTimeHeader, LocalDateTime.class);
        return Optional.ofNullable(startProcessingTime)
                .map(startTime -> Duration.between(startTime, LocalDateTime.now()));
    }
}
