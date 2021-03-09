package org.gamboni.shopping.server.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author tendays
 */
public class Dto {
    protected static final ObjectMapper MAPPER = new ObjectMapper();

    public String toString() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"status\": \"failed\"}";
        }
    }
}
