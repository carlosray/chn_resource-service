package ru.vas.resourceservice.service.impl.rss;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.vas.resourceservice.db.RssUpdateRepository;
import ru.vas.resourceservice.db.domain.RssUpdate;
import ru.vas.resourceservice.service.rss.LocalRssService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocalRssServiceImpl implements LocalRssService {
    private final RssUpdateRepository rssUpdateRepository;

    public Optional<RssUpdate> getLocalRssUpdate() {
        List<RssUpdate> allRssUpdates = rssUpdateRepository.findAll(Sort.by(Sort.Order.desc("updateTime")));
        if (allRssUpdates.isEmpty()) {
            return Optional.empty();
        }
        else {
            return Optional.of(allRssUpdates.iterator().next());
        }
    }

    @Override
    public RssUpdate saveRssUpdate(RssUpdate rssUpdate) {
        return rssUpdateRepository.save(rssUpdate);
    }
}
