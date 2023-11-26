package org.gamboni.shopping.server.tech.ui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.List;
import java.util.function.Function;

/**
 * @author tendays
 */
public class AbstractPage {
    protected Html img(Css.ClassName style, String href) {
        return new Tag("img class="+ Html.quote(style) +" src=" + Html.quote(href));
    }

    protected Html img(String href) {
        return new Tag("img src=" + Html.quote(href));
    }

    protected Element div(List<Html.Attribute> attributes, Html... content) {
        return new Element("div", attributes, content);
    }

    protected HtmlElement html(Iterable<Resource> dependencies, Iterable<Element> body) {
        return new HtmlElement(dependencies, body, ImmutableList.of());
    }

    protected static class HtmlElement extends Element {
        private final Iterable<Resource> dependencies;
        private final Iterable<Element> body;
        private final List<Html.Attribute> bodyAttributes;

        public HtmlElement(Iterable<Resource> dependencies, Iterable<Element> body, List<Html.Attribute> bodyAttributes) {
            super("html",
                    new Element("head", Iterables.transform(dependencies, Resource::asElement)),
                    new Element("body", bodyAttributes, body));
            this.dependencies = dependencies;
            this.body = body;
            this.bodyAttributes = bodyAttributes;
        }

        public HtmlElement onLoad(AbstractScript.JsExpression code) {
            return new HtmlElement(dependencies, body, ImmutableList.<Attribute>builder()
            .addAll(bodyAttributes)
            .add(Html.attribute("onload", code.toString()))
            .build());
        }
    }

    protected <T> Element ul(Iterable<T> list, Function<T, Html> renderer) {
        return new Element("ul", Iterables.transform(list,
                e -> new Element("li", renderer.apply(e))));
    }
}
