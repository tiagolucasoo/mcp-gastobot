package br.edu.ifpr.gastobot.client.telegram;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TelegramPollingBotTest {
    private final TelegramGateway telegram = mock(TelegramGateway.class);
    private final TelegramUpdateHandler handler = mock(TelegramUpdateHandler.class);
    private final TelegramPollingBot bot = new TelegramPollingBot(telegram, handler);
    private final TelegramMessage message = new TelegramMessage(123, 123, true, false, "/saida 10 pix");

    @Test
    void avancaOffsetENaoRepeteComandoSeFalharEntrega() throws Exception {
        when(telegram.receber(0)).thenReturn(List.of(new TelegramUpdate(7, message)));
        when(telegram.receber(8)).thenReturn(List.of(new TelegramUpdate(7, message)));
        when(handler.responder(message)).thenReturn(Optional.of("registrado"));
        doThrow(new TelegramApiException(500)).when(telegram).enviar(123, "registrado");
        bot.processarLote();
        bot.processarLote();
        verify(handler, times(1)).responder(message);
        verify(telegram, times(1)).enviar(123, "registrado");
        verify(telegram).receber(8);
    }

    @Test
    void atualizacaoIgnoradaTambemAvancaOffset() throws Exception {
        when(telegram.receber(0)).thenReturn(List.of(new TelegramUpdate(1, null)));
        when(telegram.receber(2)).thenReturn(List.of());
        when(handler.responder(null)).thenReturn(Optional.empty());
        bot.processarLote();
        bot.processarLote();
        verify(telegram).receber(2);
        verify(telegram, never()).enviar(anyLong(), anyString());
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 409})
    void autenticacaoOuConflitoEncerraSemLoopDeTentativas(int code) throws Exception {
        when(telegram.receber(0)).thenThrow(new TelegramApiException(code));
        assertEquals(code, assertThrows(TelegramApiException.class, bot::executar).codigo());
        verify(telegram, times(1)).receber(0);
    }

    @Test
    void interrupcaoDoPollingEhPropagada() throws Exception {
        when(telegram.receber(0)).thenThrow(new InterruptedException());
        assertThrows(InterruptedException.class, bot::executar);
    }
}
