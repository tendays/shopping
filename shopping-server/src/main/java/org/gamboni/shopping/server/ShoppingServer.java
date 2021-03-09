package org.gamboni.shopping.server;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import org.gamboni.shopping.server.domain.Action;
import org.gamboni.shopping.server.domain.Item;
import org.gamboni.shopping.server.domain.ItemState;
import org.gamboni.shopping.server.domain.Item_;
import org.gamboni.shopping.server.domain.ProductPicture;
import org.gamboni.shopping.server.domain.Store;
import org.gamboni.shopping.server.domain.WatchResult;
import org.gamboni.shopping.server.http.ApiMethod;
import org.gamboni.shopping.server.http.BadRequestException;
import org.gamboni.shopping.server.http.ShoppingApi;
import org.gamboni.shopping.server.ui.Script;
import org.gamboni.shopping.server.ui.Style;
import org.gamboni.shopping.server.ui.ShoppingPage;
import org.gamboni.shopping.server.ui.UiMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import spark.Response;
import spark.Spark;

import static org.gamboni.shopping.server.http.ApiMethod.badRequest;

/**
 * @author tendays
 */
public class ShoppingServer {
    public static final int PORT = 4570;
    private static final File IMAGE_PATH = new File("images");

    public static void main(String[] a) {
        Spark.port(PORT);

        Store store = new Store();
        final List<Consumer<Item>> watchers = new ArrayList<>();

        Spark.exception(BadRequestException.class, (t, req, res) -> {
            t.printStackTrace();
            res.status(400);
            res.body("Bad request");
                });

        Spark.exception(Exception.class, (t, req, res) -> {
            t.printStackTrace();
            res.status(500);
            res.body("Internal server error");
        });

        Style style = new Style();
        Spark.get(style.getUrl(), (req, res) -> style.render());

        Script script = new Script();
        Spark.get(script.getUrl(), (req, res) -> script.render());

        Spark.get("/", (req, res) -> "ok");

        for (UiMode mode : UiMode.values()) {
            Spark.get("/"+ mode.name().toLowerCase(), (req, res) -> store.transaction(s -> new ShoppingPage().render(mode, mode.load(s))));
        }

        /** Load a product image */
        Spark.get("/i/:text", (req, res) -> store.transaction(s -> {
            final Optional<ProductPicture> pp = s.getProductPicture(req.params("text"));
            if (!pp.isPresent()) {
                res.status(404);
                return "Not found";
            }

            res.header("Content-Type", "image/jpeg");

            try (InputStream in = new FileInputStream(new File(IMAGE_PATH, pp.get().getFile()))) {
                ByteStreams.copy(in, res.raw().getOutputStream());
            }
            return "";
        }));

        /** List all items */
        Spark.get("/l", (req, res) -> store.transaction(s -> Joiner.on('\n').join(s.getAllItems())));

        /** Perform an {@link Action} on an item */
        ShoppingApi.ACTION.implement((req, res) -> action -> name -> store.transaction(s -> {
            if (name.isEmpty()) {
                throw new BadRequestException();
            }

            final Item item = s.getItemByName(name);
            if (action.from.contains(item.getState())) {
                item.setState(action.to);
                item.setSequence(s.nextSequence());
                synchronized (watchers) {
                    System.out.println("Notifying "+ watchers.size() +" watchers");
                    watchers.forEach(w -> w.accept(item));
                    watchers.clear();
                    watchers.notifyAll();
                }
            }
            return item.getState().name();
        }));

        /** Get actions that occurred a given sequence value. If none occurred, wait a minute before returning */
        ShoppingApi.WATCH.implement((req, res) -> since -> store.transaction(s -> {
            final List<Item> items = s.search(Item.class, (query, root) ->
                    query.where(s.cb.gt(root.get(Item_.sequence), since)))
                    .getResultList();

            if (items.isEmpty()) {
                synchronized (watchers) {
                    watchers.add(items::add);

                    try {
                        watchers.wait(60_000);
                    } catch (InterruptedException e) {
                        /* Ok: return early. */
                    }
                }
            }

            return new WatchResult(Lists.transform(items,
                    i -> new ItemState(i.getText(), i.getState())),
                    ShoppingApi.WATCH.getUrl().apply(items.stream().mapToLong(Item::getSequence).max().orElse(since))
                    );
        }));
    }
}
