package ru.vas.resourceservice.service.rss;

import ru.vas.resourceservice.db.domain.RssUpdate;

import java.util.Optional;

public interface RemoteRssService {
    Optional<RssUpdate> getRemoteRssUpdate();
}
