package ru.vas.resourceservice.db;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vas.resourceservice.db.domain.RssUpdate;

public interface RssUpdateRepository extends JpaRepository<RssUpdate, Long> {
}
