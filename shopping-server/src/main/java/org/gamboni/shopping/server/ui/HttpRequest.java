package org.gamboni.shopping.server.ui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import spark.route.HttpMethod;

import static org.gamboni.shopping.server.ui.AbstractScript.literal;

/**
 * @author tendays
 */
public interface HttpRequest {

    void setHeader(String header, AbstractScript.JsExpression value);

    public static class Impl implements HttpRequest {
        private final HttpMethod method;
        private final AbstractScript.JsExpression url;
        private final String body;
        private final Map<String, AbstractScript.JsExpression> headers = new HashMap<>();

        public Impl(HttpMethod method, AbstractScript.JsExpression url, String body) {
            this.method = method;
            this.url = url;
            this.body = body;
        }
        String subscribe(Function<String, String> callback) {
            String var = "x";
            return "let "+ var +" = new XMLHttpRequest();" +
                    var +".onreadystatechange = () => {" +
                    "if ("+ var+".readyState != 4) return;" +
                    callback.apply(var+",responseText") +
                    "};" +
                    var +".open("+ literal(method.name().toUpperCase(Locale.ROOT))+", "+ url +");" +
                    headers.entrySet().stream().map(header -> var +".setRequestHeader("+ literal(header.getKey())+", "+ header.getValue() +");")
                            .collect(Collectors.joining())+
                    var +".send("+ body +");";
        }

        @Override
        public void setHeader(String header, AbstractScript.JsExpression value) {
            headers.put(header, value);
        }
    }


    public static abstract class Parametric<N extends HttpRequest> implements HttpRequest {
        private final Map<String, AbstractScript.JsExpression> headers = new HashMap<>();
        protected N copyHeaders(N next) {
            headers.forEach(next::setHeader);
            return next;
        }

        @Override
        public void setHeader(String header, AbstractScript.JsExpression value) {
            headers.put(header, value);
        }

        public abstract N param(AbstractScript.JsExpression value);
    }

}
