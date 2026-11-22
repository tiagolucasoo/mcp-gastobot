package br.edu.ifpr.gastobot.server.mcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.gastobot.controller.LancamentoController;
import br.edu.ifpr.gastobot.exception.PersistenciaException;
import br.edu.ifpr.gastobot.repository.LancamentoRepository;
import br.edu.ifpr.gastobot.service.LancamentoService;

class LancamentoToolsTest {
    private final LancamentoRepository repository = mock(LancamentoRepository.class);
    private final LancamentoTools tools = new LancamentoTools(
            new LancamentoController(new LancamentoService(repository)));

    private Map<String, Object> argumentos() {
        return new HashMap<>(Map.of("usuarioId", "u", "tipo", "SAIDA", "valor", "35.90",
                "formaPagamento", "PIX", "data", "2026-11-21"));
    }

    @Test
    void registraSemDescricao() {
        assertFalse(tools.registrar(argumentos()).isError());
        verify(repository).salvar(argThat(lancamento -> lancamento.descricao().isEmpty()));
    }

    @Test
    void rejeitaArgumentosNulosDesconhecidosOuTiposIncorretos() {
        assertTrue(tools.registrar(null).isError());
        var args = argumentos();
        args.put("extra", "campo");
        assertTrue(tools.registrar(args).isError());
        args = argumentos();
        args.put("valor", 35.90);
        assertTrue(tools.registrar(args).isError());
        args = argumentos();
        args.put("descricao", null);
        assertTrue(tools.registrar(args).isError());
        for (String key : argumentos().keySet()) {
            args = argumentos();
            args.remove(key);
            assertTrue(tools.registrar(args).isError(), key);
        }
        verifyNoInteractions(repository);
    }

    @Test
    void rejeitaDatasValoresEEnumsInvalidos() {
        for (var invalid : Map.of("data", "2026-02-30", "valor", "0.001", "tipo", "INVALIDO",
                "formaPagamento", "INVALIDO", "usuarioId", " ").entrySet()) {
            var args = argumentos();
            args.put(invalid.getKey(), invalid.getValue());
            assertTrue(tools.registrar(args).isError(), invalid.getKey());
        }
        assertTrue(tools.buscar(Map.of("usuarioId", "u", "id", "invalido")).isError());
        verifyNoInteractions(repository);
    }

    @Test
    void traduzErroDePersistenciaSemExporDetalhes() {
        doThrow(new PersistenciaException("segredo.db", new RuntimeException("SQL interno")))
                .when(repository).salvar(any());
        var result = tools.registrar(argumentos());
        assertTrue(result.isError());
        String texto = ((io.modelcontextprotocol.spec.McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(texto.contains("ERRO_PERSISTENCIA"));
        assertFalse(texto.contains("segredo.db"));
    }

    @Test
    void listaVaziaUsaPaginaPadraoERejeitaParametrosInvalidos() {
        var valid = new HashMap<String, Object>(Map.of("usuarioId", "u", "dataInicio", "2026-11-01", "dataFim", "2026-11-22"));
        assertFalse(tools.listar(valid).isError());
        verify(repository).listarPorPeriodo("u", java.time.LocalDate.of(2026, 11, 1),
                java.time.LocalDate.of(2026, 11, 22), 11, 0);
        clearInvocations(repository);
        for (Object page : new Object[]{0, -1, 1.5, "1", Long.MAX_VALUE}) {
            var invalid = new HashMap<>(valid);
            invalid.put("pagina", page);
            assertTrue(tools.listar(invalid).isError());
        }
        var reversed = new HashMap<>(valid);
        reversed.put("dataFim", "2026-10-31");
        assertTrue(tools.listar(reversed).isError());
        assertTrue(tools.listar(null).isError());
        assertTrue(tools.listar(Map.of()).isError());
        verifyNoInteractions(repository);
    }
}
