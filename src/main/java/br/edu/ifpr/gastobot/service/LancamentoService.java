package br.edu.ifpr.gastobot.service;

import java.util.Objects;
import java.util.UUID;
import java.util.Optional;
import java.util.List;
import br.edu.ifpr.gastobot.dto.ConsultarLancamentosRequest;
import br.edu.ifpr.gastobot.dto.RelatorioFinanceiroRequest;
import br.edu.ifpr.gastobot.model.ResumoPeriodo;
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

import br.edu.ifpr.gastobot.dto.CriarLancamentoRequest;
import br.edu.ifpr.gastobot.model.Lancamento;
import br.edu.ifpr.gastobot.repository.LancamentoRepository;

public final class LancamentoService {
    public static final int TAMANHO_PAGINA = 10;
    private final LancamentoRepository repository;

    public LancamentoService(LancamentoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository obrigatório");
    }

    public Lancamento registrar(CriarLancamentoRequest request) {
        Objects.requireNonNull(request, "Dados do lançamento obrigatórios");
        Lancamento lancamento = new Lancamento(UUID.randomUUID(), request.usuarioId(),
                request.tipo(), request.valor(), request.formaPagamento(),
                request.descricao(), request.data());
        repository.salvar(lancamento);
        return lancamento;
    }

    public Optional<Lancamento> buscarPorId(UUID id, String usuarioId) {
        Objects.requireNonNull(id, "ID obrigatório");
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new IllegalArgumentException("Usuário obrigatório");
        }
        return repository.buscarPorId(id, usuarioId.trim());
    }

    /** Busca um registro adicional para indicar a existência da próxima página. */
    public List<Lancamento> consultar(ConsultarLancamentosRequest request) {
        Objects.requireNonNull(request, "Consulta obrigatória");
        if (request.usuarioId() == null || request.usuarioId().isBlank()) {
            throw new IllegalArgumentException("Usuário obrigatório");
        }
        if (request.dataInicio() == null || request.dataFim() == null
                || request.dataFim().isBefore(request.dataInicio())) {
            throw new IllegalArgumentException("Informe datas válidas, com fim igual ou posterior ao início");
        }
        if (request.pagina() < 1) {
            throw new IllegalArgumentException("Página deve ser maior que zero");
        }
        int offset;
        try {
            offset = Math.multiplyExact(request.pagina() - 1, TAMANHO_PAGINA);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Página fora do limite suportado");
        }
        return repository.listarPorPeriodo(request.usuarioId().trim(), request.dataInicio(), request.dataFim(),
                TAMANHO_PAGINA + 1, offset);
    }

    public List<ResumoPeriodo> relatorio(RelatorioFinanceiroRequest request) {
        Objects.requireNonNull(request, "Relatório obrigatório");
        if (request.usuarioId() == null || request.usuarioId().isBlank() || request.dataReferencia() == null) {
            throw new IllegalArgumentException("Usuário e data de referência são obrigatórios");
        }
        String usuario = request.usuarioId().trim();
        var data = request.dataReferencia();
        var segunda = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var mes = YearMonth.from(data);
        return List.of(
                new ResumoPeriodo("DIA", data, data, repository.resumirPorPeriodo(usuario, data, data)),
                new ResumoPeriodo("SEMANA", segunda, segunda.plusDays(6),
                        repository.resumirPorPeriodo(usuario, segunda, segunda.plusDays(6))),
                new ResumoPeriodo("MES", mes.atDay(1), mes.atEndOfMonth(),
                        repository.resumirPorPeriodo(usuario, mes.atDay(1), mes.atEndOfMonth())));
    }
}
