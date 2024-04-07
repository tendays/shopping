package org.gamboni.shopping.server.ui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.gamboni.shopping.server.domain.Item;
import org.gamboni.shopping.server.domain.Items;
import org.gamboni.tech.web.js.JavaScript;
import org.gamboni.tech.web.ui.*;

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

                    @Override
                    public String getMime() {
                        return "image/png";
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
