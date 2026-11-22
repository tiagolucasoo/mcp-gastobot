package br.edu.ifpr.gastobot.client.telegram;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Converte comandos textuais em opções, sem executar operações financeiras. */
public final class TelegramCommandParser {
    public static final String AJUDA = """
            GastoBot — controle de entradas e saídas

            /saida — registrar uma saída, respondendo às perguntas
            /entrada — registrar uma entrada, respondendo às perguntas
            /registrar — escolher o tipo e registrar
            /consultar — consultar lançamentos por data ou período
            /relatorio — totais e quantidades do dia, semana e mês
            /cancelar — abandonar a operação em andamento

            Formas: pix, dinheiro, debito, credito, transferencia e boleto.
            O cadastro guiado usa a data de hoje. Não use separador de milhar no valor.
            Também aceita comandos completos: /saida VALOR FORMA DESCRIÇÃO
            Data opcional no comando completo: data=AAAA-MM-DD antes da descrição.
            Consulta aceita DD/MM/AAAA ou AAAA-MM-DD. Use este bot no chat privado.
            """;

    public TelegramCommand parse(String texto) {
        if (texto == null || texto.isBlank()) {
            return new TelegramCommand("ajuda", Map.of());
        }
        String[] tokens = texto.strip().split("\\s+");
        String comando = tokens[0].split("@", 2)[0].toLowerCase(Locale.ROOT);
        return switch (comando) {
            case "/start", "/ajuda", "/help" -> new TelegramCommand("ajuda", Map.of());
            case "/consultar" -> {
                if (tokens.length < 2) {
                    throw new IllegalArgumentException("Use /consultar DATA ou /consultar DATA_INICIAL DATA_FINAL");
                }
                if (tokens.length == 2 && uuid(tokens[1])) {
                    yield new TelegramCommand("consultar", Map.of("id", tokens[1]));
                }
                yield new TelegramCommand("consultar", TelegramDateQuery.interpretar(
                        String.join(" ", java.util.Arrays.copyOfRange(tokens, 1, tokens.length))).opcoes());
            }
            case "/relatorio" -> {
                if (tokens.length != 1) {
                    throw new IllegalArgumentException("Use /relatorio sem parâmetros para resumir dia, semana e mês atuais");
                }
                yield new TelegramCommand("relatorio", Map.of());
            }
            case "/saida", "/entrada" -> registro(tokens, comando.substring(1), 1);
            case "/registrar" -> {
                if (tokens.length < 4) {
                    throw new IllegalArgumentException("Use /registrar tipo valor forma descrição");
                }
                yield registro(tokens, tokens[1], 2);
            }
            default -> new TelegramCommand("ajuda", Map.of());
        };
    }

    private TelegramCommand registro(String[] tokens, String tipo, int inicio) {
        if (tokens.length < inicio + 2) {
            throw new IllegalArgumentException("Informe valor e forma de pagamento. Ex.: /saida 35,90 pix Almoço");
        }
        String tipoNormalizado = normalizar(tipo);
        if (!tipoNormalizado.equals("ENTRADA") && !tipoNormalizado.equals("SAIDA")) {
            throw new IllegalArgumentException("Tipo deve ser entrada ou saida");
        }
        var opcoes = new HashMap<String, String>();
        opcoes.put("tipo", tipoNormalizado);
        opcoes.put("valor", tokens[inicio]);
        opcoes.put("forma", formaPagamento(tokens[inicio + 1]));
        int descricaoInicio = inicio + 2;
        if (tokens.length > descricaoInicio && tokens[descricaoInicio].startsWith("data=")) {
            opcoes.put("data", tokens[descricaoInicio].substring(5));
            descricaoInicio++;
        }
        if (tokens.length > descricaoInicio) {
            String descricao = String.join(" ", java.util.Arrays.copyOfRange(tokens, descricaoInicio, tokens.length));
            if (descricao.length() > 500) {
                throw new IllegalArgumentException("Descrição deve ter até 500 caracteres");
            }
            opcoes.put("descricao", descricao);
        }
        return new TelegramCommand("registrar", opcoes);
    }

    public static String formaPagamento(String valor) {
        return switch (normalizar(valor).replaceAll("\\s+", "_")) {
            case "DEBITO", "CARTAO_DEBITO", "CARTAO_DE_DEBITO" -> "CARTAO_DEBITO";
            case "CREDITO", "CARTAO_CREDITO", "CARTAO_DE_CREDITO" -> "CARTAO_CREDITO";
            case "PIX" -> "PIX";
            case "DINHEIRO" -> "DINHEIRO";
            case "TRANSFERENCIA" -> "TRANSFERENCIA";
            case "BOLETO" -> "BOLETO";
            default -> throw new IllegalArgumentException("Forma inválida. Use pix, dinheiro, debito, credito, transferencia ou boleto");
        };
    }

    private static boolean uuid(String texto) {
        try {
            java.util.UUID.fromString(texto);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }
}
