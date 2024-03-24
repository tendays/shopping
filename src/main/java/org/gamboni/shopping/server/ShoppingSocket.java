package org.gamboni.shopping.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.gamboni.shopping.server.domain.Action;
import org.gamboni.shopping.server.domain.Item;
import org.gamboni.shopping.server.domain.ItemTransition;
import org.gamboni.shopping.server.domain.Store;
import org.gamboni.shopping.server.http.ShoppingApi;
import org.gamboni.shopping.server.tech.Enums;
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
        int space = message.indexOf(' ');
        if (space == -1) {
            log.error("Unknown message {}", message);
            return;
        }
        String command = message.substring(0, space);
        String param = message.substring(space + 1);

        Enums.valueOf(UiMode.class, command)
                .ifPresentOrElse(mode -> {
                    long since = Long.parseLong(param);
                    vertx.executeBlocking(() -> {
                        log.debug("Processing subscription since {}", since);

                        for (Item item : s.getItemsSince(since)) {
                            log.debug("Sending initial item {} to {}",
                                    item.getText(), session);
                            session.getAsyncRemote().sendText(
                                    ItemTransition.forItem(mode, item)
                                            .toJsonString());
                        }
                        log.debug("Adding {} to broadcast list", session);
                        sessions.put(session, mode);
                        return null;
                    });
                }, () -> Enums.valueOf(Action.class, command)
                        .ifPresentOrElse(action -> {
                            if (param.isEmpty()) {
                                log.error(action + " without name!");
                                return;
                            }

                            vertx.executeBlocking(() -> s.setItemState(param, action));
                        }, () -> log.error("unknown action " + command)));
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
