package ru.vas.resourceservice.schedule.download;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.vas.resourceservice.schedule.ScheduleConfig;
import ru.vas.resourceservice.service.DownloadService;
import ru.vas.resourceservice.service.UpdateService;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(ScheduleConfig.class)
@TestPropertySource
class DownloadRegistrySchedulerTest {

    @SpyBean
    private DownloadRegistryScheduler downloadRegistryScheduler;

    @MockBean
    private UpdateService updateService;
    @MockBean
    private DownloadService downloadService;

    @Test
    void getDownloadDelay_And_NotNeedUpdate() {
        //given
        when(updateService.isNeedUpdate()).thenReturn(false);
        int times = 3;
        //when-then
        await()
                .atMost(downloadRegistryScheduler.getDownloadDelay().toMillis() * times, TimeUnit.MILLISECONDS)
                .untilAsserted(() ->
                    verify(downloadRegistryScheduler, atLeast(times-1)).download());
        verifyNoInteractions(downloadService);
    }

    @Test
    void getDownloadDelay_And_NeedUpdate() throws IOException {
        //given
        lenient().when(updateService.isNeedUpdate()).thenReturn(true);
        final File fileMock = mock(File.class);
        when(downloadService.downloadFile()).thenReturn(fileMock);
        when(fileMock.getAbsolutePath()).thenReturn("test");
        //when-then
        await()
                .atMost(downloadRegistryScheduler.getDownloadDelay().toMillis() * 3, TimeUnit.MILLISECONDS)
                .untilAsserted(() ->
                        verify(downloadRegistryScheduler, atLeast(2)).download());
        verify(downloadService, atLeast(1)).downloadFile();
    }
}