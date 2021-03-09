package org.gamboni.shopping.server.ui;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author tendays
 */
public class Element implements Html {
    private final String name;
    private final String attributes;
    private final Iterable<? extends Html> contents;

    public Element(String name, Iterable<? extends Attribute> attributes, Iterable<? extends Html> contents) {
        this(name, renderAttributes(attributes), contents);
    }

    public Element(String name, Iterable<? extends Attribute> attributes, Html... contents) {
        this(name, renderAttributes(attributes), Arrays.asList(contents));
    }

    private static String renderAttributes(Iterable<? extends Attribute> attributes) {
        return Streams.stream(attributes).map(Attribute::render).collect(Collectors.joining(" "));
    }

    public Element(String name, Html... contents) {
        this(name, "", ImmutableList.copyOf(contents));
    }

    public Element(String name, String attributes, Html... contents) {
        this(name, attributes, ImmutableList.copyOf(contents));
    }

    public Element(String name, Iterable<? extends Html> contents) {
        this(name, "", contents);
    }

    public Element(String name, String attributes, Iterable<? extends Html> contents) {
        this.name = name;
        this.attributes = attributes;
        this.contents = contents;
    }

    public String toString() {
        return "<"+ name + (attributes.isEmpty() ? "" : " "+attributes) +">"+
                Joiner.on("").join(contents)
                +"</"+ name +">";
    }
}
