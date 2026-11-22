package br.edu.ifpr.gastobot.server.mcp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.gastobot.controller.LancamentoController;
import br.edu.ifpr.gastobot.dto.CriarLancamentoRequest;
import br.edu.ifpr.gastobot.dto.LancamentoResponse;
import br.edu.ifpr.gastobot.dto.ConsultarLancamentosRequest;
import br.edu.ifpr.gastobot.dto.RelatorioFinanceiroRequest;
import br.edu.ifpr.gastobot.exception.PersistenciaException;
import br.edu.ifpr.gastobot.model.FormaPagamento;
import br.edu.ifpr.gastobot.model.TipoLancamento;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

/** Adapta chamadas MCP aos casos de uso de lançamentos. */
public final class LancamentoTools {
    private static final Logger LOG = LoggerFactory.getLogger(LancamentoTools.class);
    private final LancamentoController controller;
    private final JsonMapper json = new JsonMapper();

    public LancamentoTools(LancamentoController controller) {
        this.controller = java.util.Objects.requireNonNull(controller);
    }

    public List<SyncToolSpecification> specifications() {
        Map<String, Object> criarSchema = Map.of(
                "type", "object", "additionalProperties", false,
                "properties", Map.of(
                        "usuarioId", texto("Identificador do usuário"),
                        "tipo", Map.of("type", "string", "enum", List.of("ENTRADA", "SAIDA")),
                        "valor", texto("Valor positivo em reais, como texto decimal: 35.90"),
                        "formaPagamento", Map.of("type", "string", "enum",
                                java.util.Arrays.stream(FormaPagamento.values()).map(Enum::name).toList()),
                        "descricao", texto("Descrição opcional"),
                        "data", Map.of("type", "string", "format", "date", "description", "Data AAAA-MM-DD")),
                "required", List.of("usuarioId", "tipo", "valor", "formaPagamento", "data"));
        Map<String, Object> buscarSchema = Map.of(
                "type", "object", "additionalProperties", false,
                "properties", Map.of("id", texto("UUID do lançamento"),
                        "usuarioId", texto("Identificador do proprietário")),
                "required", List.of("id", "usuarioId"));
        Map<String, Object> listarSchema = Map.of("type", "object", "additionalProperties", false,
                "properties", Map.of("usuarioId", texto("Identificador do proprietário"),
                        "dataInicio", Map.of("type", "string", "format", "date"),
                        "dataFim", Map.of("type", "string", "format", "date"),
                        "pagina", Map.of("type", "integer", "minimum", 1, "default", 1)),
                "required", List.of("usuarioId", "dataInicio", "dataFim"));
        Map<String, Object> relatorioSchema = Map.of("type", "object", "additionalProperties", false,
                "properties", Map.of("usuarioId", texto("Identificador do proprietário"),
                        "dataReferencia", Map.of("type", "string", "format", "date")),
                "required", List.of("usuarioId", "dataReferencia"));
        return List.of(
                SyncToolSpecification.builder()
                        .tool(Tool.builder("registrar_lancamento", criarSchema)
                                .description("Registra uma entrada ou saída no SQLite. Cada chamada cria um novo lançamento.")
                                .build())
                        .callHandler((exchange, request) -> registrar(request.arguments())).build(),
                SyncToolSpecification.builder()
                        .tool(Tool.builder("buscar_lancamento", buscarSchema)
                                .description("Busca um lançamento por UUID e usuário proprietário.").build())
                        .callHandler((exchange, request) -> buscar(request.arguments())).build(),
                SyncToolSpecification.builder()
                        .tool(Tool.builder("listar_lancamentos", listarSchema)
                                .description("Lista até 10 lançamentos por página, em período inclusivo, ordenados por data e ID.").build())
                        .callHandler((exchange, request) -> listar(request.arguments())).build(),
                SyncToolSpecification.builder()
                        .tool(Tool.builder("relatorio_financeiro", relatorioSchema)
                                .description("Totais e quantidades de entradas e saídas no dia, semana (segunda a domingo) e mês calendário da data informada.").build())
                        .callHandler((exchange, request) -> relatorio(request.arguments())).build());
    }

    public CallToolResult registrar(Map<String, Object> argumentos) {
        return executar(() -> {
            validarCampos(argumentos, Set.of("usuarioId", "tipo", "valor", "formaPagamento", "descricao", "data"));
            String descricao = argumentos.containsKey("descricao") ? texto(argumentos, "descricao") : null;
            return dados(controller.registrar(new CriarLancamentoRequest(
                    texto(argumentos, "usuarioId"), TipoLancamento.valueOf(texto(argumentos, "tipo")),
                    new BigDecimal(texto(argumentos, "valor")),
                    FormaPagamento.valueOf(texto(argumentos, "formaPagamento")), descricao,
                    LocalDate.parse(texto(argumentos, "data")))));
        });
    }

    public CallToolResult buscar(Map<String, Object> argumentos) {
        return executar(() -> {
            validarCampos(argumentos, Set.of("id", "usuarioId"));
            return controller.buscarPorId(UUID.fromString(texto(argumentos, "id")),
                    texto(argumentos, "usuarioId")).map(this::dados).orElse(null);
        });
    }

    public CallToolResult listar(Map<String, Object> argumentos) {
        return executar(() -> {
            validarCampos(argumentos, Set.of("usuarioId", "dataInicio", "dataFim", "pagina"));
            int pagina = 1;
            if (argumentos.containsKey("pagina")) {
                if (!(argumentos.get("pagina") instanceof Number numero)) {
                    throw new IllegalArgumentException("Página deve ser inteira");
                }
                try {
                    pagina = new BigDecimal(numero.toString()).intValueExact();
                } catch (ArithmeticException exception) {
                    throw new IllegalArgumentException("Página inválida");
                }
            }
            var response = controller.consultar(new ConsultarLancamentosRequest(texto(argumentos, "usuarioId"),
                    LocalDate.parse(texto(argumentos, "dataInicio")), LocalDate.parse(texto(argumentos, "dataFim")), pagina));
            return Map.of("dataInicio", response.dataInicio().toString(), "dataFim", response.dataFim().toString(),
                    "pagina", response.pagina(), "temProximaPagina", response.temProximaPagina(),
                    "lancamentos", response.lancamentos().stream().map(this::dados).toList());
        });
    }

    private Map<String, Object> dados(LancamentoResponse response) {
        return Map.of("id", response.id().toString(), "usuarioId", response.usuarioId(),
                "tipo", response.tipo().name(), "valor", response.valor().toPlainString(),
                "formaPagamento", response.formaPagamento().name(),
                "descricao", response.descricao(), "data", response.data().toString());
    }

    public CallToolResult relatorio(Map<String, Object> argumentos) {
        return executar(() -> {
            validarCampos(argumentos, Set.of("usuarioId", "dataReferencia"));
            var response = controller.relatorio(new RelatorioFinanceiroRequest(texto(argumentos, "usuarioId"),
                    LocalDate.parse(texto(argumentos, "dataReferencia"))));
            return Map.of("dataReferencia", response.dataReferencia().toString(), "periodos",
                    response.periodos().stream().map(periodo -> Map.<String, Object>of(
                            "periodo", periodo.periodo(), "dataInicio", periodo.dataInicio().toString(),
                            "dataFim", periodo.dataFim().toString(), "totalEntradas", periodo.totalEntradas().toPlainString(),
                            "totalSaidas", periodo.totalSaidas().toPlainString(), "quantidadeEntradas", periodo.quantidadeEntradas(),
                            "quantidadeSaidas", periodo.quantidadeSaidas(), "quantidade", periodo.quantidade())).toList());
        });
    }

    private CallToolResult executar(Supplier<Map<String, Object>> operacao) {
        try {
            Map<String, Object> response = operacao.get();
            if (response == null) {
                return erro("NAO_ENCONTRADO", "Lançamento não encontrado para o usuário informado");
            }
            return resultado(response, false);
        } catch (IllegalArgumentException | DateTimeException exception) {
            return erro("DADOS_INVALIDOS", "Confira os campos obrigatórios, tipos, enums, valor e data AAAA-MM-DD");
        } catch (PersistenciaException exception) {
            LOG.error("Falha de persistência em ferramenta MCP", exception);
            return erro("ERRO_PERSISTENCIA", "Não foi possível acessar o banco de dados");
        }
    }

    private CallToolResult erro(String codigo, String mensagem) {
        return resultado(Map.of("codigo", codigo, "mensagem", mensagem), true);
    }

    private CallToolResult resultado(Map<String, Object> dados, boolean erro) {
        return CallToolResult.builder()
                .content(List.of(TextContent.builder(json.writeValueAsString(dados)).build()))
                .isError(erro).build();
    }

    private static Map<String, Object> texto(String descricao) {
        return Map.of("type", "string", "description", descricao);
    }

    private static String texto(Map<String, Object> argumentos, String campo) {
        if (!(argumentos.get(campo) instanceof String value)) {
            throw new IllegalArgumentException("Campo deve ser texto: " + campo);
        }
        return value;
    }

    private static void validarCampos(Map<String, Object> argumentos, Set<String> permitidos) {
        if (argumentos == null || !permitidos.containsAll(argumentos.keySet())) {
            throw new IllegalArgumentException("Argumentos ausentes ou desconhecidos");
        }
    }
}
