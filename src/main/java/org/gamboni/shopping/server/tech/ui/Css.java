package org.gamboni.shopping.server.tech.ui;

import com.google.common.base.CaseFormat;
import com.google.common.reflect.Reflection;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author tendays
 */
public abstract class Css implements Resource {
    public String getUrl() {
        return "/"+ CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, getClass().getSimpleName().toLowerCase()) +".css";
    }

    @Override
    public Html asElement() {
        return new Element("link", "rel='stylesheet' href=" + Html.quote(getUrl()));
    }

    protected String rule(Selector selector, Property... attributes) {
        return selector.renderSelector() +" {\n" +
                Stream.of(attributes).map(Objects::toString).collect(Collectors.joining())
                +"}\n";
    }

    /** A Property factory */
    public interface Properties {
        // TODO later: make Css be abstract and implement Properties instead
        Properties INSTANCE = Reflection.newProxy(Properties.class, (proxy, method, args) ->
                new Property(
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN,
                                method.getName().startsWith("_")? method.getName().substring(1) : method.getName()),
                        args[0].toString())
        );

        Property width(String value);
        Property height(String value);
        Property _float(String value);
        Property position(String value);
        Property backgroundColor(String value);
        Property fontSize(String value);
        Property fontWeight(String value);
        Property color(String value);
        Property content(String value);
        Property top(String value);
        Property right(String value);
        Property borderBottomRightRadius(String value);
        Property padding(String value);
        Property filter(String value);
    }

    /** A css property (Something:something;) */
    protected static class Property {
        public final String name;
        public final String value;

        public Property(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public static Property width(String value) {
            return new Property("width", value);
        }

        public String toString() {
            return "  "+ name +": "+ value +";\n";
        }
    }

    public interface Selector {

        Selector NOTHING = new Selector() {
            @Override
            public String renderSelector() {
                throw new UnsupportedOperationException();
            }

            public Selector or(Selector that) {
                return that;
            }
        };

        String renderSelector();

        /** this, that */
        default Selector or(Selector that) {
            return () -> this.renderSelector() +", "+ that.renderSelector();
        }

        /** "this that" */
        default Selector child(Selector that) {
            return () -> this.renderSelector() +" "+ that.renderSelector();
        }

        /** "this::before" */
        default Selector before() {
            return () -> this.renderSelector() +"::before";
        }
    }

    public static class ClassName implements Selector, Html.Attribute {
        final String name;
        public ClassName(String name) {
            this.name = name;
        }

        public String renderSelector() {
            return "."+ name;
        }

        public String toString() {
            return this.name;
        }

        @Override
        public String getAttributeName() {
            return "class";
        }

        @Override
        public String getAttributeValue() {
            return name;
        }
    }

    {
        try {
            for (Field f : getClass().getDeclaredFields()) {
                if (f.getType() == ClassName.class && f.get(this) == null) {
                    f.set(this, new ClassName(f.getName()));
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
