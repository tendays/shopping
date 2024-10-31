package org.gamboni.shopping.server.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.gamboni.tech.history.event.Event;
import org.gamboni.tech.web.js.JS;

/**
 * @author tendays
 */
@RegisterForReflection
@JS
public record NewItemEvent(String id, String image, State state) implements Event {

    public static NewItemEvent forItem(Item item) {
        return new NewItemEvent(item.getText(),
                item.getImage().getText(),
                item.getState());
    }
}
