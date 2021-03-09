package org.gamboni.shopping.server.ui;

/**
 * @author tendays
 */
public interface Html {
    static final Html EMPTY = new Html() {
        public String toString() { return ""; }
    };

    static Html escape(String text) {
        return new Html() {
            public String toString() {
                return text.replace("&", "amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;");
            }
        };
    }

    static String quote(Css.ClassName className) {
        return quote(className.name);
    }

    static String quote(String attribute) {
        return '"' + attribute
                .replace("&", "&amp;")
                .replace("\"", "&quot;") + '"';
    }

    static Attribute attribute(String name, String value) {
        return new Attribute() {
            @Override
            public String getAttributeName() {
                return name;
            }

            @Override
            public String getAttributeValue() {
                return value;
            }
        };
    }

    interface Attribute {
        String getAttributeName();
        String getAttributeValue();

        default String render() {
            return getAttributeName() +"="+ quote(getAttributeValue());
        }
    }
}
