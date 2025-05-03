package com.recargapay.wallet.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JsonHelper {

    private final ObjectMapper objectMapper;

    public JsonHelper(@Qualifier("om") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> String serialize(@Nullable T source) {
        if (source == null) {
            return null;
        }
        try {
            return this.objectMapper.writeValueAsString(source);
        } catch (Exception e) {
            log.error("Error serializing object: {}", source, e);
            return null;
        }
    }

    public <T> ObjectNode parse(T source) {
        try {
            return this.objectMapper.valueToTree(source);
        } catch (Exception e) {
            log.error("Error parsing object: {}", source, e);
            return null;
        }
    }

    public ObjectNode createObjectNode() {
        return this.objectMapper.createObjectNode();
    }
}
