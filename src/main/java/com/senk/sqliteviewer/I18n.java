package com.senk.sqliteviewer;

import javafx.beans.property.SimpleObjectProperty;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;

public final class I18n {

    private static final SimpleObjectProperty<Locale> locale =
            new SimpleObjectProperty<>();
    private static Properties en;
    private static Properties zh;
    private static Properties current;

    private static final Path CONFIG_DIR = Paths.get(
            System.getProperty("user.home"), ".sqliteviewer");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");

    static {
        en = loadProperties("/com/senk/sqliteviewer/messages.properties");
        zh = loadProperties("/com/senk/sqliteviewer/messages_zh_CN.properties");
        locale.addListener((obs, oldVal, newVal) -> {
            if (newVal != null
                    && (Locale.SIMPLIFIED_CHINESE.equals(newVal)
                    || "zh".equals(newVal.getLanguage()))) {
                current = zh;
            } else {
                current = en;
            }
        });
        setLocale(loadSavedLocale());
    }

    private I18n() {
    }

    private static Properties loadProperties(String path) {
        Properties props = new Properties();
        try (InputStream is = I18n.class.getResourceAsStream(path)) {
            if (is != null) {
                props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
        }
        return props;
    }

    private static Locale loadSavedLocale() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                Properties props = new Properties();
                try (Reader r = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                    props.load(r);
                }
                String s = props.getProperty("locale", "");
                if ("zh_CN".equals(s) || "zh".equals(s)) {
                    return Locale.SIMPLIFIED_CHINESE;
                }
                if ("en".equals(s)) {
                    return Locale.ENGLISH;
                }
            }
        } catch (IOException ignored) {
        }
        return Locale.ENGLISH;
    }

    public static void setLocale(Locale newLocale) {
        locale.set(newLocale);
        saveLocale(newLocale);
    }

    private static void saveLocale(Locale loc) {
        try {
            Files.createDirectories(CONFIG_DIR);
            Properties props = new Properties();
            if (Locale.SIMPLIFIED_CHINESE.equals(loc)
                    || "zh".equals(loc.getLanguage())) {
                props.setProperty("locale", "zh_CN");
            } else {
                props.setProperty("locale", "en");
            }
            try (Writer w = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                props.store(w, "SQLite Viewer config");
            }
        } catch (IOException ignored) {
        }
    }

    public static Locale getLocale() {
        return locale.get();
    }

    public static SimpleObjectProperty<Locale> localeProperty() {
        return locale;
    }

    public static String get(String key) {
        if (current == null) return key;
        return current.getProperty(key, key);
    }

    public static String get(String key, Object... args) {
        String pattern = get(key);
        return MessageFormat.format(pattern, args);
    }
}
