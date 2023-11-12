package org.gamboni.shopping.server.ui;

import org.gamboni.shopping.server.domain.Action;
import org.gamboni.shopping.server.domain.State;
import org.gamboni.shopping.server.http.ShoppingApi;
import org.gamboni.shopping.server.tech.ui.AbstractScript;

/**
 * @author tendays
 */
public class Script extends AbstractScript {
    public final String actionForState = "actionForState";
    /** Submit an action to send to back-end */
    private final String submit = "submit";
    /** Set the state of an item. */
    private final String setState = "setState";

    public final String poll = "poll";

    private final Style style = new Style();
    public String render() {
        return function(actionForState, (mode, e) -> {
            JsHtmlElement elt = new JsHtmlElement(e);

            return _if(mode.eq(UiMode.SELECT.name()), block(
                    _if(elt.classList().contains(style.forState.get(State.UNUSED)),
                            doAction(elt, Action.ADD_TO_LIST))
                            ._elseIf(elt.classList().contains(style.forState.get(State.TO_BUY)),
                                    doAction(elt, Action.REMOVE_FROM_LIST))))
                    ._elseIf(mode.eq(UiMode.SHOP.name()), block(
                            _if(elt.classList().contains(style.forState.get(State.TO_BUY)),
                                    doAction(elt, Action.MARK_AS_BOUGHT))
                                    ._elseIf(elt.classList().contains(style.forState.get(State.BOUGHT)),
                                            doAction(elt, Action.MARK_AS_NOT_BOUGHT)
                                    )));
                }
        ) +

                function(submit, action -> ShoppingApi.ACTION
                        .invoke().param(action.dot("action")).param(action.dot("id")).subscribe(r -> "")) +

                function(setState, state ->
                        // TODO create/destroy element depending on state and mode
                        let(getElementById(literal(ShoppingPage.ID_PREFIX).plus(state.dot("id"))),
                                JsHtmlElement::new,
                                e ->
                                _if(e, block(
                                        e.classList().remove(e.classList().item(0)),
                                        e.classList().add(state.dot("state.toLowerCase()")) // TODO toLowerCase repeats work done by style.forState
                                )))) +
                function(poll, sequence -> ShoppingApi.WATCH.invoke().param(sequence).subscribe(json ->
                        let(new JsExpression("JSON.parse("+ json +")"),
                                JsExpression::new,
                                result ->
                                new JsStatement() {
                                    @Override
                                    public String toString() {
                                        return result.dot("batch") + ".forEach(item => "+
                                                setState +"(item));" +
                                                poll +"(" + result + ".continuation);";
                                    }
                                }
                        )));
    }

    private JsStatement doAction(JsHtmlElement elt, Action action) {
        State targetState = action.to;
        return block(submit + "("+ obj("id", elt.id().substring(ShoppingPage.ID_PREFIX.length()),
                "action", literal(action.name())) + ")",
                setState + "("+ obj("id", elt.id().substring(ShoppingPage.ID_PREFIX.length()),
                "state", literal(style.forState.get(targetState))) + ")");
    }
}
