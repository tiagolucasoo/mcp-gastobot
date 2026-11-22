package br.edu.ifpr.gastobot.client.telegram;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

class TelegramUpdateHandlerTest {
    private final TelegramCommandService service = mock(TelegramCommandService.class);
    private final TelegramUpdateHandler handler = new TelegramUpdateHandler(new TelegramCommandParser(), service);

    @Test
    void usaIdDoRemetenteComNamespaceDoTelegram() {
        when(service.executar(anyString(), anyString(), anyMap())).thenReturn("registrado");
        assertEquals("registrado", handler.responder(new TelegramMessage(456, 123, true, false,
                "/saida 35,90 pix Almoço")).orElseThrow());
        verify(service).executar("registrar", "telegram:123", Map.of("tipo", "SAIDA", "valor", "35,90",
                "forma", "PIX", "descricao", "Almoço"));
    }

    @Test
    void ignoraGruposBotsEMensagensSemUsuario() {
        assertTrue(handler.responder(null).isEmpty());
        assertTrue(handler.responder(new TelegramMessage(-1, 123, false, false, "/saida 10 pix")).isEmpty());
        assertTrue(handler.responder(new TelegramMessage(123, 123, true, true, "/saida 10 pix")).isEmpty());
        assertTrue(handler.responder(new TelegramMessage(123, 0, true, false, "/saida 10 pix")).isEmpty());
        verifyNoInteractions(service);
    }

    @Test
    void ajudaEErroDeSintaxeNaoChamamMcp() {
        assertTrue(handler.responder(new TelegramMessage(123, 123, true, false, "/start"))
                .orElseThrow().contains("/saida"));
        assertTrue(handler.responder(new TelegramMessage(123, 123, true, false, "/saida"))
                .orElseThrow().contains("valor"));
        verifyNoInteractions(service);
    }

    private String enviar(long usuario, String texto) {
        return handler.responder(new TelegramMessage(usuario, usuario, true, false, texto)).orElseThrow();
    }

    @Test
    void conversaGuardaRespostasESalvaSomenteAoConcluir() {
        when(service.executar(anyString(), anyString(), anyMap())).thenReturn("Lançamento registrado.");
        assertTrue(enviar(123, "/saida").contains("valor"));
        assertTrue(enviar(123, "35,90").contains("forma de pagamento"));
        assertTrue(enviar(123, "pix").contains("descrição"));
        verifyNoInteractions(service);
        assertEquals("Lançamento registrado.", enviar(123, "Almoço"));
        verify(service).executar("registrar", "telegram:123", Map.of("tipo", "SAIDA", "valor", "35,90",
                "forma", "PIX", "descricao", "Almoço"));
        assertTrue(enviar(123, "Outra mensagem").contains("envie /saida"));
        verifyNoMoreInteractions(service);
    }

    @Test
    void entradaSemDescricaoEUsuariosIsolados() {
        when(service.executar(anyString(), anyString(), anyMap())).thenReturn("registrado");
        enviar(123, "/entrada");
        enviar(456, "/saida");
        enviar(123, "1500");
        enviar(123, "transferencia");
        assertEquals("registrado", enviar(123, "-"));
        assertTrue(enviar(456, "20").contains("forma de pagamento"));
        verify(service).executar("registrar", "telegram:123",
                Map.of("tipo", "ENTRADA", "valor", "1500", "forma", "TRANSFERENCIA"));
        verifyNoMoreInteractions(service);
    }

    @Test
    void errosMantemEtapaECancelamentoNaoGrava() {
        enviar(123, "/registrar");
        assertTrue(enviar(123, "invalido").contains("entrada ou saída"));
        assertTrue(enviar(123, "saída").contains("valor"));
        for (String valor : new String[]{"abc", "0", "-10", "1.234,56", "1,234", "92233720368547758.08"}) {
            assertTrue(enviar(123, valor).contains("valor maior"));
        }
        assertTrue(enviar(123, "10").contains("forma de pagamento"));
        assertTrue(enviar(123, "invalido").contains("Forma inválida"));
        assertTrue(enviar(123, "cartão de crédito").contains("descrição"));
        assertTrue(enviar(123, "a".repeat(501)).contains("500"));
        assertTrue(enviar(123, "/cancelar").contains("cancelada"));
        assertTrue(enviar(123, "Almoço").contains("envie /saida"));
        verifyNoInteractions(service);
    }

    @Test
    void consultaGuiadaValidaPeriodoEUsaUsuarioAutenticado() {
        when(service.executar(anyString(), anyString(), anyMap())).thenReturn("encontrado");
        assertTrue(enviar(123, "/consultar").contains("data"));
        assertTrue(enviar(123, "invalido").contains("Data"));
        verifyNoInteractions(service);
        assertEquals("encontrado", enviar(123, "01/11/2026 22/11/2026"));
        verify(service).executar("consultar", "telegram:123",
                Map.of("dataInicio", "2026-11-01", "dataFim", "2026-11-22", "pagina", "1"));
    }

    @Test
    void ajudaExplicitaNaoTrazValoresFicticiosETextoLivreRecebeOrientacaoCurta() {
        String ajuda = enviar(123, "/start");
        assertTrue(ajuda.contains("/saida"));
        assertFalse(ajuda.contains("35,90"));
        assertFalse(ajuda.contains("1500"));
        String orientacao = enviar(123, "oi");
        assertFalse(orientacao.equals(ajuda));
        assertTrue(orientacao.length() < 200);
        verifyNoInteractions(service);
    }

    @Test
    void relatorioUsaUsuarioDoChatSemPedirParametros() {
        when(service.executar(anyString(), anyString(), anyMap())).thenReturn("Relatório financeiro");
        assertEquals("Relatório financeiro", enviar(123, "/relatorio"));
        verify(service).executar("relatorio", "telegram:123", Map.of());
        assertThrows(IllegalArgumentException.class, () -> new TelegramCommandParser().parse("/relatorio outro-usuario"));
    }
}
