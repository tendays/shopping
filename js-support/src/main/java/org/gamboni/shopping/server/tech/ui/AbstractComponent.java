package org.gamboni.shopping.server.tech.ui;

import com.google.common.collect.Iterables;

import java.util.List;
import java.util.function.Function;

import static org.gamboni.shopping.server.tech.ui.Html.attribute;

public abstract class AbstractComponent {

    public enum End {
        FRONT, BACK
    }

    protected final End end;

    protected AbstractComponent(End end) {
        this.end = end;
    }

    protected Html img(Css.ClassName style, Value<String> href) {
        return new Tag("img", attribute("class", style.name),
                attribute("src", href));
    }

    protected Html img(String href) {
        return new Tag("img",
                attribute("src", href));
    }

    protected Element div(List<Html.Attribute> attributes, Html... content) {
        return new Element("div", attributes, content);
    }

    protected <T> Element ul(Iterable<T> list, Function<T, Html> renderer) {
        return new Element("ul", Iterables.transform(list,
                e -> new Element("li", renderer.apply(e))));
    }
}
