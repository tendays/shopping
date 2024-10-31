package org.gamboni.shopping.server.ui;

import org.gamboni.shopping.server.domain.NewItemEventValues;
import org.gamboni.shopping.server.domain.State;
import org.gamboni.tech.history.ui.EnumViewElementTemplate;
import org.gamboni.tech.web.ui.AbstractComponent;
import org.gamboni.tech.web.ui.Html;
import org.gamboni.tech.web.ui.IdentifiedElementRenderer;
import org.gamboni.tech.web.ui.Value;

import java.util.List;

public class ItemComponent extends AbstractComponent {
    public static final ItemComponent INSTANCE = new ItemComponent();

    public IdentifiedElementRenderer<NewItemEventValues> addTo(ShoppingPage page) {
        return EnumViewElementTemplate.ofDynamicBase(State.class,
                NewItemEventValues::id,
                NewItemEventValues::state,
                event -> div(List.of(Html.eventHandler("onclick", page.actionForState::invoke)),
                        img(page.style.image, Value.concat(Value.of("/i/"), event.image())),
                        div(List.of(page.style.text), Html.escape(event.id())
                        )))
                .withElementKey("i")
                .withStyle(page.style.forState)
                .addTo(page);
    }
}
