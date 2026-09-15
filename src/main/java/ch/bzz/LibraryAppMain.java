package ch.bzz;

import ch.bzz.io.BookImporter;
import ch.bzz.io.BookRepository;
import ch.bzz.io.UserRepository;
import ch.bzz.model.Book;
import ch.bzz.model.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryAppMain {

    private static final Logger log = LoggerFactory.getLogger(LibraryAppMain.class);

    @FunctionalInterface
    private interface CommandAction {
        boolean execute();
    }

    private record Command(String description, CommandAction action) {
    }

    private static final Map<String, Command> COMMANDS = new LinkedHashMap<>();
    private static final BookRepository bookRepository = new BookRepository();
    private static final UserRepository userRepository = new UserRepository();
    private static final BookImporter bookImporter = new BookImporter();

    static {
        COMMANDS.put("help", new Command("zeigt alle verfügbaren Befehle an", LibraryAppMain::printHelp));
        COMMANDS.put("listBooks", new Command("listet Bücher auf (optional: listBooks <LIMIT>)", () -> listBooks(null)));
        COMMANDS.put("importBooks", new Command("importiert Bücher aus einer TSV-Datei (importBooks <FILE_PATH>)", () -> true));
        COMMANDS.put("quit", new Command("beendet das Programm", () -> false));
    }

    public static void main(String[] args) {
        log.info("LibraryApp started");
        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();

            if (input.startsWith("importBooks ")) {
                importBooks(input.substring("importBooks ".length()).trim());
                continue;
            }

            if (input.startsWith("listBooks")) {
                handleListBooks(input);
                continue;
            }

            if (input.startsWith("createUser ")) {
                createUser(input.substring("createUser ".length()).trim());
                continue;
            }

            Command command = COMMANDS.get(input);

            if (command == null) {
                System.out.println("Die Eingabe \"" + input + "\" wurde nicht als Befehl erkannt.");
                continue;
            }

            if (!command.action().execute()) {
                break;
            }
        }

        log.info("LibraryApp stopped");
    }

    private static void handleListBooks(String input) {
        Integer limit = null;

        if (input.length() > "listBooks".length()) {
            String limitText = input.substring("listBooks".length()).trim();
            if (!limitText.isEmpty()) {
                try {
                    limit = Integer.parseInt(limitText);
                } catch (NumberFormatException e) {
                    log.warn("Ungültiges Limit für listBooks: '{}'", limitText, e);
                    System.out.println("Das Limit \"" + limitText + "\" konnte nicht gelesen werden.");
                }
            }
        }

        listBooks(limit);
    }

    private static boolean printHelp() {
        System.out.println("Verfügbare Befehle:");
        COMMANDS.forEach((name, command) ->
                System.out.println(name + " - " + command.description()));
        return true;
    }

    private static boolean listBooks(Integer limit) {
        try {
            int effectiveLimit = limit == null ? 0 : limit;
            for (Book book : bookRepository.getAll(effectiveLimit)) {
                System.out.println(book);
            }
        } catch (RuntimeException e) {
            log.error("Fehler beim Laden der Bücher", e);
            System.out.println("Die Bücher konnten nicht geladen werden.");
        }

        return true;
    }

    private static void createUser(String arguments) {
        String[] tokens = arguments.split("\\s+");
        if (tokens.length != 5) {
            log.warn("Ungültige Argumente für createUser: '{}'", arguments);
            System.out.println("Die Eingabe für createUser konnte nicht gelesen werden.");
            return;
        }

        String firstname = tokens[0];
        String lastname = tokens[1];
        LocalDate dateOfBirth;
        try {
            dateOfBirth = LocalDate.parse(tokens[2]);
        } catch (RuntimeException e) {
            log.warn("Ungültiges Geburtsdatum für createUser: '{}'", tokens[2], e);
            System.out.println("Das Geburtsdatum \"" + tokens[2] + "\" konnte nicht gelesen werden.");
            return;
        }
        String email = tokens[3];
        String password = tokens[4];

        try {
            byte[] salt = PasswordHandler.generateSalt();
            byte[] hash = PasswordHandler.hashPassword(password, salt);

            User user = new User();
            user.setFirstname(firstname);
            user.setLastname(lastname);
            user.setDateOfBirth(dateOfBirth);
            user.setEmail(email);
            user.setPasswordSalt(Base64.getEncoder().encodeToString(salt));
            user.setPasswordHash(Base64.getEncoder().encodeToString(hash));

            userRepository.save(user);
        } catch (NoSuchAlgorithmException e) {
            log.error("Fehler beim Hashen des Passworts", e);
            System.out.println("Der Benutzer konnte nicht angelegt werden.");
        } catch (RuntimeException e) {
            log.error("Fehler beim Speichern des Benutzers", e);
            System.out.println("Der Benutzer konnte nicht angelegt werden.");
        }
    }

    private static void importBooks(String filePath) {
        Path path = Path.of(filePath);

        if (!Files.exists(path)) {
            log.warn("Importdatei nicht gefunden: {}", filePath);
            System.out.println("Die Importdatei konnte nicht gefunden werden: " + filePath);
            return;
        }

        try {
            List<Book> books = bookImporter.readFromTsv(path);
            bookRepository.saveAll(books);
            log.info("Import abgeschlossen: {} Bücher aus {}", books.size(), filePath);
        } catch (IOException e) {
            log.error("Fehler beim Lesen der Importdatei {}", filePath, e);
            System.out.println("Die Importdatei konnte nicht gelesen werden: " + filePath);
        }
    }
}
