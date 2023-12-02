package org.gamboni.shopping.server.domain;

/**
 * @author tendays
 */
public class ItemState implements Dto {
    public final String id;
    public final State state;

    private ItemState(String id, State state) {
        this.id = id;
        this.state = state;
    }

    public static ItemState forItem(Item item) {
        return new ItemState(item.getText(), item.getState());
    }
}
