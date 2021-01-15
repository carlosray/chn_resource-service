package ru.vas.resourceservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.ToString;
import org.junit.platform.commons.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@ToString
public class BlockedResource {
    private final String rowLine;
    private List<String> ip;
    private String domain;
    private String reason;
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    private LocalDate dateOfBlock;
    private Set<String> additionalParams = new HashSet<>();

    public BlockedResource(String line) {
        this.rowLine = line;
        setUp(line);
    }

    private void setUp(String line) {
        final String[] params = line.split(Delimiters.SEMICOLON.getValue());
        setIp(params[0]);
        setDomain(params[1]);
        addAdditionalParam(params[2]);
        setReason(params[3]);
        setNumber(params[4]);
        setDateOfBlock(params[5]);
    }

    private void setIp(String param) {
        this.ip = Arrays.stream(param.split(Delimiters.VERT_LINE.getValue()))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private void setDomain(String param) {
        this.domain = param.trim();
    }

    private void addAdditionalParam(String param) {
        final String trimmedParam = param.trim();
        if (StringUtils.isNotBlank(trimmedParam)) {
            additionalParams.add(trimmedParam);
        }
    }

    private void setReason(String param) {
        this.reason = param.trim();
    }

    private void setNumber(String param) {
        additionalParams.add(param.trim());
    }

    private void setDateOfBlock(String param) {
        try {
            this.dateOfBlock = LocalDate.parse(param);
        } catch (DateTimeParseException ex) {
            addAdditionalParam(param);
        }
    }

    @Getter
    public enum Delimiters {
        SEMICOLON(";"),
        VERT_LINE("\\|");

        private String value;

        Delimiters(String delimiter) {
            this.value = delimiter;
        }
    }
}
