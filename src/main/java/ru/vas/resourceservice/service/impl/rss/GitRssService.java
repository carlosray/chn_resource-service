package ru.vas.resourceservice.service.impl.rss;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.vas.resourceservice.db.domain.RssUpdate;
import ru.vas.resourceservice.service.rss.RemoteRssService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "resource-service.rss.git")
public class GitRssService implements RemoteRssService {
    @Setter
    private String url;
    @Setter
    private static String zoneId;

    private final RestTemplate restTemplate;

    public Optional<RssUpdate> getRemoteRssUpdate() {
        return Optional.ofNullable(requestForAnswer());
    }

    private RssUpdate requestForAnswer() {
        final GitRssFeed gitRssFeed = restTemplate.getForObject(url, GitRssFeed.class);
        return Optional.ofNullable(gitRssFeed)
                .flatMap(feed -> Optional.ofNullable(feed.getUpdate()))
                .map(update -> new RssUpdate().withUpdateTime(update.getDateTime()))
                .orElse(null);
    }

    @Data
    static final class GitRssFeed {
        @JacksonXmlProperty(localName = "updated")
        private GitRssUpdate update;
    }

    @Data
    static final class GitRssUpdate {
        @JacksonXmlText(value = true)
        @Setter
        private ZonedDateTime dateValue;

        LocalDateTime getDateTime() {
            return dateValue.withZoneSameInstant(getZoneId()).toLocalDateTime();
        }

        private ZoneId getZoneId() {
            return Objects.isNull(zoneId) ? ZoneId.systemDefault() : ZoneId.of(zoneId);
        }

    }
}
