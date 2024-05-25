package org.gamboni.shopping.server.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.gamboni.shopping.server.domain.*;
import org.gamboni.shopping.server.http.ShoppingApi;
import org.gamboni.tech.web.js.JsPersistentWebSocket;
import org.gamboni.tech.web.ui.AbstractComponent;
import org.gamboni.tech.web.ui.AbstractScript;
import org.gamboni.tech.web.ui.Value;

import static org.gamboni.tech.web.js.JavaScript.*;

/**
 * @author tendays
 */
@ApplicationScoped
public class Script extends AbstractScript {

    @Inject
    Style style;

    // TODO auto-initialise all these by their name
    public final Fun2 actionForState = new Fun2("actionForState");

    public final Fun2 init = new Fun2("init");

    /** Set the state of an item. */
    private final Fun1 setState = new Fun1("setState");

    /** Function creating a div for a given item. */
    public final Fun2 newElement = new Fun2("newElement");

    /** Latest sequence number obtained from server. */
    private final JsGlobal sequence = new JsGlobal("sequence");

    private final JsGlobal mode = new JsGlobal("mode");

    private final JsPersistentWebSocket socket = new JsPersistentWebSocket(ShoppingApi.SOCKET_URL) {

        @Override
        protected JsStatement handleEvent(JsExpression message) {
            return block(
                    let(
                            new JsItemTransition(message),
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
                                    )));
        }

        @Override
        protected JsExpression helloValue() {
            return JsHello.literal(
                    mode,
                    sequence);
        }
    };

    public String render() {
        return sequence.declare(0) + // initialised by init()
                mode.declare(_null) + // initialised by init()
                init.declare((modeValue, sequenceValue) -> block(
                        mode.set(modeValue),
                        sequence.set(sequenceValue),
                        socket.poll())
                ) +
                socket.declare() +
                actionForState.declare((mode, e) -> {
                            JsHtmlElement elt = new JsHtmlElement(e);

                            return _if(mode.eq(UiMode.SELECT), block(
                                    _if(elt.classList().contains(style.forState.get(State.UNUSED)),
                                            doAction(elt, Action.ADD_TO_LIST))
                                            ._elseIf(elt.classList().contains(style.forState.get(State.TO_BUY)),
                                                    doAction(elt, Action.REMOVE_FROM_LIST))))
                                    ._elseIf(mode.eq(UiMode.SHOP), block(
                                            _if(elt.classList().contains(style.forState.get(State.TO_BUY)),
                                                    doAction(elt, Action.MARK_AS_BOUGHT))
                                                    ._elseIf(elt.classList().contains(style.forState.get(State.BOUGHT)),
                                                            doAction(elt, Action.MARK_AS_NOT_BOUGHT)
                                                    )));
                        }
                ) +

                setState.declare(state -> seq(
                        _if(state.dot("sequence"), sequence.set(state.dot("sequence"))),
                        let(getElementForState(state),
                                JsHtmlElement::new,
                                e ->
                                        _if(e, block(
                                                e.classList().remove(e.classList().item(0)),
                                                e.classList().add(state.dot("state").toLowerCase()) // TODO toLowerCase repeats work done by style.forState
                                        ))))) +
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
        return block(socket.submit(
                JsShoppingCommand.literal(
                        elt.id().substring(ShoppingPage.ID_PREFIX.length()),
                        literal(action.name()))),
                setState.invoke(obj(
                        "id", elt.id().substring(ShoppingPage.ID_PREFIX.length()),
                        "state", literal(style.forState.get(targetState)))));
    }
}
