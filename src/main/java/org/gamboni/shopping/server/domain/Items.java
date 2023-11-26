package org.gamboni.shopping.server.domain;

import java.util.Collection;
import java.util.OptionalLong;

/**
 * @author tendays
 */
public abstract class Items {
    public static OptionalLong nextSequence(Collection<Item> items) {
        return  items.stream().mapToLong(Item::getSequence).max();
    }
}
