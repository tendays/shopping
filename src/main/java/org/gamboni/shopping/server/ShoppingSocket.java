package org.gamboni.shopping.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.gamboni.shopping.server.domain.Item;
import org.gamboni.shopping.server.domain.ItemState;
import org.gamboni.shopping.server.domain.Item_;
import org.gamboni.shopping.server.domain.Store;
import org.gamboni.shopping.server.http.ShoppingApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    List<Session> sessions = new ArrayList<>();
    @OnOpen
    public synchronized void onOpen(Session session) {
        log.debug("New session opened");
    }

    @OnMessage
    public synchronized void onMessage(String message, Session session) throws IOException {
        log.info("Got message '{}' on {}", message, session);
        Long since = Long.parseLong(message);
        vertx.executeBlocking(() -> {
            log.debug("Processing subscription since {}", since);


            for (Item item : s.getItemsSince(since)) {
                log.debug("Sending initial item {} to {}",
                        item.getText(), session);
                session.getAsyncRemote().sendText(json.writeValueAsString(item));
            }
            log.debug("Adding {} to broadcast list", session);
            sessions.add(session);
            return null;
        });
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

    public synchronized void broadcast(ItemState item) throws JsonProcessingException {
        String itemText = json.writeValueAsString(item);
        log.info("Notifying " + sessions.size() + " watchers");
        for (var s : sessions) {
            s.getAsyncRemote().sendText(itemText);
        }
    }
}
