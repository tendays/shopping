package org.gamboni.shopping.server.domain;

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
