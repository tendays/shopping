package org.gamboni.shopping.server.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.gamboni.tech.web.js.JS;

/** Command sent from the front end to the back end. */
@RegisterForReflection
@JS
public record ShoppingCommand(String id, Action action) implements WebSocketPayload {

}
