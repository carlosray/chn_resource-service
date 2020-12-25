package ru.vas.resourceservice.service.rss;

import ru.vas.resourceservice.db.domain.RssUpdate;

import java.util.Optional;

public interface LocalRssService {
    Optional<RssUpdate> getLocalRssUpdate();
    RssUpdate saveRssUpdate(RssUpdate rssUpdate);
}
