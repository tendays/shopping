package org.gamboni.shopping.server.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.gamboni.shopping.server.domain.Action;
import org.gamboni.shopping.server.domain.ItemTransition;
import org.gamboni.shopping.server.domain.JsItemTransition;
import org.gamboni.shopping.server.domain.State;
import org.gamboni.shopping.server.http.ShoppingApi;
import org.gamboni.shopping.server.tech.ui.AbstractComponent;
import org.gamboni.shopping.server.tech.ui.AbstractScript;
import org.gamboni.shopping.server.tech.ui.Value;

import static org.gamboni.shopping.server.tech.js.JavaScript.*;

/**
 * @author tendays
 */
@ApplicationScoped
public class Script extends AbstractScript {
    public static final int POLL_INTERVAL = 60000;

    @Inject
    Style style;

    // TODO auto-initialise all these by their name
    public final Fun2 actionForState = new Fun2("actionForState");
    /** Submit an action to send to back-end */
    private final Fun1 submit = new Fun1("submit");
    /** Set the state of an item. */
    private final Fun1 setState = new Fun1("setState");

    private final Fun2 flushQueue = new Fun2("flushQueue");

    public final Fun2 poll = new Fun2("poll");

    /** Function creating a div for a given item. */
    public final Fun2 newElement = new Fun2("newElement");

    /** Latest sequence number obtained from server. */
    private final JsGlobal sequence = new JsGlobal("sequence");
    /** Current websocket object. A new one is created every time the connection drops. */
    private final JsGlobal socket = new JsGlobal("socket");
    /** Queued events, used when the connection is down. */
    private final JsGlobal queue = new JsGlobal("queue");

    public String render() {
        return sequence.declare(0) +
                socket.declare("null") + // should likely immediately call the poll() function
                queue.declare("[]") +
                actionForState.declare((mode, e) -> {
                            JsHtmlElement elt = new JsHtmlElement(e);

                            return _if(mode.eq(literal(UiMode.SELECT.name())), block(
                                    _if(elt.classList().contains(style.forState.get(State.UNUSED)),
                                            doAction(elt, Action.ADD_TO_LIST))
                                            ._elseIf(elt.classList().contains(style.forState.get(State.TO_BUY)),
                                                    doAction(elt, Action.REMOVE_FROM_LIST))))
                                    ._elseIf(mode.eq(literal(UiMode.SHOP.name())), block(
                                            _if(elt.classList().contains(style.forState.get(State.TO_BUY)),
                                                    doAction(elt, Action.MARK_AS_BOUGHT))
                                                    ._elseIf(elt.classList().contains(style.forState.get(State.BOUGHT)),
                                                            doAction(elt, Action.MARK_AS_NOT_BOUGHT)
                                                    )));
                        }
                ) +

                submit.declare(action ->
                        _if(socket.dot("readyState").eq(WebSocket.dot("OPEN")),
                                socket.invoke("send", action.dot("action").plus(" ")
                                        .plus(action.dot("id"))))
                                ._else(queue.invoke("push", action))) +

                setState.declare(state -> seq(
                        _if(state.dot("sequence"), sequence.set(state.dot("sequence"))),
                        let(getElementForState(state),
                                JsHtmlElement::new,
                                e ->
                                        _if(e, block(
                                                e.classList().remove(e.classList().item(0)),
                                                e.classList().add(state.dot("state").toLowerCase()) // TODO toLowerCase repeats work done by style.forState
                                        ))))) +

                flushQueue.declare((newSequence, mode) -> seq(
                        socket.invoke("send", mode.plus(literal(" "))
                                .plus(newSequence)),
                        let(queue,
                                JsExpression::of,
                                queueCopy -> seq(
                                        queue.set(array()),
                                        queueCopy.invoke("forEach", lambda(
                                                "item",
                                                submit::invoke
                                        ))
                                )
                        ))) +

                poll.declare((mode, newSequence) -> seq(
                                sequence.set(newSequence),
                                socket.set(newWebSocket(
                                                        new JsString(s -> "((window.location.protocol === 'https:') ? 'wss://' : 'ws://')")
                                                                .plus(s -> "window.location.host")
                                        .plus(literal(ShoppingApi.SOCKET_URL)))),
                                /* If the websocket is already closed, we could not establish the connection, and try again later. */
                                _if(socket.dot("readyState").eq(WebSocket.dot("CLOSED")), block(
                                                setTimeout(poll.invoke(mode, sequence), POLL_INTERVAL),
                                        _return()
                                ))._elseIf(socket.dot("readyState").eq(WebSocket.dot("OPEN")),
                                        flushQueue.invoke(newSequence, mode)),
                                socket.invoke("addEventListener", literal("open"),
                                        lambda(flushQueue.invoke(newSequence, mode))),

                                socket.invoke("addEventListener", literal("message"),
                                        lambda("event",
                                                event -> block(
                                                        let(
                                                                new JsItemTransition(jsonParse(event.dot("data"))),
                                                        JsItemTransition::new,
                                                        data ->
                                                                let(getElementForState(data),
                                                                JsHtmlElement::new,
                                                                existing ->
                                                                _if(data.type().eq(literal(ItemTransition.Type.HIDDEN))
                                                                                .and(existing),
                                                                        existing.remove())
                                                                ._elseIf(data.type().eq(literal(ItemTransition.Type.VISIBLE))
                                                                                .and(existing),
                                                                        setState.invoke(data))
                                                                ._elseIf(data.type().eq(literal(ItemTransition.Type.VISIBLE)),
                                                                                newElement.invoke(mode, data))
                                                                                ))))),

                                let(/* close handler */
                                        lambda("event", event ->
                                                seq(consoleLog(event),
                                                        setTimeout(poll.invoke(mode, sequence), POLL_INTERVAL)
                                                )),
                                        JsExpression::of,
                                        closeHandler -> seq(
                                                // infinite loops trying to reconnect
                                                socket.invoke("addEventListener", literal("close"), closeHandler),

                                                socket.invoke("addEventListener", literal("error"), lambda("event",
                                                                event ->
                                                                        seq(consoleLog(event),
                                                                                // when an open socket errors out we get both error and close events
                                                                                // ... but we don't want to run setTimeout twice.
                                                                                socket.invoke("removeEventListener", literal("close"), closeHandler),

                                                                                setTimeout(poll.invoke(mode, sequence), POLL_INTERVAL)
                                                                        )
                                                        )
                                                )
                                        ))
                        )
                ) +
                newElement.declare((mode, item) -> {
                    var itemTransition = new JsItemTransition(item);
                    return seq(
                            sequence.set(itemTransition.sequence()),
                            new ItemComponent(AbstractComponent.End.FRONT, style, this)
                                    .render(Value.of(mode),
                                    /* Actual value:
                                    {"id":"basilic","type":"CREATE","state":"TO_BUY","sequence":5007}
                                     */
                                            Value.of(itemTransition.state()),
                                            Value.of(itemTransition.id()),
                                            Value.of(itemTransition.id())
                                    ).javascriptCreate(
                                            elt ->
                                                    JsStatement.of(getBodyElement()
                                                            .prepend(elt))));
                });
    }

    private static JsHtmlElement getElementForState(JsExpression state) {
        return getElementById(literal(ShoppingPage.ID_PREFIX).plus(state.dot("id")));
    }

    private JsStatement doAction(JsHtmlElement elt, Action action) {
        State targetState = action.to;
        return block(submit.invoke(obj(
                        "id", elt.id().substring(ShoppingPage.ID_PREFIX.length()),
                        "action", literal(action.name()))),
                setState.invoke(obj(
                        "id", elt.id().substring(ShoppingPage.ID_PREFIX.length()),
                        "state", literal(style.forState.get(targetState)))));
    }
}
