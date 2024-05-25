package org.gamboni.shopping.server.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.gamboni.shopping.server.ui.UiMode;
import org.gamboni.tech.web.js.JS;
@RegisterForReflection
@JS
public record Hello(UiMode mode, long since)  implements WebSocketPayload {
}
