package org.gamboni.shopping.server.ui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.gamboni.shopping.server.domain.Item;
import org.gamboni.shopping.server.domain.Items;
import org.gamboni.tech.web.js.JavaScript;
import org.gamboni.tech.web.ui.AbstractPage;
import org.gamboni.tech.web.ui.FavIconResource;
import org.gamboni.tech.web.ui.Html;
import org.gamboni.tech.web.ui.Value;

import java.util.List;

/**
 * @author tendays
 */
@ApplicationScoped
public class ShoppingPage extends AbstractPage {
    public static final String ID_PREFIX = "i-";

    @Inject
    Style style;

    @Inject
    Script script;

    public Html render(UiMode mode, List<Item> items) {

        final ItemComponent itemComponent = new ItemComponent(end, style, script);

        return html(ImmutableList.of(
                style,
                script,
                new FavIconResource("favicon.png", "image/png")
                ), Lists.transform(items, i ->
                itemComponent.render(
                        Value.of(mode),
                        Value.of(i.getState()),
                        Value.of(i.getImage().getText()),
                        Value.of(i.getText()))
                )).onLoad(script.init.invoke(JavaScript.literal(mode.name()), JavaScript.literal(Items.nextSequence(items).orElse(0))));
    }
}
