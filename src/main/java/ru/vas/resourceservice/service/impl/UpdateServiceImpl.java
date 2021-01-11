package ru.vas.resourceservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.vas.resourceservice.db.domain.RssUpdate;
import ru.vas.resourceservice.service.UpdateService;
import ru.vas.resourceservice.service.rss.LocalRssService;
import ru.vas.resourceservice.service.rss.RemoteRssService;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class UpdateServiceImpl implements UpdateService {
    private final LocalRssService localRssService;
    private final RemoteRssService remoteRssService;

    @Override
    public boolean isNeedUpdate() {
        return localRssService.getLocalRssUpdate()
                .map(isLocalDateBeforeRemote())
                .orElseGet(createFirstLocalRssUpdate());
    }

    private Function<RssUpdate, Boolean> isLocalDateBeforeRemote() {
        return localRssUpdate -> remoteRssService.getRemoteRssUpdate()
                .filter(isLocalDateBeforeRemote(localRssUpdate))
                .map(localRssService::saveRssUpdate)
                .isPresent();
    }

    private Predicate<RssUpdate> isLocalDateBeforeRemote(RssUpdate local) {
        return remoteRssUpdate -> local.getUpdateTime().isBefore(remoteRssUpdate.getUpdateTime());
    }

    private Supplier<Boolean> createFirstLocalRssUpdate() {
        return () -> remoteRssService.getRemoteRssUpdate()
                .map(localRssService::saveRssUpdate)
                .isPresent();
    }
}
