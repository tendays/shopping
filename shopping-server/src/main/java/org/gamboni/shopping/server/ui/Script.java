package org.gamboni.shopping.server.ui;

import com.google.common.collect.ImmutableMap;

import org.gamboni.shopping.server.domain.Action;
import org.gamboni.shopping.server.domain.State;
import org.gamboni.shopping.server.http.ShoppingApi;

import spark.route.HttpMethod;

/**
 * @author tendays
 */
public class Script extends AbstractScript {
    public final String actionForState = "actionForState";
    /** Submit an action to send to back-end */
    private final String submit = "submit";
    /** Set the state of an item. */
    private final String setState = "setState";

    private final String poll = "poll";

    private final Style style = new Style();
    public String render() {
        return function(actionForState, (mode, e) -> {
            JsHtmlElement elt = new JsHtmlElement(e);

            return _if(mode + "===" + literal(UiMode.SELECT.name()), block(
                    _if(elt.classList().contains(style.forState.get(State.UNUSED)),
                            doAction(elt, Action.ADD_TO_LIST))
                            ._elseIf(elt.classList().contains(style.forState.get(State.TO_BUY)),
                                    doAction(elt, Action.REMOVE_FROM_LIST))))
                    ._elseIf(mode + "===" + literal(UiMode.SHOP.name()), block(
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
                        let(getElementById(literal(ShoppingPage.ID_PREFIX).plus(state.dot("id"))), e ->
                                _if(e, block(
                                        e.classList().remove(e.classList().item(0)),
                                        e.classList().add(state.dot("state"))
                                )))) +
                function(poll, sequence -> ShoppingApi.WATCH.invoke().param(sequence).subscribe(result -> ""));
    }

    private String doAction(JsHtmlElement elt, Action action) {
        State targetState = action.to;
        return block(submit + "("+ obj("id", elt.id().substring(ShoppingPage.ID_PREFIX.length()),
                "action", literal(action.name())) + ")",
                setState + "("+ obj("id", elt.id().substring(ShoppingPage.ID_PREFIX.length()),
                "state", literal(style.forState.get(targetState))) + ")");
    }
}
