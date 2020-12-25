package ru.vas.resourceservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vas.resourceservice.db.domain.RssUpdate;
import ru.vas.resourceservice.service.rss.LocalRssService;
import ru.vas.resourceservice.service.rss.RemoteRssService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateServiceImplTest {
    @Mock
    private LocalRssService localRssService;
    @Mock
    private RemoteRssService remoteRssService;

    private UpdateServiceImpl updateService;

    @BeforeEach
    void setUp() {
        updateService = new UpdateServiceImpl(localRssService, remoteRssService);
    }

    @Test
    void whenLocalEmpty_isNeedUpdate_True(@Mock RssUpdate remoteUpdate) {
        //given
        when(localRssService.getLocalRssUpdate()).thenReturn(Optional.empty());
        when(remoteRssService.getRemoteRssUpdate()).thenReturn(Optional.of(remoteUpdate));
        when(localRssService.saveRssUpdate(remoteUpdate)).thenReturn(remoteUpdate);
        //when
        final boolean needUpdate = updateService.isNeedUpdate();
        //then
        assertTrue(needUpdate);
        verify(localRssService, times(1)).saveRssUpdate(remoteUpdate);
    }

    @Test
    void whenLocalBefore_isNeedUpdate_True() {
        //given
        RssUpdate remoteUpdate = new RssUpdate().withUpdateTime(LocalDateTime.now());
        RssUpdate localUpdate = new RssUpdate().withUpdateTime(remoteUpdate.getUpdateTime().minusHours(1));
        when(localRssService.getLocalRssUpdate()).thenReturn(Optional.of(localUpdate));
        when(remoteRssService.getRemoteRssUpdate()).thenReturn(Optional.of(remoteUpdate));
        when(localRssService.saveRssUpdate(remoteUpdate)).thenReturn(remoteUpdate);
        //when
        final boolean needUpdate = updateService.isNeedUpdate();
        //then
        assertTrue(needUpdate);
        verify(localRssService, times(1)).saveRssUpdate(remoteUpdate);
    }

    @Test
    void whenLocalEquals_isNeedUpdate_False() {
        //given
        RssUpdate remoteUpdate = new RssUpdate().withUpdateTime(LocalDateTime.now());
        RssUpdate localUpdate = new RssUpdate().withUpdateTime(remoteUpdate.getUpdateTime());
        when(localRssService.getLocalRssUpdate()).thenReturn(Optional.of(localUpdate));
        when(remoteRssService.getRemoteRssUpdate()).thenReturn(Optional.of(remoteUpdate));
        //when
        final boolean needUpdate = updateService.isNeedUpdate();
        //then
        assertFalse(needUpdate);
        verify(localRssService, times(0)).saveRssUpdate(remoteUpdate);
    }

}