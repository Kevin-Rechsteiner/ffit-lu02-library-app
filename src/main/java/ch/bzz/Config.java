package ch.bzz;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);
    private static final Properties properties = new Properties();

    static {
        try (FileInputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
            log.info("Configuration loaded from config.properties");
        } catch (IOException e) {
            log.error("config.properties konnte nicht geladen werden", e);
            throw new RuntimeException("config.properties konnte nicht geladen werden", e);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static Properties getProperties() {
        return properties;
    }
}
