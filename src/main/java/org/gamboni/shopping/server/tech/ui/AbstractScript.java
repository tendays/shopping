package org.gamboni.shopping.server.tech.ui;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author tendays
 */
public abstract class AbstractScript implements Resource {

    @Override
    public String getUrl() {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, getClass().getSimpleName()) +".js";
    }

    @Override
    public Html asElement() {
        return new Element("script",
                "type='text/javascript' src="+ Html.quote(getUrl()));
    }

    public static JsExpression literal(Css.ClassName className) {
        return literal(className.name);
    }

    public static JsExpression literal(String text) {
        return new JsExpression("'"+ text.replace("\\", "\\\\")
                .replace("'", "\\'")
                +"'");
    }

    public static class JsExpression {
        protected final String code;

        public JsExpression(String code) {
            this.code = code;
        }

        public JsExpression plus(JsExpression that) {
            return new JsExpression(this +"+"+ that);
        }

        public JsString plus(String that) {
            return new JsString(this +"+"+ literal(that));
        }

        public String toString() {
            return code;
        }

        public JsExpression dot(String attr) {
            return new JsExpression(this +"."+ attr);
        }

        public JsExpression eq(String value) {
            return new JsExpression(this +" === "+ literal(value));
        }
    }

    protected static class JsHtmlElement extends JsExpression {
        public JsHtmlElement(String code) { super(code); } // needed to work with let()
        public JsHtmlElement(JsExpression code) {
            super(code.code);
        }

        public JsDOMTokenList classList() {
            return new JsDOMTokenList(this +".classList");
        }

        public JsString id() {
            return new JsString(this +".id");
        }
    }

    protected static class JsString extends JsExpression {
        public JsString(String code) {
            super(code);
        }

        public JsString substring(int len) {
            return new JsString(code +".substring("+ len +")");
        }
    }

    protected static class JsDOMTokenList extends JsExpression {
        public JsDOMTokenList(String code) {
            super(code);
        }

        public JsExpression contains(Css.ClassName className) {
            return new JsExpression(this +".contains("+ literal(className) +")");
        }

        public JsString item(int index) {
            return new JsString(this +".item("+ index +")");
        }

        public String remove(JsExpression item) {
            return this +".remove("+ item +")";
        }

        public String add(JsExpression item) {
            return this +".add("+ item +")";
        }
    }

    protected JsStatement block(Object... statements) {
        return new JsStatement() {
            @Override
            public String toString() {
                return "{"+ Arrays.stream(statements).map(s -> s +";").collect(Collectors.joining()) +"}";
            }
        };
    }

    protected <T> String let(T value, Function<String, T> newInstance, Function<T, JsStatement> code) {
        String var = "e";
        //try {
            return "let "+ var +" = "+ value +";" +
                    code.apply(newInstance.apply(var));
            // not working in Quarkus (T)value.getClass().getDeclaredConstructor(String.class).newInstance(var));
        /*} catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }*/
    }

    protected JsHtmlElement getElementById(JsExpression id) {
        return new JsHtmlElement(new JsExpression("document.getElementById("+ id +")"));
    }

    protected IfBlock _if(String condition, JsStatement body) {
        return _if(new JsExpression(condition), body);
    }

    protected IfBlock _if(JsExpression condition, JsStatement body) {
        return new IfBlock(condition, body);
    }

    protected interface JsStatement {}

    protected static class IfBlock implements JsStatement {
        private final JsExpression condition;
        private final JsStatement body;

        public IfBlock(JsExpression condition, JsStatement body) {
            this.condition = condition;
            this.body = body;
        }

        public IfChain _elseIf(JsExpression condition, JsStatement body) {
            return new IfChain(this, condition, body);
        }

        public String toString() {
            return "if ("+ condition +")"+ body;
        }
    }

    protected static class IfChain extends IfBlock {
        private final IfBlock previous;

        public IfChain(IfBlock previous, JsExpression condition, JsStatement body) {
            super(condition, body);
            this.previous = previous;
        }

        public String toString() {
            return previous +" else "+ super.toString();
        }
    }

    /**
     *
     * @param method HTTP method to use
     * @param url escaped URL
     * @param headers non-escaped header to escaped value
     * @param body escaped http body
     * @param callback escaped callback
     * @return code sending the request
     */
    public static String http(String method, String url, Map<String, String> headers, String body, Function<JsString, String> callback/*, Function<>*/) {
        String var = "x";
        return "let "+ var +" = new XMLHttpRequest();" +
                var +".onload = () => {" +
                callback.apply(new JsString(var+".responseText")) +
                "};" +
                var +".open("+ literal(method)+", "+ url +");" +
                headers.entrySet().stream().map(header -> var +".setRequestHeader("+ literal(header.getKey())+", "+ header.getValue() +");")
                .collect(Collectors.joining())+
                var +".send("+ body +");";
    }

    protected String function(String name, Supplier<String> body) {
        return "function "+ name +"() {\n" +
                body.get() +"\n"+
                "}";
    }

    protected String function(String name, Function<JsExpression, String> body) {
        String arg = "a";
        return "function "+ name +"("+ arg +") {\n" +
                body.apply(new JsExpression(arg)) +"\n"+
                "}";
    }

    protected String function(String name, BiFunction<JsExpression, JsExpression, JsStatement> body) {
        String arg1 = "a";
        String arg2 = "b";
        return "function "+ name +"("+ arg1 +","+ arg2 +") {\n" +
                body.apply(new JsExpression(arg1), new JsExpression(arg2)) +"\n"+
                "}";
    }

    protected String obj(Map<String, JsExpression> values) {
        return "{"+ values.entrySet().stream().map(e -> literal(e.getKey()) +":"+ e.getValue()).collect(Collectors.joining(",")) +"}";
    }

    protected String obj(String k1, JsExpression v1, String k2, JsExpression v2) {
        return obj(ImmutableMap.of(k1, v1, k2, v2));
    }
}
