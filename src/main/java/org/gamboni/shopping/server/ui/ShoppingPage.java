package org.gamboni.shopping.server.ui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.gamboni.shopping.server.domain.Item;
import org.gamboni.shopping.server.domain.Items;
import org.gamboni.shopping.server.tech.ui.AbstractPage;
import org.gamboni.shopping.server.tech.ui.AbstractScript;
import org.gamboni.shopping.server.tech.ui.Html;

import java.util.List;

/**
 * @author tendays
 */
public class ShoppingPage extends AbstractPage {
    public static final String ID_PREFIX = "i-";
    private final Style style = new Style();
    private final Script script = new Script();
    public Html render(UiMode mode, List<Item> items) {
        return html(ImmutableList.of(style, script), Lists.transform(items, i ->
                div(ImmutableList.of(style.forState.get(i.getState()),
                        Html.attribute("onclick", script.actionForState +"("+ AbstractScript.literal(mode.name())+", this)"),
                        Html.attribute("id", ID_PREFIX + i.getText())),
                        i.image().map(pp -> img(style.image, "/i/" + pp.getText()))
                                .orElse(Html.EMPTY),
                        div(ImmutableList.of(style.text), Html.escape(i.getText())
                        )
                ))).onLoad(new AbstractScript.JsExpression(script.poll +"("+ Items.nextSequence(items).orElse(0) +")"));
    }
}
