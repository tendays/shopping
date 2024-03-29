package org.gamboni.shopping.server.tech.ui;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.gamboni.shopping.server.tech.js.JavaScript;
import org.gamboni.shopping.server.tech.js.JavaScript.JsHtmlElement;
import org.gamboni.shopping.server.tech.js.JavaScript.JsStatement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.gamboni.shopping.server.tech.js.JavaScript.let;
import static org.gamboni.shopping.server.tech.ui.Tag.renderAttributes;

/**
 * @author tendays
 */
public class Element implements Html {
    private final String name;
    private final Iterable<? extends Attribute> attributes;
    private final Iterable<? extends Html> contents;

    public Element(String name, Iterable<? extends Attribute> attributes, Iterable<? extends Html> contents) {
        this.name = name;
        this.attributes = attributes;
        this.contents = contents;
    }

    public Element(String name, Iterable<? extends Attribute> attributes, Html... contents) {
        this(name, attributes, Arrays.asList(contents));
    }


    public Element(String name, Html... contents) {
        this(name, List.of(), ImmutableList.copyOf(contents));
    }

    public Element(String name, Iterable<? extends Html> contents) {
        this(name, List.of(), contents);
    }

    public String toString() {
        return "<"+ name + (Iterables.isEmpty(attributes) ? "" : " "+ renderAttributes(attributes)) +">"+
                Joiner.on("").join(contents)
                +"</"+ name +">";
    }

    @Override
    public JsStatement javascriptCreate(Function<JsHtmlElement, JsStatement> continuation) {
        return let(JavaScript.createElement(name), JsHtmlElement::new,
                elt -> {
            var statements = new ArrayList<JsStatement>();
                    for (var attr : attributes) {
                        statements.add(attr.javascriptCreate(elt));
                    }
                    for (var child : contents) {
                        statements.add(child.javascriptCreate(
                                childRef ->
                                        JsStatement.of(elt.invoke("appendChild", childRef))
                        ));
                    }
                    statements.add(
                            continuation.apply(elt));
                    return JavaScript.seq(statements);
                });
    }
}
