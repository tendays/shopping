package org.gamboni.shopping.server;

import io.quarkus.websockets.next.WebSocket;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.gamboni.shopping.server.domain.Hello;
import org.gamboni.shopping.server.domain.ShoppingCommand;
import org.gamboni.shopping.server.domain.Store;
import org.gamboni.shopping.server.domain.WebSocketPayload;
import org.gamboni.tech.history.event.StampedEventList;
import org.gamboni.tech.quarkus.QuarkusWebSocket;
import org.gamboni.tech.web.js.JsPersistentWebSocket;
import org.gamboni.tech.web.ws.BroadcastTarget;

import java.io.IOException;

@ApplicationScoped
@Slf4j
@WebSocket(path = JsPersistentWebSocket.DEFAULT_URL)
public class ShoppingSocket extends QuarkusWebSocket {

    @Inject
    Store s;

    @Override
    protected synchronized void handleMessage(BroadcastTarget client, String message) throws IOException {
        log.info("Got message '{}' on {}", message, client);
        WebSocketPayload payload = json.readValue(message, WebSocketPayload.class);
        if (payload instanceof Hello hello) {
            log.debug("Processing subscription since {}", hello.since());

            StampedEventList firstBatch = s.addListener(client, hello.mode(), hello.since());
            log.debug("Sending initial items {} to {}", firstBatch, client);
            client.sendOrLog(firstBatch);
        } else if (payload instanceof ShoppingCommand command) {
            broadcast(s.update(us -> us.setItemState(command.id(), command.action()))::get);
        } else {
            throw new IllegalArgumentException();
        }
    }
}
