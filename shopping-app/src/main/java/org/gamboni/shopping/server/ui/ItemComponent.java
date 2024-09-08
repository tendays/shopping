package org.gamboni.shopping.server.ui;

import org.gamboni.shopping.server.domain.ItemTransitionValues;
import org.gamboni.tech.web.ui.AbstractComponent;
import org.gamboni.tech.web.ui.Element;
import org.gamboni.tech.web.ui.Html;
import org.gamboni.tech.web.ui.Value;

import java.util.List;

import static org.gamboni.shopping.server.ui.ShoppingPage.ID_PREFIX;

public class ItemComponent extends AbstractComponent {
    private final Style style;
    private final ShoppingPage page;

    public ItemComponent(Style style, ShoppingPage page) {
        this.style = style;
        this.page = page;
    }

    public Element render(Value<UiMode> mode, ItemTransitionValues item) {
        return div(List.of(style.forState.get(item.state()),
                        Html.eventHandler("onclick", self -> page.actionForState.invoke(
                                mode.toExpression(), self)),
                        Html.attribute("id", Value.concat(Value.of(ID_PREFIX), item.id()))),
                img(style.image, Value.concat(Value.of("/i/"), item.image())),
                div(List.of(style.text), Html.escape(item.id())
                )
        );
    }
}
