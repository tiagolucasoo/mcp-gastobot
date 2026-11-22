package br.edu.ifpr.gastobot.controller;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import br.edu.ifpr.gastobot.dto.CriarLancamentoRequest;
import br.edu.ifpr.gastobot.dto.LancamentoResponse;
import br.edu.ifpr.gastobot.dto.ConsultarLancamentosRequest;
import br.edu.ifpr.gastobot.dto.ConsultaLancamentosResponse;
import br.edu.ifpr.gastobot.dto.RelatorioFinanceiroRequest;
import br.edu.ifpr.gastobot.dto.RelatorioFinanceiroResponse;
import br.edu.ifpr.gastobot.dto.ResumoPeriodoResponse;
import br.edu.ifpr.gastobot.model.Lancamento;
import br.edu.ifpr.gastobot.service.LancamentoService;

/** Ponto de entrada para os adaptadores, sem dependência do protocolo MCP. */
public final class LancamentoController {
    private final LancamentoService service;

    public LancamentoController(LancamentoService service) {
        this.service = Objects.requireNonNull(service, "Service obrigatório");
    }

    public LancamentoResponse registrar(CriarLancamentoRequest request) {
        return resposta(service.registrar(request));
    }

    public Optional<LancamentoResponse> buscarPorId(UUID id, String usuarioId) {
        return service.buscarPorId(id, usuarioId).map(this::resposta);
    }

    public ConsultaLancamentosResponse consultar(ConsultarLancamentosRequest request) {
        var lancamentos = service.consultar(request);
        return new ConsultaLancamentosResponse(request.dataInicio(), request.dataFim(), request.pagina(),
                lancamentos.size() > LancamentoService.TAMANHO_PAGINA,
                lancamentos.stream().limit(LancamentoService.TAMANHO_PAGINA).map(this::resposta).toList());
    }

    public RelatorioFinanceiroResponse relatorio(RelatorioFinanceiroRequest request) {
        var periodos = service.relatorio(request).stream().map(periodo -> new ResumoPeriodoResponse(
                periodo.periodo(), periodo.inicio(), periodo.fim(), periodo.totais().totalEntradas(),
                periodo.totais().totalSaidas(), periodo.totais().quantidadeEntradas(),
                periodo.totais().quantidadeSaidas(), periodo.totais().quantidade())).toList();
        return new RelatorioFinanceiroResponse(request.dataReferencia(), periodos);
    }

    private LancamentoResponse resposta(Lancamento lancamento) {
        return new LancamentoResponse(lancamento.id(), lancamento.usuarioId(),
                lancamento.tipo(), lancamento.valor(), lancamento.formaPagamento(),
                lancamento.descricao(), lancamento.data());
    }
}
