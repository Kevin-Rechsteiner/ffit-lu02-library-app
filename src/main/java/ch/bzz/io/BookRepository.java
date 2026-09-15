package ch.bzz.io;

import ch.bzz.Config;
import ch.bzz.model.Book;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BookRepository {

    private static final Logger log = LoggerFactory.getLogger(BookRepository.class);

    private final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("localPU", Config.getProperties());

    public List<Book> getAll(int limit) {
        try (EntityManager em = emf.createEntityManager()) {
            var query = em.createQuery("SELECT b FROM Book b ORDER BY b.id", Book.class);
            if (limit > 0) {
                query.setMaxResults(limit);
            }
            List<Book> books = query.getResultList();
            log.info("Loaded {} books from database", books.size());
            return books;
        }
    }

    public void saveAll(List<Book> books) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                books.forEach(em::merge);
                em.getTransaction().commit();
                log.info("Saved {} books to database", books.size());
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                log.error("Error during saving of books to the database:", e);
            }
        }
    }
}
