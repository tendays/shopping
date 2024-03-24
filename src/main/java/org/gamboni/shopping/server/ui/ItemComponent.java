package org.gamboni.shopping.server.ui;

import org.gamboni.shopping.server.domain.State;
import org.gamboni.shopping.server.tech.ui.AbstractComponent;
import org.gamboni.shopping.server.tech.ui.Element;
import org.gamboni.shopping.server.tech.ui.Html;
import org.gamboni.shopping.server.tech.ui.Value;

import java.util.List;

import static org.gamboni.shopping.server.ui.ShoppingPage.ID_PREFIX;

public class ItemComponent extends AbstractComponent {
    private final Style style;
    private final Script script;

    public ItemComponent(End end, Style style, Script script) {
        super(end);
        this.style = style;
        this.script = script;
    }

    public Element render(Value<UiMode> mode, /*Item i*/Value<State> state, Value<String> image, Value<String> text) {
        // TODO 1. have a dto metamodel to make 'text', 'image' and 'state' accessible
        return div(List.of(style.forState.get(state),
                        Html.eventHandler("onclick", self -> script.actionForState.invoke(
                                mode.toExpression(), self)),
                        Html.attribute("id", Value.concat(Value.of(ID_PREFIX), text))),
                /*i.image().map(pp -> */img(style.image,
                        Value.concat(Value.of("/i/"), image)/*pp.getText()))
                        .orElse(Html.EMPTY)*/),
                div(List.of(style.text), Html.escape(text)
                )
        );
    }
}
