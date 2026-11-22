package br.edu.ifpr.gastobot.repository.sqlite;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.edu.ifpr.gastobot.exception.PersistenciaException;
import br.edu.ifpr.gastobot.model.FormaPagamento;
import br.edu.ifpr.gastobot.model.Lancamento;
import br.edu.ifpr.gastobot.model.TipoLancamento;

class SqliteLancamentoRepositoryIT {
    @TempDir Path directory;
    private SqliteConnectionFactory connections;
    private SqliteLancamentoRepository repository;

    @BeforeEach
    void configurar() {
        connections = new SqliteConnectionFactory(directory.resolve("nested/gastobot.db"));
        repository = new SqliteLancamentoRepository(connections);
    }

    private Lancamento lancamento(FormaPagamento forma) {
        return new Lancamento(UUID.randomUUID(), "usuario-1", TipoLancamento.SAIDA,
                new BigDecimal("35.90"), forma, "Almoço ' especial 🍲", LocalDate.of(2026, 11, 21));
    }

    @ParameterizedTest
    @EnumSource(FormaPagamento.class)
    void persisteERecuperaAposReabrirOArquivo(FormaPagamento forma) {
        Lancamento original = lancamento(forma);
        repository.salvar(original);
        var reopened = new SqliteLancamentoRepository(connections);
        assertEquals(original, reopened.buscarPorId(original.id(), original.usuarioId()).orElseThrow());
    }

    @Test
    void naoRetornaRegistrosDeOutroUsuarioOuIdInexistente() {
        Lancamento original = lancamento(FormaPagamento.PIX);
        repository.salvar(original);
        assertTrue(repository.buscarPorId(original.id(), "outro").isEmpty());
        assertTrue(repository.buscarPorId(UUID.randomUUID(), original.usuarioId()).isEmpty());
        assertTrue(repository.buscarPorId(original.id(), "' OR 1=1 --").isEmpty());
    }

    @Test
    void guardaCentavosInteirosInclusiveNoLimite() throws Exception {
        var original = new Lancamento(UUID.randomUUID(), "u", TipoLancamento.ENTRADA,
                BigDecimal.valueOf(Long.MAX_VALUE, 2), FormaPagamento.TRANSFERENCIA,
                "", LocalDate.of(2026, 11, 21));
        repository.salvar(original);
        try (var connection = connections.abrir(); var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT valor_centavos, typeof(valor_centavos) AS tipo FROM lancamentos")) {
            assertTrue(result.next());
            assertEquals(Long.MAX_VALUE, result.getLong("valor_centavos"));
            assertEquals("integer", result.getString("tipo"));
        }
        assertEquals(original, repository.buscarPorId(original.id(), "u").orElseThrow());
    }

    @Test
    void idDuplicadoNaoSobrescreveRegistro() {
        Lancamento original = lancamento(FormaPagamento.PIX);
        repository.salvar(original);
        assertThrows(PersistenciaException.class, () -> repository.salvar(original));
        assertEquals(original, repository.buscarPorId(original.id(), original.usuarioId()).orElseThrow());
    }

    @Test
    void traduzFalhasDeLeituraEGravacao() throws Exception {
        try (var connection = connections.abrir(); var statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE lancamentos");
        }
        assertThrows(PersistenciaException.class, () -> repository.salvar(lancamento(FormaPagamento.PIX)));
        assertThrows(PersistenciaException.class, () -> repository.buscarPorId(UUID.randomUUID(), "u"));
        assertThrows(PersistenciaException.class, () -> repository.resumirPorPeriodo("u", LocalDate.now(), LocalDate.now()));
    }

    @Test
    void falhaAoInicializarBancoEmDiretorio() {
        assertThrows(PersistenciaException.class,
                () -> new SqliteLancamentoRepository(new SqliteConnectionFactory(directory)));
    }

    @Test
    void falhaQuandoDiretorioPaiEhArquivo() throws Exception {
        Path file = Files.createFile(directory.resolve("arquivo"));
        assertThrows(PersistenciaException.class,
                () -> new SqliteConnectionFactory(file.resolve("gastobot.db")));
    }

    @Test
    void periodoIncluiLimitesOrdenaPaginaEIsolaUsuario() {
        var inicio = LocalDate.of(2026, 11, 1);
        var fim = LocalDate.of(2026, 11, 22);
        var primeiro = new Lancamento(new UUID(0, 1), "u", TipoLancamento.ENTRADA, BigDecimal.ONE,
                FormaPagamento.PIX, "primeiro", inicio);
        var segundo = new Lancamento(new UUID(0, 2), "u", TipoLancamento.SAIDA, BigDecimal.ONE,
                FormaPagamento.PIX, "segundo", fim);
        var terceiro = new Lancamento(new UUID(0, 3), "u", TipoLancamento.SAIDA, BigDecimal.ONE,
                FormaPagamento.PIX, "terceiro", fim);
        for (var item : java.util.List.of(terceiro, segundo, primeiro,
                new Lancamento(UUID.randomUUID(), "outro", TipoLancamento.SAIDA, BigDecimal.ONE, FormaPagamento.PIX, "", inicio),
                new Lancamento(UUID.randomUUID(), "u", TipoLancamento.SAIDA, BigDecimal.ONE, FormaPagamento.PIX, "", inicio.minusDays(1)),
                new Lancamento(UUID.randomUUID(), "u", TipoLancamento.SAIDA, BigDecimal.ONE, FormaPagamento.PIX, "", fim.plusDays(1)))) {
            repository.salvar(item);
        }
        assertEquals(java.util.List.of(primeiro, segundo, terceiro), repository.listarPorPeriodo("u", inicio, fim, 10, 0));
        assertEquals(java.util.List.of(segundo, terceiro), repository.listarPorPeriodo("u", fim, fim, 10, 0));
        assertEquals(java.util.List.of(terceiro), repository.listarPorPeriodo("u", inicio, fim, 2, 2));
        assertTrue(repository.listarPorPeriodo("' OR 1=1 --", inicio, fim, 10, 0).isEmpty());
        assertTrue(repository.listarPorPeriodo("u", inicio, fim, 10, 99).isEmpty());
    }

    @Test
    void resumoSomaCentavosSemOverflowEContaSomenteUsuarioEPeriodo() {
        var data = LocalDate.of(2026, 11, 22);
        for (int i = 0; i < 2; i++) {
            repository.salvar(new Lancamento(UUID.randomUUID(), "u", TipoLancamento.ENTRADA,
                    BigDecimal.valueOf(Long.MAX_VALUE, 2), FormaPagamento.PIX, "", data));
        }
        repository.salvar(new Lancamento(UUID.randomUUID(), "u", TipoLancamento.SAIDA,
                new BigDecimal("0.01"), FormaPagamento.PIX, "", data));
        repository.salvar(new Lancamento(UUID.randomUUID(), "outro", TipoLancamento.ENTRADA,
                BigDecimal.ONE, FormaPagamento.PIX, "", data));
        repository.salvar(new Lancamento(UUID.randomUUID(), "u", TipoLancamento.ENTRADA,
                BigDecimal.ONE, FormaPagamento.PIX, "", data.minusDays(1)));
        var resumo = repository.resumirPorPeriodo("u", data, data);
        assertEquals(new BigDecimal("184467440737095516.14"), resumo.totalEntradas());
        assertEquals(new BigDecimal("0.01"), resumo.totalSaidas());
        assertEquals(2, resumo.quantidadeEntradas());
        assertEquals(1, resumo.quantidadeSaidas());
        assertEquals(3, resumo.quantidade());
        var vazio = repository.resumirPorPeriodo("u", data.plusDays(1), data.plusDays(1));
        assertEquals(new BigDecimal("0.00"), vazio.totalEntradas());
        assertEquals(new BigDecimal("0.00"), vazio.totalSaidas());
        assertEquals(0, vazio.quantidade());
        assertEquals(0, repository.resumirPorPeriodo("' OR 1=1 --", data, data).quantidade());
    }
}
