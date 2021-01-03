package ru.vas.resourceservice.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serializer;

@Slf4j
public class BlockedResourceSerializer implements Serializer<BlockedResource> {

    @Override
    public byte[] serialize(String s, BlockedResource blockedResource) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(blockedResource).getBytes();
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации BlockedResource", e);
            return null;
        }
    }
}
