package org.gamboni.shopping.server.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.gamboni.shopping.server.ui.UiMode;

/**
 * @author tendays
 */
@RegisterForReflection
public record ItemTransition(String id, Type type, State state, long sequence) implements Dto {

    public enum Type {
        CREATE, UPDATE, REMOVE;

        static Type ofTransition(boolean before, boolean after) {
            if (after && !before) {
                return CREATE;
            } else if (after && before) {
                return UPDATE;
            } else if (before && !after) {
                return REMOVE;
            } else { // !before && !after. We could make this return Optional to support that last case.
                throw new IllegalStateException();
            }
        }
    }

    public static ItemTransition forItem(UiMode mode, State oldState, Item item) {
        return new ItemTransition(item.getText(),
                Type.ofTransition(
                        mode.test(oldState),
                        mode.test(item.getState())
                ),
                item.getState(),
                item.getSequence());
    }
}
