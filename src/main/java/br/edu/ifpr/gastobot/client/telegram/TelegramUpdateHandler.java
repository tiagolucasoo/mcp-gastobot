package br.edu.ifpr.gastobot.client.telegram;

import java.util.Optional;

public final class TelegramUpdateHandler {
    private final TelegramConversation conversation;
    private final TelegramCommandService service;

    public TelegramUpdateHandler(TelegramCommandParser parser, TelegramCommandService service) {
        this.conversation = new TelegramConversation(parser);
        this.service = java.util.Objects.requireNonNull(service);
    }

    public Optional<String> responder(TelegramMessage message) {
        if (message == null || !message.privado() || message.bot() || message.usuarioId() <= 0) {
            return Optional.empty();
        }
        return Optional.of(conversation.responder(message.texto(), message.chatId(),
                "telegram:" + message.usuarioId(), service));
    }
}
