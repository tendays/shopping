package org.gamboni.shopping.server;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.StreamingOutput;
import org.gamboni.shopping.server.domain.*;
import org.gamboni.shopping.server.tech.http.BadRequestException;
import org.gamboni.shopping.server.ui.Script;
import org.gamboni.shopping.server.ui.ShoppingPage;
import org.gamboni.shopping.server.ui.Style;
import org.gamboni.shopping.server.ui.UiMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author tendays
 */
@Path("/")
public class ShoppingServer {
    @Inject
    Store s;
    public static final int PORT = 4570;
    private static final File IMAGE_PATH = new File("images");

    final List<Consumer<Item>> watchers = new ArrayList<>();

    /*        Spark.exception(BadRequestException.class, (t, req, res) -> {
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
    */
    @GET
    @Path("ping")
    public String ping() {
        return "ok";
    }

    @GET
    @Path("style.css")
    public String style() {
        return new Style().render();
    }

    @GET
    @Path("/script.js")
    public String script() {
        return new Script().render();
    }

    @GET
    @Path("/{mode}")
    @Produces("text/html")
    @Transactional
    public String ui(@PathParam("mode") UiMode mode) {
        return new ShoppingPage().render(mode, mode.load(s)).toString();
    }

    /**
     * Load a product image
     */
    @GET
    @Path("/i/{text}")
    @Produces("image/jpeg")
    @Transactional
    public StreamingOutput productImage(@PathParam("text") String text) throws IOException {
        final Optional<ProductPicture> pp = s.getProductPicture(
                text);
        if (pp.isEmpty()) {
            throw new EntityNotFoundException();
        }
        return out -> {
            try (InputStream in = new FileInputStream(new File(IMAGE_PATH, pp.get().getFile()))) {
                ByteStreams.copy(in, out);
            }
        };
    }


    /**
     * List all items
     */
    @GET
    @Path("/l")
    @Transactional
    public String allItems() {
        return Joiner.on('\n').join(s.getAllItems());
    }

    /**
     * Perform an {@link Action} on an item
     */
    @POST
    @Path("/l/{name}")
    @Transactional
    public String action(@PathParam("name") String name, @HeaderParam("X-Shopping-Action") Action action) {
        if (name.isEmpty()) {
            throw new BadRequestException();
        }

        final Item item = s.getItemByName(name);
        if (action.from.contains(item.getState())) {
            item.setState(action.to);
            item.setSequence(s.nextSequence());
            synchronized (watchers) {
                System.out.println("Notifying " + watchers.size() + " watchers");
                watchers.forEach(w -> w.accept(item));
                watchers.clear();
                watchers.notifyAll();
            }
        }
        return item.getState().name();
    }

    /**
     * Get actions that occurred a given sequence value. If none occurred, wait a minute before returning
     */
    @GET
    @Path("/a/{since}")
    @Transactional
    public WatchResult watch(@PathParam("since") long since) {
        var cb = s.getEm().getCriteriaBuilder();
        final List<Item> items = s.search(Item.class, (query, root) ->
                        query.where(cb.gt(root.get(Item_.sequence), since)))
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
                Items.nextSequence(items).orElse(since)
        );

    }
}
