package br.edu.ifpr.gastobot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class DatabaseConfigTest {
    @Test
    void propriedadeTemPrecedenciaSobreAmbiente() {
        var properties = new Properties();
        properties.setProperty(DatabaseConfig.PROPERTY, "data/propriedade.db");
        assertEquals(Path.of("data/propriedade.db").toAbsolutePath().normalize(),
                DatabaseConfig.caminho(properties, Map.of(DatabaseConfig.ENVIRONMENT, "ambiente.db")));
    }

    @Test
    void propriedadeVaziaUsaAmbiente() {
        var properties = new Properties();
        properties.setProperty(DatabaseConfig.PROPERTY, " ");
        assertEquals(Path.of("ambiente.db").toAbsolutePath().normalize(),
                DatabaseConfig.caminho(properties, Map.of(DatabaseConfig.ENVIRONMENT, "ambiente.db")));
    }

    @Test
    void ausenciaOuAmbienteVazioUsaPadrao() {
        Path expected = Path.of("data/gastobot.db").toAbsolutePath().normalize();
        assertEquals(expected, DatabaseConfig.caminho(new Properties(), Map.of()));
        assertEquals(expected, DatabaseConfig.caminho(new Properties(), Map.of(DatabaseConfig.ENVIRONMENT, " ")));
    }
}
