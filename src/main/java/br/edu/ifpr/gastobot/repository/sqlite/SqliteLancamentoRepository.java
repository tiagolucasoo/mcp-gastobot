package br.edu.ifpr.gastobot.repository.sqlite;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import br.edu.ifpr.gastobot.exception.PersistenciaException;
import br.edu.ifpr.gastobot.model.FormaPagamento;
import br.edu.ifpr.gastobot.model.Lancamento;
import br.edu.ifpr.gastobot.model.TipoLancamento;
import br.edu.ifpr.gastobot.model.ResumoFinanceiro;
import br.edu.ifpr.gastobot.repository.LancamentoRepository;

public final class SqliteLancamentoRepository implements LancamentoRepository {
    private final SqliteConnectionFactory connections;

    public SqliteLancamentoRepository(SqliteConnectionFactory connections) {
        this.connections = Objects.requireNonNull(connections, "Conexões obrigatórias");
        inicializar();
    }

    private void inicializar() {
        String sql = """
                CREATE TABLE IF NOT EXISTS lancamentos (
                    id TEXT PRIMARY KEY NOT NULL,
                    usuario_id TEXT NOT NULL CHECK (length(trim(usuario_id)) > 0),
                    tipo TEXT NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
                    valor_centavos INTEGER NOT NULL
                        CHECK (typeof(valor_centavos) = 'integer' AND valor_centavos > 0),
                    forma_pagamento TEXT NOT NULL CHECK (forma_pagamento IN (
                        'PIX', 'DINHEIRO', 'CARTAO_DEBITO', 'CARTAO_CREDITO', 'TRANSFERENCIA', 'BOLETO')),
                    descricao TEXT NOT NULL,
                    data TEXT NOT NULL
                )
                """;
        try (var connection = connections.abrir(); var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_lancamentos_usuario_data_id ON lancamentos (usuario_id, data, id)");
        } catch (SQLException exception) {
            throw new PersistenciaException("Não foi possível inicializar o banco", exception);
        }
    }

    @Override
    public void salvar(Lancamento lancamento) {
        Objects.requireNonNull(lancamento, "Lançamento obrigatório");
        String sql = """
                INSERT INTO lancamentos
                    (id, usuario_id, tipo, valor_centavos, forma_pagamento, descricao, data)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (var connection = connections.abrir(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, lancamento.id().toString());
            statement.setString(2, lancamento.usuarioId());
            statement.setString(3, lancamento.tipo().name());
            statement.setLong(4, lancamento.valor().movePointRight(2).longValueExact());
            statement.setString(5, lancamento.formaPagamento().name());
            statement.setString(6, lancamento.descricao());
            statement.setString(7, lancamento.data().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenciaException("Não foi possível salvar o lançamento", exception);
        }
    }

    @Override
    public Optional<Lancamento> buscarPorId(UUID id, String usuarioId) {
        String sql = "SELECT * FROM lancamentos WHERE id = ? AND usuario_id = ?";
        try (var connection = connections.abrir(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, Objects.requireNonNull(id).toString());
            statement.setString(2, Objects.requireNonNull(usuarioId));
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapear(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new PersistenciaException("Não foi possível consultar o lançamento", exception);
        }
    }

    private Lancamento mapear(ResultSet result) throws SQLException {
        return new Lancamento(UUID.fromString(result.getString("id")), result.getString("usuario_id"),
                TipoLancamento.valueOf(result.getString("tipo")),
                BigDecimal.valueOf(result.getLong("valor_centavos"), 2),
                FormaPagamento.valueOf(result.getString("forma_pagamento")),
                result.getString("descricao"), LocalDate.parse(result.getString("data")));
    }

    @Override
    public List<Lancamento> listarPorPeriodo(String usuarioId, LocalDate inicio, LocalDate fim, int limite, int offset) {
        String sql = """
                SELECT * FROM lancamentos
                WHERE usuario_id = ? AND data >= ? AND data <= ?
                ORDER BY data ASC, id ASC LIMIT ? OFFSET ?
                """;
        try (var connection = connections.abrir(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, usuarioId);
            statement.setString(2, inicio.toString());
            statement.setString(3, fim.toString());
            statement.setInt(4, limite);
            statement.setInt(5, offset);
            var lancamentos = new ArrayList<Lancamento>();
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    lancamentos.add(mapear(result));
                }
            }
            return List.copyOf(lancamentos);
        } catch (SQLException exception) {
            throw new PersistenciaException("Não foi possível consultar os lançamentos", exception);
        }
    }

    @Override
    public ResumoFinanceiro resumirPorPeriodo(String usuarioId, LocalDate inicio, LocalDate fim) {
        String sql = "SELECT tipo, valor_centavos FROM lancamentos WHERE usuario_id = ? AND data >= ? AND data <= ?";
        // Soma em BigDecimal para evitar tanto arredondamento quanto overflow do SUM(INTEGER) do SQLite.
        BigDecimal entradas = new BigDecimal("0.00");
        BigDecimal saidas = new BigDecimal("0.00");
        long quantidadeEntradas = 0;
        long quantidadeSaidas = 0;
        try (var connection = connections.abrir(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, usuarioId);
            statement.setString(2, inicio.toString());
            statement.setString(3, fim.toString());
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    BigDecimal valor = BigDecimal.valueOf(result.getLong("valor_centavos"), 2);
                    if (TipoLancamento.ENTRADA.name().equals(result.getString("tipo"))) {
                        entradas = entradas.add(valor);
                        quantidadeEntradas++;
                    } else {
                        saidas = saidas.add(valor);
                        quantidadeSaidas++;
                    }
                }
            }
            return new ResumoFinanceiro(entradas, saidas, quantidadeEntradas, quantidadeSaidas);
        } catch (SQLException exception) {
            throw new PersistenciaException("Não foi possível calcular o resumo financeiro", exception);
        }
    }
}
