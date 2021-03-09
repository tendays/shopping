package org.gamboni.shopping.server.ui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.gamboni.shopping.server.domain.Item;

import java.util.List;
import java.util.function.Function;

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
                )));
    }
}
