package org.gamboni.shopping.server.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author tendays
 */
public interface Dto {
    ObjectMapper MAPPER = new ObjectMapper();

    default String toJsonString() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"status\": \"failed\"}";
        }
    }
}
