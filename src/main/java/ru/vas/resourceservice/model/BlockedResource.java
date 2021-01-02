package ru.vas.resourceservice.model;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@ToString
public class BlockedResource {
    private String id;
    private List<String> ip;
    private String domain;
    private String reason;
    private String number;
    private LocalDate dateOfBlock;

    public BlockedResource(String id, String line) {
        this.id = id;
        setUp(line);
    }

    private void setUp(String line) {
        final String[] params = line.split(Delimiters.SEMICOLON.getValue());
        setIp(params[0]);
        setDomain(params[1]);
        setReason(params[3]);
        setNumber(params[4]);
        setDateOfBlock(params[5]);
        System.out.println();
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

    private void setReason(String param) {
        this.reason = param.trim();
    }

    private void setNumber(String param) {
        this.number = param.trim();
    }

    private void setDateOfBlock(String param) {
        this.dateOfBlock = LocalDate.parse(param);
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
