package ch.bzz;

import ch.bzz.db.BookPersistor;
import ch.bzz.model.Book;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JavalinMainTest {

    private static final int PORT = 7070;
    private static final int TIMEOUT = 5_000;
    private static final ObjectMapper mapper = new ObjectMapper();
    private static BookPersistor bookPersistor;

    @BeforeAll
    static void startApp() throws InterruptedException {
        bookPersistor = new BookPersistor();
        new Thread(() -> JavalinMain.main(new String[]{})).start();
        Thread.sleep(2_000);
    }

    @Test
    void testGetBooksWithLimit() throws Exception {
        // Arrange
        int limit = 3;
        List<Book> expectedBooks = bookPersistor.getAll(limit);

        // Act
        URI uri = new URI("http://localhost:" + PORT + "/books?limit=" + limit);
        HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(TIMEOUT);
        con.setReadTimeout(TIMEOUT);

        int status = con.getResponseCode();
        assertEquals(200, status);

        StringBuilder responseBuilder = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                responseBuilder.append(line);
            }
        }
        String response = responseBuilder.toString();

        List<Book> actualBooks = mapper.readValue(response, new TypeReference<List<Book>>() {});

        // Assert
        assertEquals(limit, actualBooks.size(), "The number of returned books is incorrect");
        String expectedJson = mapper.writeValueAsString(expectedBooks);
        String actualJson = mapper.writeValueAsString(actualBooks);
        assertEquals(expectedJson, actualJson, "The JSON-Objects are not equals");
    }
}
