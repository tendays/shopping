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
import org.gamboni.shopping.server.ui.ShoppingPage;
import org.gamboni.shopping.server.ui.Style;

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

    @Inject
    ShoppingPage shoppingPage;

    @Inject
    Style style;

    // The removeSubPath() is needed in dev-mode. In native mode it should be run just above the images folder
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

    @GET
    @Path("ping")
    public String ping() {
        return "ok";
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
