package org.gamboni.shopping.server.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.gamboni.shopping.server.ui.UiMode;
import org.gamboni.tech.web.js.JS;

/**
 * @author tendays
 */
@RegisterForReflection
@JS
public record ItemTransition(String id, String image, Type type, State state) implements Dto {

    public enum Type {
        VISIBLE, HIDDEN;

    }

    public static ItemTransition forItem(UiMode mode, Item item) {
        return new ItemTransition(item.getText(),
                item.getImage().getText(),
                mode.test(item.getState()) ? Type.VISIBLE : Type.HIDDEN,
                item.getState());
    }
}
