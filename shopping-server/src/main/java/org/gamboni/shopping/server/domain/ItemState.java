package org.gamboni.shopping.server.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author tendays
 */
public class ItemState extends Dto {
    public final String id;
    public final State state;

    public ItemState(String id, State state) {
        this.id = id;
        this.state = state;
    }
}
