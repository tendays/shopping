package org.gamboni.shopping.server.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.gamboni.shopping.server.ui.UiMode;

/**
 * @author tendays
 */
@RegisterForReflection
public record ItemTransition(String id, Type type, State state, long sequence) implements Dto {

    public enum Type {
        VISIBLE, HIDDEN;

    }

    public static ItemTransition forItem(UiMode mode, Item item) {
        return new ItemTransition(item.getText(),
                mode.test(item.getState()) ? Type.VISIBLE : Type.HIDDEN,
                item.getState(),
                item.getSequence());
    }
}
