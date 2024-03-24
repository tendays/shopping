package org.gamboni.shopping.server.ui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.gamboni.shopping.server.domain.Item;
import org.gamboni.shopping.server.domain.Items;
import org.gamboni.shopping.server.tech.ui.*;

import java.util.List;

/**
 * @author tendays
 */
public class ShoppingPage extends AbstractPage {
    public static final String ID_PREFIX = "i-";
    private final Style style = new Style();
    private final Script script = new Script();

    private final ItemComponent itemComponent = new ItemComponent(end, style, script);
    public Html render(UiMode mode, List<Item> items) {
        return html(ImmutableList.of(style, script,
                new Resource() {
                    @Override
                    public String render() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public String getUrl() {
                        return "/favicon.png";
                    }

                    @Override
                    public Html asElement() {
                        return new Element("link",
                                List.of(Html.attribute("rel", "icon"),
                                        Html.attribute("href", getUrl())));
                    }
                }), Lists.transform(items, i ->
                itemComponent.render(
                        Value.of(mode),
                        Value.of(i.getState()),
                        Value.of(i.getImage().getText()),
                        Value.of(i.getText()))
                )).onLoad(script.poll.invoke(JavaScript.literal(mode.name()), JavaScript.literal(Items.nextSequence(items).orElse(0))));
    }
}
