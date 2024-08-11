package org.gamboni.shopping.server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.gamboni.shopping.server.domain.*;
import org.gamboni.shopping.server.http.ShoppingApi;
import org.gamboni.tech.quarkus.QuarkusWebSocket;

import java.io.IOException;

@ServerEndpoint(ShoppingApi.SOCKET_URL)
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

                for (ItemTransition transition : s.addListener(
                        new SessionBroadcastTarget(session), hello.mode(), hello.since())
                        .updates()) {
                    log.debug("Sending initial item {} to {}",
                            transition.id(), session);
                    session.getAsyncRemote().sendText(
                            transition.toJsonString());
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
