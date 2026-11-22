package br.edu.ifpr.gastobot.client.telegram;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Estado da conversa de cada usuário no chat privado; não grava dados por conta própria. */
public final class TelegramConversation {
    private enum Etapa { TIPO, VALOR, FORMA, DESCRICAO, PERIODO }
    private record Chave(long chatId, String usuarioId) { }
    private record Estado(Etapa etapa, Map<String, String> opcoes) { }

    private final TelegramCommandParser parser;
    private final Map<Chave, Estado> conversas = new HashMap<>();

    public TelegramConversation(TelegramCommandParser parser) {
        this.parser = java.util.Objects.requireNonNull(parser);
    }

    public String responder(String entrada, long chatId, String usuarioId, TelegramCommandService service) {
        try {
            var step = interpretar(new Chave(chatId, usuarioId), entrada);
            if (step instanceof TelegramStep.Pergunta pergunta) {
                return pergunta.texto();
            }
            var command = ((TelegramStep.Executar) step).comando();
            return service.executar(command.nome(), usuarioId, command.opcoes());
        } catch (IllegalArgumentException exception) {
            return exception.getMessage();
        }
    }

    private TelegramStep interpretar(Chave chave, String entrada) {
        String texto = entrada == null ? "" : entrada.strip();
        String[] partes = texto.split("\\s+", 2);
        String comando = partes[0].split("@", 2)[0].toLowerCase(Locale.ROOT);
        if (comando.equals("/cancelar")) {
            return pergunta(conversas.remove(chave) != null
                    ? "Operação cancelada. Nenhum lançamento foi registrado nesta conversa."
                    : "Não há operação em andamento. Envie /saida ou /entrada para começar.");
        }
        if (comando.equals("/start") || comando.equals("/ajuda") || comando.equals("/help")) {
            conversas.remove(chave);
            return pergunta(TelegramCommandParser.AJUDA);
        }
        if (partes.length == 1) {
            String atalho = normalizar(comando);
            if (atalho.equals("/SAIDA") || (atalho.equals("SAIDA") && !conversas.containsKey(chave))) {
                return iniciar(chave, Etapa.VALOR, Map.of("tipo", "SAIDA"), "Qual é o valor da saída?");
            }
            if (atalho.equals("/ENTRADA") || (atalho.equals("ENTRADA") && !conversas.containsKey(chave))) {
                return iniciar(chave, Etapa.VALOR, Map.of("tipo", "ENTRADA"), "Qual é o valor da entrada?");
            }
            if (comando.equals("/registrar")) {
                return iniciar(chave, Etapa.TIPO, Map.of(), "É uma entrada ou uma saída?");
            }
            if (comando.equals("/consultar")) {
                return iniciar(chave, Etapa.PERIODO, Map.of(),
                        "Envie uma data ou um período: DATA_INICIAL DATA_FINAL. Use DD/MM/AAAA ou AAAA-MM-DD. As duas datas são incluídas.");
            }
        }
        if (texto.startsWith("/")) {
            var command = parser.parse(texto);
            if (command.nome().equals("ajuda")) {
                return pergunta("Comando não reconhecido. Use /saida, /entrada, /consultar, /relatorio ou /cancelar.");
            }
            conversas.remove(chave);
            return new TelegramStep.Executar(command);
        }
        Estado estado = conversas.get(chave);
        if (estado == null) {
            return pergunta("Para registrar um gasto, envie /saida. Para uma receita, envie /entrada. Para consultar, envie /consultar. Para totais, envie /relatorio.");
        }
        return continuar(chave, estado, texto);
    }

    private TelegramStep continuar(Chave chave, Estado estado, String texto) {
        var opcoes = new HashMap<>(estado.opcoes());
        return switch (estado.etapa()) {
            case TIPO -> {
                String tipo = normalizar(texto);
                if (!tipo.equals("ENTRADA") && !tipo.equals("SAIDA")) {
                    yield pergunta("Informe entrada ou saída, ou envie /cancelar.");
                }
                opcoes.put("tipo", tipo);
                yield iniciar(chave, Etapa.VALOR, opcoes, "Qual é o valor?");
            }
            case VALOR -> {
                if (!valorValido(texto)) {
                    yield pergunta("Informe um valor maior que zero, com até duas casas decimais e sem separador de milhar.");
                }
                opcoes.put("valor", texto);
                yield iniciar(chave, Etapa.FORMA, opcoes,
                        "Qual é a forma de pagamento? pix, dinheiro, debito, credito, transferencia ou boleto.");
            }
            case FORMA -> {
                try {
                    opcoes.put("forma", TelegramCommandParser.formaPagamento(texto));
                } catch (IllegalArgumentException exception) {
                    yield pergunta(exception.getMessage());
                }
                yield iniciar(chave, Etapa.DESCRICAO, opcoes,
                        "Qual é a descrição? Envie - para deixar em branco. Após sua resposta, o lançamento será salvo com a data de hoje.");
            }
            case DESCRICAO -> {
                if (texto.length() > 500) {
                    yield pergunta("A descrição deve ter até 500 caracteres. Envie novamente ou use /cancelar.");
                }
                if (!texto.equals("-") && !texto.equalsIgnoreCase("pular")) {
                    opcoes.put("descricao", texto);
                }
                conversas.remove(chave);
                yield new TelegramStep.Executar(new TelegramCommand("registrar", opcoes));
            }
            case PERIODO -> {
                TelegramDateQuery consulta;
                try {
                    consulta = TelegramDateQuery.interpretar(texto);
                } catch (IllegalArgumentException exception) {
                    yield pergunta(exception.getMessage());
                }
                conversas.remove(chave);
                yield new TelegramStep.Executar(new TelegramCommand("consultar", consulta.opcoes()));
            }
        };
    }

    private TelegramStep iniciar(Chave chave, Etapa etapa, Map<String, String> opcoes, String texto) {
        conversas.put(chave, new Estado(etapa, Map.copyOf(opcoes)));
        return pergunta(texto);
    }

    private static TelegramStep pergunta(String texto) {
        return new TelegramStep.Pergunta(texto);
    }

    private static boolean valorValido(String texto) {
        if (!texto.matches("[0-9]+([.,][0-9]{1,2})?")) {
            return false;
        }
        try {
            var valor = new BigDecimal(texto.replace(',', '.'));
            return valor.signum() > 0 && valor.movePointRight(2).longValueExact() > 0;
        } catch (ArithmeticException exception) {
            return false;
        }
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
    }
}
