package ch.bzz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.bzz.db.BookPersistor;
import ch.bzz.model.Book;

import static org.junit.jupiter.api.Assertions.*;

public class JavalinMainTest {
    private static final int PORT = 7070;
    private static final int TIMEOUT = 5_000;
    private static final ObjectMapper mapper = new ObjectMapper();
    private static BookPersistor bookPersistor;

    @BeforeAll
    static void startApp() {
        bookPersistor = new BookPersistor();
        new Thread(() -> JavalinMain.main(new String[]{})).start();
        waitForServer();
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

        List<Book> actualBooks = mapper.readValue(response, new TypeReference<>() {});

        // Assert
        assertEquals(limit, actualBooks.size(), "The number of returned books is incorrect");
        String expectedJson = mapper.writeValueAsString(expectedBooks);
        String actualJson = mapper.writeValueAsString(actualBooks);
        assertEquals(expectedJson, actualJson, "The JSON-Objects are not equals");
    }

    @Test
    void testLoginReturnsJwt() throws Exception {
        // Arrange
        URI uri = new URI("http://localhost:" + PORT + "/auth/login");
        HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
        con.setRequestMethod("POST");
        con.setConnectTimeout(TIMEOUT);
        con.setReadTimeout(TIMEOUT);
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json");

        String jsonBody = mapper.writeValueAsString(Map.of(
                "email", "max.mustermann@example.com",
                "password", "geheim123"));
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Act
        int status = con.getResponseCode();
        assertEquals(200, status, "Login should return 200 OK");

        StringBuilder responseBuilder = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                responseBuilder.append(line);
            }
        }
        String response = responseBuilder.toString();
        JsonNode jsonNode = mapper.readTree(response);

        // Assert
        assertTrue(jsonNode.has("token"), "Response should contain a 'token' field");
        String token = jsonNode.get("token").asText();
        assertNotNull(token);
        assertFalse(token.isBlank(), "Token must not be empty");
        assertEquals(3, token.split("\\.").length, "Token should look like a JWT");
    }

    @AfterAll
    static void cleanUp() {
        bookPersistor.close();
    }

    private static void waitForServer() {
        int attempts = 0;
        boolean isUp = false;
        while (attempts < 25 && !isUp) {
            try (Socket _ = new Socket("localhost", PORT)) {
                isUp = true;
            } catch (IOException e) {
                attempts++;
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    fail(ex);
                }
            }
        }
        if (!isUp) {
            throw new IllegalStateException("Server did not start in time");
        }
    }
}
