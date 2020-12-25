package ru.vas.resourceservice.service.impl;

import ch.qos.logback.core.util.FileSize;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import ru.vas.resourceservice.service.DownloadService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@ConfigurationProperties(prefix = "resource-service.download.git")
@Slf4j
public class GitDownloadService implements DownloadService {
    @Setter
    private URL url;
    @Value("${resource-service.download.git.file-location}")
    private String path;

    private static int BUFFER_SIZE = 1024;

    @Override
    public File downloadFile() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        final long completeFileSize = connection.getContentLength();
        log.info("Скачивание файла ... " + (completeFileSize / FileUtils.ONE_KB) + " Килобайт");
        try (ProgressBar progressBar = new ProgressBar("Прогресс", completeFileSize);
             BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());
             FileOutputStream fos = new FileOutputStream(path);
             BufferedOutputStream outputStream = new BufferedOutputStream(fos, BUFFER_SIZE)) {

            byte[] data = new byte[BUFFER_SIZE];
            int x = 0;
            while ((x = inputStream.read(data, 0, BUFFER_SIZE)) >= 0) {
                outputStream.write(data, 0, x);
                progressBar.stepBy(BUFFER_SIZE);
            }
        }
        return Paths.get(path).toFile();
    }
}
