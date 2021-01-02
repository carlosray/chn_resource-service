package ru.vas.resourceservice.schedule.download;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.vas.resourceservice.service.DownloadService;
import ru.vas.resourceservice.service.UpdateService;

import java.io.File;
import java.io.IOException;
import java.time.*;

@ConditionalOnProperty(value = "resource-service.download.enable", matchIfMissing = true, havingValue = "true")
@Component("downloadScheduler")
@Slf4j
@RequiredArgsConstructor
public class DownloadRegistryScheduler {
    private final UpdateService updateService;
    private final DownloadService downloadService;
    @Value("#{T(java.time.Duration).parse('${resource-service.registry.download-delay}')}")
    @Getter
    private Duration downloadDelay;

    @Scheduled(fixedDelayString = "#{downloadScheduler.downloadDelay.toMillis()}")
    void download() throws IOException {
        boolean needUpdate = updateService.isNeedUpdate();
        log.info("Проверка обновлений: нужно ли обновление? " + (needUpdate ? "Да" : "Нет"));
        if (needUpdate) {
            File downloaded = downloadService.downloadFile();
            log.info("Скачан файл: " + downloaded.getAbsolutePath());
        }
    }
}
