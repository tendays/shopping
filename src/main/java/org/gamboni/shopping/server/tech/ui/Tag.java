package org.gamboni.shopping.server.tech.ui;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.gamboni.shopping.server.tech.ui.JavaScript.*;

/**
 * @author tendays
 */
public class Tag implements Html {
    private final String name;
    private final Iterable<? extends Attribute> attributes;

    public Tag(String name, Iterable<? extends Attribute> attributes) {
        this.name = name;
        this.attributes = attributes;
    }
    public Tag(String name, Attribute... attributes) {
        this(name, Arrays.asList(attributes));
    }

    public String toString() {
        return "<"+ name + (Iterables.isEmpty(attributes) ? "" : " "+ renderAttributes(attributes)) +">";
    }

    @Override
    public JsStatement javascriptCreate(Function<JsHtmlElement, JsStatement> continuation) {
        return let(JavaScript.createElement(name), JsHtmlElement::new,
                elt -> {
                    var statements = new ArrayList<JsStatement>();
                    for (var attr : attributes) {
                        statements.add(attr.javascriptCreate(elt));
                    }
                    statements.add(
                            continuation.apply(elt));
                    return seq(statements);
                });
    }

    static String renderAttributes(Iterable<? extends Attribute> attributes) {
        return Streams.stream(attributes).map(Attribute::render).collect(Collectors.joining(" "));
    }
}
