package ch.bzz.db;

import ch.bzz.io.BookRepository;
import ch.bzz.model.Book;

import java.util.List;

public class BookPersistor {

    private final BookRepository bookRepository = new BookRepository();

    public List<Book> getAll(int limit) {
        return bookRepository.getAll(limit);
    }
}
