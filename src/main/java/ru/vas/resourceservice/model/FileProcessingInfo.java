package ru.vas.resourceservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileProcessingInfo {
    private String fileMessageGuid;
    private Duration duration;
    private boolean isSuccess;
}
