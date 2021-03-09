package org.gamboni.shopping.server.http;

import org.gamboni.shopping.server.domain.Action;
import org.gamboni.shopping.server.ui.HttpRequest;

import java.util.function.Function;

import spark.route.HttpMethod;

/**
 * @author tendays
 */
public class ShoppingApi {
    public static final ApiMethod<
            ApiMethod.TFunction<Action, ApiMethod.TFunction<String, Object>>,
            Function<Action, Function<String, String>>,
            HttpRequest.Parametric<HttpRequest.Parametric<HttpRequest.Impl>>>
            ACTION = new ApiMethod.WithSplat<>(HttpMethod.post, Format.STRING, "/l/", "")
            .withHeader(Format.ofEnum(Action.class), "X-Shopping-Action");

    public static final ApiMethod.WithParam<Long> WATCH = new ApiMethod.WithParam<>(HttpMethod.get, Format.LONG, "/a/", "");
}
