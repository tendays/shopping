package org.gamboni.shopping.server;

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.StreamingOutput;
import lombok.extern.slf4j.Slf4j;
import org.gamboni.shopping.server.domain.ProductPicture;
import org.gamboni.shopping.server.domain.Store;
import org.gamboni.shopping.server.ui.Script;
import org.gamboni.shopping.server.ui.ShoppingPage;
import org.gamboni.shopping.server.ui.Style;
import org.gamboni.shopping.server.ui.UiMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * @author tendays
 */
@Path("/")
@Slf4j
public class ShoppingServer {
    @Inject
    Store s;

    private static final File IMAGE_PATH = new File(removeSubPath(new File("."),
            "build", "classes", "java", "main", "."), "images");

    static File removeSubPath(File file, String... toRemove) {
        File pointer = file.getAbsoluteFile();
        for (int i = toRemove.length - 1; i >= 0; i--) {
            if (pointer.getName().equals(toRemove[i])) {
                pointer = pointer.getParentFile();
            } else {
                return file;
            }
        }
        return pointer;
    }


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
            File file = new File(IMAGE_PATH, pp.get().getFile());
            if (!file.exists()) {
                log.error("file {} does not exist", file.getAbsolutePath());
                return; // TODO 404
            }
            try (InputStream in = new FileInputStream(file)) {
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
}
