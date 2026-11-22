package br.edu.ifpr.gastobot.client.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TelegramPollingBot {
    private static final Logger LOG = LoggerFactory.getLogger(TelegramPollingBot.class);
    private final TelegramGateway telegram;
    private final TelegramUpdateHandler handler;
    private long offset;

    public TelegramPollingBot(TelegramGateway telegram, TelegramUpdateHandler handler) {
        this.telegram = java.util.Objects.requireNonNull(telegram);
        this.handler = java.util.Objects.requireNonNull(handler);
    }

    public void executar() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                processarLote();
            } catch (TelegramApiException exception) {
                if (exception.codigo() == 401 || exception.codigo() == 409) {
                    throw exception;
                }
                LOG.warn("{}", exception.getMessage());
                Thread.sleep(exception.codigo() == 429 ? 30_000 : 3_000);
            }
        }
    }

    public void processarLote() throws InterruptedException {
        for (var update : telegram.receber(offset)) {
            if (update.id() < offset) {
                continue;
            }
            // Avança antes de responder: falha de entrega não repete uma operação MCP nesta execução.
            offset = update.id() + 1;
            var resposta = handler.responder(update.mensagem());
            if (resposta.isPresent()) {
                try {
                    telegram.enviar(update.mensagem().chatId(), resposta.get());
                } catch (TelegramApiException exception) {
                    if (exception.codigo() == 401 || exception.codigo() == 409) {
                        throw exception;
                    }
                    LOG.warn("Falha ao entregar resposta; a operação não será repetida nesta execução");
                }
            }
        }
    }
}
