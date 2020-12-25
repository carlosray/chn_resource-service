package ru.vas.resourceservice.service.impl.rss;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ru.vas.resourceservice.db.RssUpdateRepository;
import ru.vas.resourceservice.db.domain.RssUpdate;
import ru.vas.resourceservice.service.rss.LocalRssService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalRssServiceImplTest {
    @Mock
    private RssUpdateRepository rssUpdateRepository;

    private LocalRssServiceImpl localRssService;

    @BeforeEach
    void setUp() {
        localRssService = new LocalRssServiceImpl(rssUpdateRepository);
    }

    @Test
    void getLocalRssUpdate_List() {
        //given
        Long firstId = 1L;
        when(rssUpdateRepository.findAll(any(Sort.class)))
                .thenReturn(Arrays.asList(new RssUpdate().withId(firstId), new RssUpdate().withId(2L)));
        //when
        final Optional<RssUpdate> localRssUpdate = localRssService.getLocalRssUpdate();
        //then
        assertTrue(localRssUpdate.isPresent());
        assertEquals(firstId, localRssUpdate.get().getId());
    }

    @Test
    void getLocalRssUpdate_Empty() {
        //given
        when(rssUpdateRepository.findAll(any(Sort.class)))
                .thenReturn(Collections.emptyList());
        //when
        final Optional<RssUpdate> result = localRssService.getLocalRssUpdate();
        //then
        assertFalse(result.isPresent());
    }

    @Test
    void saveRssUpdate() {
        //given
        Long id = 1L;
        RssUpdate rssUpdate = new RssUpdate();
        when(rssUpdateRepository.save(rssUpdate)).thenReturn(rssUpdate.withId(id));
        //when
        final RssUpdate result = localRssService.saveRssUpdate(rssUpdate);
        //then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(id, result.getId());
    }
}