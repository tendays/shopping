package org.gamboni.shopping.server.ui;

import com.google.common.collect.Iterables;

import java.util.List;
import java.util.function.Function;

import static org.gamboni.shopping.server.ui.Html.quote;

/**
 * @author tendays
 */
public class AbstractPage {
    protected Html img(Css.ClassName style, String href) {
        return new Tag("img class="+ quote(style) +" src=" + quote(href));
    }

    protected Html img(String href) {
        return new Tag("img src=" + quote(href));
    }

    protected Element div(List<Html.Attribute> attributes, Html... content) {
        return new Element("div", attributes, content);
    }

    protected Element html(Element... body) {
        return new Element("html",
                new Element("body", body));
    }

    protected Element html(Iterable<Resource> dependencies, Iterable<Element> body) {
        return new Element("html",
                new Element("head", Iterables.transform(dependencies, Resource::asElement)),
                new Element("body", body));
    }

    protected <T> Element ul(Iterable<T> list, Function<T, Html> renderer) {
        return new Element("ul", Iterables.transform(list,
                e -> new Element("li", renderer.apply(e))));
    }
}
