package com.borcasergiu.vitaly;

import io.github.cdimascio.dotenv.Dotenv;

/*
  Central place for runtime configuration.
  Real environment variables (Railway, Docker) win over a local .env file,
  and a missing .env file is not an error.
 */
public final class Config {

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    private Config() {}

    public static String get(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) value = DOTENV.get(key);
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    public static String require(String key) {
        String value = get(key);
        if (value == null) {
            throw new IllegalStateException(key + " is not set. Add it to your environment or .env file.");
        }
        return value;
    }

    public static int getInt(String key, int fallback) {
        String value = get(key);
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(key + " must be a number, got: " + value);
        }
    }
}
