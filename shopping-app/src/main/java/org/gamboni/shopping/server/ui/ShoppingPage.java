package org.gamboni.shopping.server.ui;

import com.google.common.collect.ImmutableList;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.gamboni.shopping.server.domain.*;
import org.gamboni.tech.history.Stamped;
import org.gamboni.tech.history.ui.ArrayElement;
import org.gamboni.tech.quarkus.QuarkusDynamicPage;
import org.gamboni.tech.web.js.JavaScript;
import org.gamboni.tech.web.ui.FavIconResource;
import org.gamboni.tech.web.ui.Html;
import org.gamboni.tech.web.ui.IdentifiedElementRenderer;
import org.gamboni.tech.web.ui.Renderer;

import java.util.List;
import java.util.stream.Stream;

import static org.gamboni.tech.web.js.JavaScript.*;

/**
 * @author tendays
 */
@ApplicationScoped
@Path("/")
public class ShoppingPage extends QuarkusDynamicPage<ShoppingPage.Data> {
    private IdentifiedElementRenderer<NewItemEventValues> itemComponent;
    private Renderer<Stream<NewItemEventValues>> arrayElement;

    public record Data(UiMode mode, List<Item> items) implements Stamped {
        @Override
        public long stamp() {
            return Items.nextSequence(items).orElse(0);
        }
    }

    @Inject
    Style style;

    @Inject Store store;

    public final JavaScript.Fun1 actionForState = new JavaScript.Fun1("actionForState");

    private final JsGlobal mode = new JsGlobal("mode");

        @Override
        protected JavaScript.JsExpression helloValue(JavaScript.JsExpression sequence) {
            return JsHello.literal(
                    mode,
                    sequence);
        }

    @PostConstruct
    void init() {
        this.itemComponent = ItemComponent.INSTANCE.addTo(this);

        this.arrayElement = ArrayElement.withRemoval(NewItemEventValues::id, itemComponent)
                .withNewElementHandler(
                        // TODO this entire element matcher could be generated as part of JsNewItemEvent
                        (event, matcher) -> {
                            var newElement = new JsNewItemEvent(event);
                            matcher.expect(newElement.dot("@type").eq(literal(NewItemEvent.class.getSimpleName())));
                            return newElement;
                        },
                        NewItemEventValues::of,
                        ArrayElement.AddAt.START)
                .addTo(this);

        addToOnLoad(onLoad -> mode.set(onLoad.addParameter(data -> literal(data.mode))));

        addToScript(
                mode.declare(_null), // initialised by onLoad()

                actionForState.declare(e -> {
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
                ));
    }


    @GET
    @Path("{mode}")
    @Produces("text/html")
    @Transactional
    public String ui(@PathParam("mode") UiMode mode) {
        return render(new Data(mode, mode.load(store))).toString();
    }

    @GET
    @Path("style.css")
    @Produces("text/css")
    public String style() {
        return style.render();
    }

    public Html render(Data data) {
        return html(data, ImmutableList.of(
                style,
                new FavIconResource("favicon.png", "image/png")
        ), arrayElement.render(data.items
                .stream()
                .map(NewItemEvent::forItem)
                .map(NewItemEventValues::of)));
    }

    private JsStatement doAction(JsHtmlElement elt, Action action) {
        State targetState = action.to;
        return seq(submitMessage(
                        JsShoppingCommand.literal(
                                itemComponent.getIdFromElement(elt),
                                literal(action.name()))),

                elt.classList().remove(elt.classList().item(0)),
                elt.classList().add(literal(style.forState.get(targetState)))
        );
    }
}
