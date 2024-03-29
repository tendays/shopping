package org.gamboni.shopping.server.http;

import jakarta.ws.rs.HttpMethod;
import org.gamboni.shopping.server.domain.Action;
import org.gamboni.shopping.server.tech.http.ApiMethod;
import org.gamboni.shopping.server.tech.http.Format;
import org.gamboni.shopping.server.tech.ui.HttpRequest;

import java.util.function.Function;

/**
 * @author tendays
 */
public class ShoppingApi {
    public static final ApiMethod<
                ApiMethod.TFunction<Action, ApiMethod.TFunction<String, Object>>,
                Function<Action, Function<String, String>>,
                HttpRequest.Parametric<HttpRequest.Parametric<HttpRequest.Impl>>>
            ACTION = new ApiMethod.WithSplat<>(HttpMethod.POST, Format.STRING, "/l/", "")
            .withHeader(Format.ofEnum(Action.class), "X-Shopping-Action");

    public static final String SOCKET_URL = "/sock";
}
