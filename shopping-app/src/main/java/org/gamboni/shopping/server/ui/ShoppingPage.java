package org.gamboni.shopping.server.ui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.gamboni.shopping.server.domain.*;
import org.gamboni.tech.history.ClientStateHandler;
import org.gamboni.tech.quarkus.QuarkusPage;
import org.gamboni.tech.web.js.JavaScript;
import org.gamboni.tech.web.js.JsPersistentWebSocket;
import org.gamboni.tech.web.ui.FavIconResource;
import org.gamboni.tech.web.ui.Html;
import org.gamboni.tech.web.ui.Value;

import java.util.List;

import static org.gamboni.tech.web.js.JavaScript.*;

/**
 * @author tendays
 */
@ApplicationScoped
@Path("/")
public class ShoppingPage extends QuarkusPage<ShoppingPage.Data> {
    public static final String ID_PREFIX = "i-";

    public record Data(UiMode mode, List<Item> items) {}

    @Inject
    Style style;

    @Inject Store store;

    // TODO auto-initialise all these by their name
    public final JavaScript.Fun2 actionForState = new JavaScript.Fun2("actionForState");

    /** Set the state of an item. */
    private final JavaScript.Fun1 setState = new JavaScript.Fun1("setState");

    /** Function creating a div for a given item. */
    public final JavaScript.Fun2 newElement = new JavaScript.Fun2("newElement");

    private final JsGlobal mode = new JsGlobal("mode");
    private final ClientStateHandler stateHandler = new ClientStateHandler() {

        @Override
        protected JavaScript.JsExpression helloValue(JavaScript.JsExpression sequence) {
            return JsHello.literal(
                    mode,
                    sequence);
        }
    }.addHandler((event, matcher) -> new JsItemTransition(event),
            event -> let(getElementForState(event),
                    JavaScript.JsHtmlElement::new,
                    existing ->
                            _if(event.type().eq(literal(ItemTransition.Type.HIDDEN))
                                            .and(existing),
                                    existing.remove())
                                    ._elseIf(event.type().eq(literal(ItemTransition.Type.VISIBLE))
                                                    .and(existing),
                                            setState.invoke(event))
                                    ._elseIf(event.type().eq(literal(ItemTransition.Type.VISIBLE)),
                                            newElement.invoke(mode, event))));

    private final JsPersistentWebSocket socket = new JsPersistentWebSocket(stateHandler);

    @PostConstruct
    void init() {
        socket.addTo(this);

        addToOnLoad(onLoad -> mode.set(onLoad.addParameter(data -> literal(data.mode))));
        addToOnLoad(onLoad -> stateHandler.init(onLoad.addParameter(
                data -> literal(Items.nextSequence(data.items).orElse(0)))));
        addToOnLoad(onLoad -> socket.poll());

        addToScript(
                mode.declare(_null), // initialised by init()

                actionForState.declare((mode, e) -> {
                            JsHtmlElement elt = new JsHtmlElement(e);

                            return _if(mode.eq(UiMode.SELECT),
                                    _if(elt.classList().contains(style.forState.get(State.UNUSED)),
                                            doAction(elt, Action.ADD_TO_LIST))
                                            ._elseIf(elt.classList().contains(style.forState.get(State.TO_BUY)),
                                                    doAction(elt, Action.REMOVE_FROM_LIST)))
                                    ._elseIf(mode.eq(UiMode.SHOP),
                                            _if(elt.classList().contains(style.forState.get(State.TO_BUY)),
                                                    doAction(elt, Action.MARK_AS_BOUGHT))
                                                    ._elseIf(elt.classList().contains(style.forState.get(State.BOUGHT)),
                                                            doAction(elt, Action.MARK_AS_NOT_BOUGHT)
                                                    ));
                        }
                ),

                setState.declare(state -> seq(
                        let(getElementForState(state),
                                JsHtmlElement::new,
                                e ->
                                        _if(e,
                                                e.classList().remove(e.classList().item(0)),
                                                e.classList().add(state.dot("state").toLowerCase()) // TODO toLowerCase repeats work done by style.forState
                                        )))),

                newElement.declare((mode, item) -> {
                    var itemTransition = new JsItemTransition(item);
                    return new ItemComponent(style, this)
                            .render(Value.of(mode),
                                    /* Actual value:
                                    {"id":"basilic","type":"CREATE","state":"TO_BUY","sequence":5007}
                                     */
                                    ItemTransitionValues.of(itemTransition)
                            ).javascriptCreate(
                                    elt -> getBodyElement().prepend(elt));
                }));
    }


    @GET
    @Path("{mode}")
    @Produces("text/html")
    @Transactional
    public String ui(@PathParam("mode") UiMode mode) {
        // Wondering it this is right from an SRP point-of-view, to let the shopping *page*
        // (which should be concerned about ui only) access the storage component...
        return render(new Data(mode, mode.load(store))).toString();
    }

    // TODO actually use getUrl() and getMime() from Resource
    @GET
    @Path("style.css")
    @Produces("text/css")
    public String style() {
        return style.render();
    }

    public Html render(Data data) {

        final ItemComponent itemComponent = new ItemComponent(style, this);

        return html(data, ImmutableList.of(
                style,
                new FavIconResource("favicon.png", "image/png")
                ), Lists.transform(data.items, i ->
                itemComponent.render(
                        Value.of(mode),
                        ItemTransitionValues.of(ItemTransition.forItem(data.mode, i)))
                ));
    }

    private static JsHtmlElement getElementForState(JsExpression state) {
        return getElementById(literal(ShoppingPage.ID_PREFIX).plus(state.dot("id")));
    }

    private JsStatement doAction(JsHtmlElement elt, Action action) {
        State targetState = action.to;
        return seq(socket.submit(
                        JsShoppingCommand.literal(
                                elt.id().substring(ShoppingPage.ID_PREFIX.length()),
                                literal(action.name()))),
                setState.invoke(obj(
                        "id", elt.id().substring(ShoppingPage.ID_PREFIX.length()),
                        "state", literal(style.forState.get(targetState)))));
    }
}
