package org.gamboni.shopping.server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.gamboni.shopping.server.domain.Hello;
import org.gamboni.shopping.server.domain.ShoppingCommand;
import org.gamboni.shopping.server.domain.Store;
import org.gamboni.shopping.server.domain.WebSocketPayload;
import org.gamboni.tech.quarkus.QuarkusWebSocket;
import org.gamboni.tech.web.js.JsPersistentWebSocket;

import java.io.IOException;

@ServerEndpoint(JsPersistentWebSocket.DEFAULT_URL)
@ApplicationScoped
@Slf4j
public class ShoppingSocket extends QuarkusWebSocket {

    @Inject
    Store s;
    @OnMessage
    public synchronized void onMessage(String message, Session session) throws IOException {
        log.info("Got message '{}' on {}", message, session);
        WebSocketPayload payload = json.readValue(message, WebSocketPayload.class);
        vertx.executeBlocking(() -> {
            if (payload instanceof Hello hello) {
                log.debug("Processing subscription since {}", hello.since());

                for (Object transition : s.addListener(
                        new SessionBroadcastTarget(session), hello.mode(), hello.since())
                        .updates()) {
                    log.debug("Sending initial item {} to {}",
                            transition, session);
                    session.getAsyncRemote().sendText(
                            toJsonString(transition));
                }
            } else if (payload instanceof ShoppingCommand command) {
                broadcast(s.update(us -> us.setItemState(command.id(), command.action()))::get);
            } else {
                throw new IllegalArgumentException();
            }
            return null; // unused return value
        });
    }
}
