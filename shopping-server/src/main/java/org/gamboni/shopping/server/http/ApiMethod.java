package org.gamboni.shopping.server.http;

import org.gamboni.shopping.server.ui.AbstractScript;
import org.gamboni.shopping.server.ui.HttpRequest;

import java.util.function.Function;

import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;
import spark.route.HttpMethod;

/**
 * @author tendays
 */
public interface ApiMethod<I, U, J extends HttpRequest> {

    void implement(Implementation<I> impl);
    U getUrl();
    J invoke();

    default <T> ApiMethod<TFunction<T, I>, Function<T, U>, HttpRequest.Parametric<J>> withHeader(Format<T> format, String header) {
        ApiMethod<I, U, J> base = this;
        return new ApiMethod<TFunction<T, I>, Function<T, U>, HttpRequest.Parametric<J>>() {
            @Override
            public void implement(Implementation<TFunction<T, I>> impl) {
                base.implement((req, res) -> {
                    T value;
                    try {
                        value = format.parse(req.headers(header));
                    } catch (Exception e) {
                        throw new BadRequestException();
                    }

                    return impl.run(req, res).execute(value);
                });
            }

            @Override
            public Function<T, U> getUrl() {
                return whatever -> base.getUrl();
            }

            @Override
            public HttpRequest.Parametric<J> invoke() {
                return new HttpRequest.Parametric<J>() {
                    @Override
                    public J param(AbstractScript.JsExpression value) {
                        // which one is called first? param() or setHeader()?
                        return copyHeaders(base.invoke());
                    }
                };
            }
        };
    }

    public interface NoParam extends ApiMethod<Object, String, HttpRequest> {
    }

    public interface OneParam<T> extends ApiMethod<TFunction<T, Object>, Function<T, String>, HttpRequest.Parametric<HttpRequest.Impl>> {
    }

    public interface TFunction<T, U> {
        U execute(T parameter) throws Exception;
    }

    public interface Implementation<N> {
        N run(Request req, Response res) throws Exception;
    }

    static abstract class AbstractOneParam<T> implements OneParam<T> {
        private final HttpMethod method;
        private final Format<T> format;

        AbstractOneParam(HttpMethod method, Format<T> format) {
            this.method = method;
            this.format = format;
        }

        protected abstract String extractParam(Request request);

        @Override
        public void implement(Implementation<TFunction<T, Object>> impl) {
            Route route = (req, res) -> {
                T value;
                try {
                    value = format.parse(extractParam(req));
                } catch (Exception e) {
                    return badRequest(res);
                }

                return impl.run(req, res).execute(value);
            };
            String url = getSparkUrl();
            route(method, url, route);
        }

        protected abstract String getSparkUrl();

        protected abstract AbstractScript.JsExpression getUrlExpression(AbstractScript.JsExpression parameterExpression);

        @Override
        public Function<T, String> getUrl() {
            return value -> getUrlFromFormatted(format.format(value));
        }

        @Override
        public HttpRequest.Parametric<HttpRequest.Impl> invoke() {
            return new HttpRequest.Parametric<HttpRequest.Impl>() {
                @Override
                public Impl param(AbstractScript.JsExpression value) {
                    return new HttpRequest.Impl(method, getUrlExpression(value), "");
                }

                @Override
                public void setHeader(String header, AbstractScript.JsExpression value) {

                }
            };
        }

        protected abstract String getUrlFromFormatted(String formatted);

        private static void route(HttpMethod method, String url, Route route) {
            switch (method) {
                case get:
                    Spark.get(url, route);
                    break;
                case post:
                    Spark.post(url, route);
                    break;
                case put:
                    Spark.put(url, route);
                    break;
                case patch:
                    Spark.patch(url, route);
                    break;
                case delete:
                    Spark.delete(url, route);
                    break;
                case head:
                    Spark.head(url, route);
                    break;
                case trace:
                    Spark.trace(url, route);
                    break;
                default:
                    throw new IllegalArgumentException(method.toString());
            }
        }
    }

    public static class WithParam<T> extends AbstractOneParam<T> {

        private final String before,after;
        private static final String PARAM_NAME = "arg";

        public WithParam(HttpMethod method, Format<T> format, String before, String after) {
            super(method, format);
            this.before = before;
            this.after = after;
        }

        @Override
        protected String extractParam(Request request) {
            return request.params(PARAM_NAME);
        }

        @Override
        protected String getSparkUrl() {
            return before +":"+ PARAM_NAME + after;
        }

        @Override
        protected AbstractScript.JsExpression getUrlExpression(AbstractScript.JsExpression parameterExpression) {
            return AbstractScript.literal(before).plus(parameterExpression).plus(after);
        }

        @Override
        protected String getUrlFromFormatted(String formatted) {
            return before + formatted + after;
        }
    }

    public static class WithSplat<T> extends AbstractOneParam<T> {

        private final String before,after;

        public WithSplat(HttpMethod method, Format<T> format, String before, String after) {
            super(method, format);
            this.before = before;
            this.after = after;
        }

        @Override
        protected String extractParam(Request request) {
            String[] splat = request.splat();
            if (splat.length != 1) { throw new BadRequestException(); }
            return splat[0].trim();
        }

        @Override
        protected String getSparkUrl() {
            return before +"*" + after;
        }

        @Override
        protected String getUrlFromFormatted(String formatted) {
            return before + formatted + after;
        }

        @Override
        protected AbstractScript.JsExpression getUrlExpression(AbstractScript.JsExpression parameterExpression) {
            return AbstractScript.literal(before).plus(parameterExpression).plus(after);
        }
    }

    public static Object badRequest(Response res) {
        res.status(400);
        return "Bad request";
    }
}
