package ch.bzz.io;

import ch.bzz.model.Book;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BookImporter {

    private static final Logger log = LoggerFactory.getLogger(BookImporter.class);

    public List<Book> readFromTsv(Path filePath) throws IOException {
        List<Book> books = new ArrayList<>();

        log.debug("Reading books from TSV file {}", filePath);

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return books;
            }

            String[] headers = headerLine.split("\t", -1);
            int idIndex = indexOf(headers, "id");
            int isbnIndex = indexOf(headers, "isbn");
            int titleIndex = indexOf(headers, "title");
            int authorIndex = firstIndexOf(headers, "author", "authors");
            int yearIndex = firstIndexOf(headers, "year", "publication_year");

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                String[] columns = line.split("\t", -1);
                books.add(new Book(
                        Integer.parseInt(columns[idIndex].trim()),
                        columns[isbnIndex].trim(),
                        columns[titleIndex].trim(),
                        columns[authorIndex].trim(),
                        Integer.parseInt(columns[yearIndex].trim())
                ));
            }
        }

        log.info("Read {} books from {}", books.size(), filePath);
        return books;
    }

    private int indexOf(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (name.equalsIgnoreCase(headers[i].trim())) {
                return i;
            }
        }
        throw new IllegalArgumentException("Spalte nicht gefunden: " + name);
    }

    private int firstIndexOf(String[] headers, String... names) {
        for (String name : names) {
            for (int i = 0; i < headers.length; i++) {
                if (name.equalsIgnoreCase(headers[i].trim())) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException("Keine der Spalten gefunden: " + String.join(", ", names));
    }
}
