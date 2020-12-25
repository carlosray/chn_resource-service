package ru.vas.resourceservice.service.impl.rss;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import ru.vas.resourceservice.db.domain.RssUpdate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitRssServiceTest {
    @Mock
    private RestTemplate restTemplate;

    private GitRssService rssService;

    @BeforeEach
    void setUp() {
        rssService = new GitRssService(restTemplate);
    }

    @Test
    void getRemoteRssUpdate() {
        //given
        String zoneId = "Europe/Moscow";
        GitRssService.setZoneId(zoneId);
        String testUrl = "test";
        rssService.setUrl(testUrl);
        final ZonedDateTime zonedDateTime = ZonedDateTime.now();
        final GitRssService.GitRssFeed gitRssFeed = new GitRssService.GitRssFeed();
        final GitRssService.GitRssUpdate gitRssUpdate = new GitRssService.GitRssUpdate();
        gitRssUpdate.setDateValue(zonedDateTime);
        gitRssFeed.setUpdate(gitRssUpdate);
        when(restTemplate.getForObject(testUrl, GitRssService.GitRssFeed.class)).thenReturn(gitRssFeed);
        //when
        final Optional<RssUpdate> result = rssService.getRemoteRssUpdate();
        //then
        assertTrue(result.isPresent());
        assertEquals(zonedDateTime.withZoneSameInstant(ZoneId.of(zoneId)).toLocalDateTime(), result.get().getUpdateTime());
    }

    @Test
    void getRemoteRssUpdate_WhenNullFeed() {
        //given
        String testUrl = "test";
        rssService.setUrl(testUrl);
        when(restTemplate.getForObject(testUrl, GitRssService.GitRssFeed.class)).thenReturn(null);
        //when
        final Optional<RssUpdate> result = rssService.getRemoteRssUpdate();
        //then
        assertFalse(result.isPresent());
    }

    @Test
    void getRemoteRssUpdate_WhenNullUpdate() {
        //given
        String testUrl = "test";
        rssService.setUrl(testUrl);
        final GitRssService.GitRssFeed gitRssFeed = new GitRssService.GitRssFeed();
        when(restTemplate.getForObject(testUrl, GitRssService.GitRssFeed.class)).thenReturn(gitRssFeed);
        //when
        final Optional<RssUpdate> result = rssService.getRemoteRssUpdate();
        //then
        assertFalse(result.isPresent());
    }

}