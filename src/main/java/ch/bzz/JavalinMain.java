package ch.bzz;

import ch.bzz.db.BookPersistor;

import io.javalin.Javalin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavalinMain {

    private static final Logger log = LoggerFactory.getLogger(JavalinMain.class);
    private static final int PORT = 7070;

    private static final BookPersistor bookPersistor = new BookPersistor();

    public static void main(String[] args) {
        Javalin app = Javalin.create();

        app.get("/books", ctx -> {
            int limit = 0;
            String limitParam = ctx.queryParam("limit");
            if (limitParam != null && !limitParam.isBlank()) {
                limit = Integer.parseInt(limitParam);
            }
            ctx.json(bookPersistor.getAll(limit));
        });

        app.start(PORT);
        log.info("Javalin server started on port {}", PORT);
    }
}
