package br.edu.ifpr.gastobot.client.telegram;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.gastobot.client.mcp.McpGateway;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

class TelegramCommandServiceTest {
    private final McpGateway gateway = mock(McpGateway.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-22T01:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final TelegramCommandService service = new TelegramCommandService(gateway, clock);

    private Map<String, String> opcoes(String valor) {
        return new HashMap<>(Map.of("tipo", "SAIDA", "valor", valor, "forma", "PIX"));
    }

    private CallToolResult resultado(Map<String, Object> payload, boolean error) {
        return CallToolResult.builder().isError(error).content(List.of(
                TextContent.builder(new JsonMapper().writeValueAsString(payload)).build())).build();
    }

    private CallToolResult sucesso(String descricao) {
        return resultado(Map.of("id", "f5cf9298-848b-46a7-9179-9e97361b5804", "usuarioId", "123", "tipo", "SAIDA",
                "valor", "35.90", "formaPagamento", "PIX", "data", "2026-11-21", "descricao", descricao), false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"35,90", "35.90"})
    void usaUsuarioDoEventoNormalizaValorECalculaDataNoFuso(String valor) {
        when(gateway.executar(anyString(), anyMap())).thenReturn(sucesso(""));
        var options = opcoes(valor);
        options.put("usuarioId", "outro-usuario");
        String resposta = service.executar("registrar", "123", options);
        assertTrue(resposta.contains("Lançamento registrado."));
        assertTrue(resposta.contains("R$ 35,90"));
        assertFalse(resposta.contains("ID:"));
        assertFalse(resposta.contains("f5cf9298-848b-46a7-9179-9e97361b5804"));
        verify(gateway).executar("registrar_lancamento", Map.of("usuarioId", "123", "tipo", "SAIDA",
                "valor", "35.90", "formaPagamento", "PIX", "data", "2026-11-21"));
    }

    @Test
    void encaminhaDataEDescricaoExplicitas() {
        when(gateway.executar(anyString(), anyMap())).thenReturn(sucesso("Almoço"));
        var options = opcoes("35,90");
        options.put("data", "2026-11-01");
        options.put("descricao", "Almoço");
        service.executar("registrar", "123", options);
        verify(gateway).executar(eq("registrar_lancamento"), argThat(args ->
                args.get("data").equals("2026-11-01") && args.get("descricao").equals("Almoço")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.234,56", "abc", "", "35,900", "-35", "1e3"})
    void rejeitaFormatoAmbiguoSemChamarServidor(String valor) {
        assertTrue(service.executar("registrar", "123", opcoes(valor)).contains("Dados inválidos"));
        verifyNoInteractions(gateway);
    }

    @Test
    void rejeitaDataInvalidaOuCampoAusente() {
        var options = opcoes("35");
        options.put("data", "2026-02-30");
        assertTrue(service.executar("registrar", "123", options).contains("Dados inválidos"));
        assertTrue(service.executar("registrar", "123", Map.of()).contains("Dados inválidos"));
        assertTrue(service.executar("consultar", "123", Map.of()).contains("Dados inválidos"));
        assertTrue(service.executar("consultar", " ", Map.of("id", "x")).contains("Dados inválidos"));
        verifyNoInteractions(gateway);
    }

    @Test
    void consultaSempreUsaUsuarioDoEvento() {
        when(gateway.executar(anyString(), anyMap())).thenReturn(sucesso(""));
        assertTrue(service.executar("consultar", "123", Map.of("id", "uuid", "usuarioId", "outro"))
                .contains("Lançamento encontrado."));
        verify(gateway).executar("buscar_lancamento", Map.of("id", "uuid", "usuarioId", "123"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DADOS_INVALIDOS", "NAO_ENCONTRADO", "ERRO_PERSISTENCIA", "DESCONHECIDO"})
    void traduzErrosSemExporMensagemInterna(String codigo) {
        when(gateway.executar(anyString(), anyMap())).thenReturn(
                resultado(Map.of("codigo", codigo, "mensagem", "SQL interno secreto"), true));
        String response = service.executar("consultar", "123", Map.of("id", "uuid"));
        assertFalse(response.contains("SQL interno"));
        assertFalse(response.contains("encontrado."));
    }

    @Test
    void falhaDeTransporteNaoRepeteCadastro() {
        when(gateway.executar(anyString(), anyMap())).thenThrow(new IllegalStateException("timeout"));
        String response = service.executar("registrar", "123", opcoes("35"));
        assertTrue(response.contains("Não foi possível confirmar"));
        verify(gateway, times(1)).executar(eq("registrar_lancamento"), anyMap());
    }

    @Test
    void limitaRespostaAoTamanhoDoTelegram() {
        when(gateway.executar(anyString(), anyMap())).thenReturn(sucesso("a".repeat(5000)));
        assertTrue(service.executar("consultar", "123", Map.of("id", "uuid")).length() <= 1900);
    }

    @Test
    void comandoDesconhecidoNaoChamaMcp() {
        assertTrue(service.executar("outro", "123", Map.of()).contains("Comando desconhecido"));
        verifyNoInteractions(gateway);
    }

    @Test
    void consultaPeriodoUsaUsuarioAutenticadoEIndicaPaginaSeguinte() {
        var item = Map.<String, Object>of("id", "f5cf9298-848b-46a7-9179-9e97361b5804", "tipo", "SAIDA",
                "valor", "35.90", "formaPagamento", "PIX", "data", "2026-11-22", "descricao", "Almoço");
        when(gateway.executar(anyString(), anyMap())).thenReturn(resultado(Map.of("lancamentos", List.of(item),
                "pagina", 1, "temProximaPagina", true), false));
        String resposta = service.executar("consultar", "123", Map.of("dataInicio", "2026-11-01",
                "dataFim", "2026-11-22", "usuarioId", "outro"));
        assertTrue(resposta.contains("R$ 35,90"));
        assertTrue(resposta.contains("/consultar 2026-11-01 2026-11-22 pagina=2"));
        verify(gateway).executar("listar_lancamentos", Map.of("usuarioId", "123", "dataInicio", "2026-11-01",
                "dataFim", "2026-11-22", "pagina", 1));
    }

    @Test
    void periodoVazioMostraMensagemClara() {
        when(gateway.executar(anyString(), anyMap())).thenReturn(resultado(Map.of("lancamentos", List.of(),
                "pagina", 1, "temProximaPagina", false), false));
        assertTrue(service.executar("consultar", "123", Map.of("dataInicio", "2026-11-22", "dataFim", "2026-11-22"))
                .startsWith("Nenhum lançamento"));
    }

    @Test
    void paginaCheiaComDescricoesLongasCabeNaMensagemSemPerderRegistros() {
        var item = Map.<String, Object>of("id", "f5cf9298-848b-46a7-9179-9e97361b5804", "tipo", "ENTRADA",
                "valor", "92233720368547758.07", "formaPagamento", "TRANSFERENCIA", "data", "2026-11-22",
                "descricao", "🍲".repeat(500));
        when(gateway.executar(anyString(), anyMap())).thenReturn(resultado(Map.of("lancamentos", java.util.Collections.nCopies(10, item),
                "pagina", 1, "temProximaPagina", true), false));
        String resposta = service.executar("consultar", "123", Map.of("dataInicio", "2026-11-22", "dataFim", "2026-11-22"));
        assertTrue(resposta.length() < 4096);
        assertEquals(10, resposta.lines().filter(line -> line.contains(" | R$ ")).count());
        assertFalse(resposta.contains("ID:"));
        assertFalse(resposta.contains("f5cf9298-848b-46a7-9179-9e97361b5804"));
        assertTrue(resposta.contains("pagina=2"));
    }

    @Test
    void registroEConsultaIndividualNaoMostramId() {
        when(gateway.executar(anyString(), anyMap())).thenReturn(sucesso("Almoço"));
        for (String resposta : List.of(service.executar("registrar", "123", opcoes("35,90")),
                service.executar("consultar", "123", Map.of("id", "uuid")))) {
            assertFalse(resposta.contains("ID:"));
            assertFalse(resposta.contains("f5cf9298-848b-46a7-9179-9e97361b5804"));
        }
    }

    @Test
    void relatorioUsaDataNoFusoESomenteTotaisEQuantidades() {
        var periodos = java.util.stream.Stream.of("DIA", "SEMANA", "MES").map(periodo -> Map.<String, Object>of(
                "periodo", periodo, "dataInicio", "2026-11-21", "dataFim", "2026-11-21",
                "totalEntradas", "100.10", "totalSaidas", "20.05", "quantidadeEntradas", 2,
                "quantidadeSaidas", 1, "quantidade", 3)).toList();
        when(gateway.executar(anyString(), anyMap())).thenReturn(resultado(Map.of("periodos", periodos), false));
        String resposta = service.executar("relatorio", "123", Map.of());
        assertTrue(resposta.contains("Dia (2026-11-21)"));
        assertTrue(resposta.contains("Semana"));
        assertTrue(resposta.contains("Mês"));
        assertTrue(resposta.contains("Entradas: R$ 100,10 | Quantidade: 2"));
        assertTrue(resposta.contains("Saídas: R$ 20,05 | Quantidade: 1"));
        assertTrue(resposta.contains("Quantidade total: 3"));
        assertFalse(resposta.contains("ID:"));
        assertFalse(resposta.contains("Descrição:"));
        verify(gateway).executar("relatorio_financeiro", Map.of("usuarioId", "123", "dataReferencia", "2026-11-21"));
    }
}
