package br.edu.ifpr.gastobot.config;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public final class DatabaseConfig {
    public static final String PROPERTY = "gastobot.db.path";
    public static final String ENVIRONMENT = "GASTOBOT_DB_PATH";

    private DatabaseConfig() {
    }

    public static Path caminho() {
        String configurado = System.getProperty(PROPERTY);
        return caminho(System.getProperties(), configurado != null && !configurado.isBlank()
                ? System.getenv() : LocalEnvironment.carregar());
    }

    public static Path caminho(Properties properties, Map<String, String> environment) {
        String value = properties.getProperty(PROPERTY);
        if (value == null || value.isBlank()) {
            value = environment.get(ENVIRONMENT);
        }
        if (value == null || value.isBlank()) {
            value = "data/gastobot.db";
        }
        return Path.of(value).toAbsolutePath().normalize();
    }
}
