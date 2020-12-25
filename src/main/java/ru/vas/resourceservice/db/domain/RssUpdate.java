package ru.vas.resourceservice.db.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "rss_update",
        indexes = @Index(name = "rss_update_time_idx", columnList = "rss_update_time"))
@Data
@With
@AllArgsConstructor
@NoArgsConstructor
public class RssUpdate {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "rss_update_time", unique = true, columnDefinition = "TIMESTAMP")
    private LocalDateTime updateTime;
}
