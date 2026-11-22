package br.edu.ifpr.gastobot.client.telegram;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.gastobot.client.mcp.McpGateway;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Adaptação dos comandos Telegram para MCP; não acessa services ou repositories do servidor. */
public final class TelegramCommandService {
    private static final Logger LOG = LoggerFactory.getLogger(TelegramCommandService.class);
    private final McpGateway gateway;
    private final Clock clock;
    private final JsonMapper json = new JsonMapper();

    public TelegramCommandService(McpGateway gateway, Clock clock) {
        this.gateway = Objects.requireNonNull(gateway);
        this.clock = Objects.requireNonNull(clock);
    }

    /** O usuário vem do evento autenticado, nunca de uma opção do comando. */
    public String executar(String comando, String usuarioId, Map<String, String> opcoes) {
        try {
            if (usuarioId == null || usuarioId.isBlank()) {
                throw new IllegalArgumentException("Usuário não identificado");
            }
            return switch (comando) {
                case "registrar" -> responder(gateway.executar("registrar_lancamento",
                        argumentosDeRegistro(usuarioId, opcoes)), "Lançamento registrado.");
                case "consultar" -> opcoes.containsKey("id")
                        ? responder(gateway.executar("buscar_lancamento",
                                Map.of("id", obrigatorio(opcoes, "id"), "usuarioId", usuarioId)), "Lançamento encontrado.")
                        : consultarPeriodo(usuarioId, opcoes);
                case "relatorio" -> relatorio(usuarioId);
                default -> "Comando desconhecido. Use /registrar, /consultar ou /relatorio.";
            };
        } catch (IllegalArgumentException | DateTimeException exception) {
            return "Dados inválidos. Confira os campos, o valor, as datas do período e a página.";
        } catch (RuntimeException exception) {
            LOG.error("Falha ao executar comando Telegram por MCP", exception);
            return "Não foi possível confirmar a operação. Confira seus registros antes de repetir um cadastro.";
        }
    }

    private Map<String, Object> argumentosDeRegistro(String usuarioId, Map<String, String> opcoes) {
        String valor = obrigatorio(opcoes, "valor");
        if (!valor.matches("[0-9]+([.,][0-9]{1,2})?")) {
            throw new IllegalArgumentException("Use valor decimal sem separador de milhar");
        }
        String data = opcoes.get("data");
        if (data == null) {
            data = LocalDate.now(clock).toString();
        } else {
            data = LocalDate.parse(data).toString();
        }
        var argumentos = new HashMap<String, Object>();
        argumentos.put("usuarioId", usuarioId);
        argumentos.put("tipo", obrigatorio(opcoes, "tipo"));
        argumentos.put("valor", valor.replace(',', '.'));
        argumentos.put("formaPagamento", obrigatorio(opcoes, "forma"));
        argumentos.put("data", data);
        if (opcoes.containsKey("descricao")) {
            argumentos.put("descricao", opcoes.get("descricao"));
        }
        return argumentos;
    }

    private String responder(CallToolResult result, String titulo) {
        String texto = result.content().stream().filter(TextContent.class::isInstance)
                .map(TextContent.class::cast).map(TextContent::text).findFirst().orElseThrow();
        JsonNode dados = json.readTree(texto);
        if (Boolean.TRUE.equals(result.isError())) {
            return erro(dados);
        }
        String resposta = titulo
                // + "\nID: " + campo(dados, "id") // Reative para exibir o identificador no Telegram.
                + "\nTipo: " + campo(dados, "tipo")
                + "\nValor: R$ " + campo(dados, "valor").replace('.', ',')
                + "\nForma de pagamento: " + campo(dados, "formaPagamento")
                + "\nData: " + campo(dados, "data")
                + "\nDescrição: " + campo(dados, "descricao");
        return resposta.length() <= 1900 ? resposta : resposta.substring(0, 1897) + "...";
    }

    private String consultarPeriodo(String usuarioId, Map<String, String> opcoes) {
        LocalDate inicio = LocalDate.parse(obrigatorio(opcoes, "dataInicio"));
        LocalDate fim = LocalDate.parse(obrigatorio(opcoes, "dataFim"));
        int pagina = Integer.parseInt(opcoes.getOrDefault("pagina", "1"));
        if (fim.isBefore(inicio) || pagina < 1) {
            throw new IllegalArgumentException("Período ou página inválido");
        }
        var result = gateway.executar("listar_lancamentos", Map.of("usuarioId", usuarioId,
                "dataInicio", inicio.toString(), "dataFim", fim.toString(), "pagina", pagina));
        String texto = result.content().stream().filter(TextContent.class::isInstance).map(TextContent.class::cast)
                .map(TextContent::text).findFirst().orElseThrow();
        JsonNode dados = json.readTree(texto);
        if (Boolean.TRUE.equals(result.isError())) {
            return erro(dados);
        }
        JsonNode lancamentos = dados.path("lancamentos");
        if (lancamentos.isEmpty()) {
            return "Nenhum lançamento encontrado de " + inicio + " a " + fim + " na página " + pagina + ".";
        }
        var resposta = new StringBuilder("Lançamentos de " + inicio + " a " + fim + "\nPágina " + pagina + "\n");
        for (var lancamento : lancamentos) {
            resposta.append("\n").append(campo(lancamento, "data")).append(" | ").append(campo(lancamento, "tipo"))
                    .append(" | R$ ").append(campo(lancamento, "valor").replace('.', ','))
                    .append(" | ").append(campo(lancamento, "formaPagamento"))
                    .append("\n").append(descricaoCurta(campo(lancamento, "descricao")));
            // resposta.append("\nID: ").append(campo(lancamento, "id")); // Exibição opcional do ID.
            resposta.append("\n");
        }
        if (dados.path("temProximaPagina").asBoolean(false)) {
            resposta.append("\nPróxima página: /consultar ").append(inicio).append(" ").append(fim)
                    .append(" pagina=").append(pagina + 1);
        }
        return resposta.toString();
    }

    private static String descricaoCurta(String texto) {
        String descricao = texto.replaceAll("\\s+", " ");
        int fim = descricao.offsetByCodePoints(0, Math.min(60, descricao.codePointCount(0, descricao.length())));
        return fim < descricao.length() ? descricao.substring(0, fim) + "..." : descricao;
    }

    private String relatorio(String usuarioId) {
        var result = gateway.executar("relatorio_financeiro", Map.of("usuarioId", usuarioId,
                "dataReferencia", LocalDate.now(clock).toString()));
        String texto = result.content().stream().filter(TextContent.class::isInstance).map(TextContent.class::cast)
                .map(TextContent::text).findFirst().orElseThrow();
        JsonNode dados = json.readTree(texto);
        if (Boolean.TRUE.equals(result.isError())) {
            return erro(dados);
        }
        var resposta = new StringBuilder("Relatório financeiro\n");
        for (var periodo : dados.path("periodos")) {
            String nome = switch (campo(periodo, "periodo")) {
                case "DIA" -> "Dia";
                case "SEMANA" -> "Semana";
                case "MES" -> "Mês";
                default -> throw new IllegalStateException("Período de relatório desconhecido");
            };
            resposta.append("\n").append(nome).append(" (").append(campo(periodo, "dataInicio"));
            if (!campo(periodo, "dataInicio").equals(campo(periodo, "dataFim"))) {
                resposta.append(" a ").append(campo(periodo, "dataFim"));
            }
            resposta.append(")\nEntradas: R$ ").append(campo(periodo, "totalEntradas").replace('.', ','))
                    .append(" | Quantidade: ").append(periodo.path("quantidadeEntradas").asLong())
                    .append("\nSaídas: R$ ").append(campo(periodo, "totalSaidas").replace('.', ','))
                    .append(" | Quantidade: ").append(periodo.path("quantidadeSaidas").asLong())
                    .append("\nQuantidade total: ").append(periodo.path("quantidade").asLong()).append("\n");
        }
        return resposta.toString();
    }

    private static String erro(JsonNode dados) {
        return switch (dados.path("codigo").asString()) {
                case "DADOS_INVALIDOS" -> "Dados inválidos. Confira tipo, valor positivo, forma de pagamento, data e ID.";
                case "NAO_ENCONTRADO" -> "Lançamento não encontrado para o seu usuário.";
                case "ERRO_PERSISTENCIA" -> "O banco de dados está indisponível. Não foi possível concluir a operação.";
                default -> "O servidor não conseguiu concluir a operação.";
        };
    }

    private static String campo(JsonNode dados, String nome) {
        return dados.path(nome).asString();
    }

    private static String obrigatorio(Map<String, String> opcoes, String nome) {
        String value = opcoes.get(nome);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório: " + nome);
        }
        return value.trim();
    }
}
