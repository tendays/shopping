package org.gamboni.shopping.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.gamboni.shopping.server.domain.*;
import org.gamboni.shopping.server.http.ShoppingApi;
import org.gamboni.shopping.server.ui.UiMode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ServerEndpoint(ShoppingApi.SOCKET_URL)
@ApplicationScoped
@Slf4j
public class ShoppingSocket {

    @Inject
    Vertx vertx;

    @Inject
    Store s;

    @Inject
    ObjectMapper json;

    private final Map<Session, UiMode> sessions = new HashMap<>();
    @OnOpen
    public synchronized void onOpen(Session session) {
        log.debug("New session opened");
    }

    @OnMessage
    public synchronized void onMessage(String message, Session session) throws IOException {
        log.info("Got message '{}' on {}", message, session);
        WebSocketPayload payload = json.readValue(message, WebSocketPayload.class);
        if (payload instanceof Hello hello) {
            vertx.executeBlocking(() -> {
                log.debug("Processing subscription since {}", hello.since());

                for (Item item : s.getItemsSince(hello.since())) {
                    log.debug("Sending initial item {} to {}",
                            item.getText(), session);
                    session.getAsyncRemote().sendText(
                            ItemTransition.forItem(hello.mode(), item)
                                    .toJsonString());
                }
                log.debug("Adding {} to broadcast list", session);
                return sessions.put(session, hello.mode());
            });
        } else if (payload instanceof ShoppingCommand command) {
            vertx.executeBlocking(() -> s.setItemState(command.id(), command.action()));
        }
    }

    @OnClose
    public synchronized void onClose(Session session) {
        log.info("Session {} closing", session);
        sessions.remove(session);
    }

    @OnError
    public synchronized void onError(Session session, Throwable error) {
        log.error("Session {} failed", session, error);
        sessions.remove(session);
    }

    public synchronized void broadcast(Item item) {
        log.info("Notifying " + sessions.size() + " watchers");
        sessions.forEach((session, mode) -> {
            session.getAsyncRemote().sendText(
                    ItemTransition.forItem(mode, item)
                            .toJsonString()
            );
        });
    }
}
