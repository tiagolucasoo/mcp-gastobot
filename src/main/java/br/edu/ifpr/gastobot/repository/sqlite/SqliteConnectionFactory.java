package br.edu.ifpr.gastobot.repository.sqlite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

import br.edu.ifpr.gastobot.exception.PersistenciaException;

/** Uma conexão por operação, sempre fechada pelo chamador com try-with-resources. */
public final class SqliteConnectionFactory {
    private final String jdbcUrl;

    public SqliteConnectionFactory(Path caminho) {
        Path absoluto = Objects.requireNonNull(caminho, "Caminho obrigatório")
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(absoluto.getParent());
        } catch (IOException exception) {
            throw new PersistenciaException("Não foi possível preparar o diretório do banco", exception);
        }
        jdbcUrl = "jdbc:sqlite:" + absoluto;
    }

    public Connection abrir() throws SQLException {
        var properties = new java.util.Properties();
        properties.setProperty("busy_timeout", "5000");
        return DriverManager.getConnection(jdbcUrl, properties);
    }
}
