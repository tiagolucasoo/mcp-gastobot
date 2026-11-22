package br.edu.ifpr.gastobot.repository;

import br.edu.ifpr.gastobot.model.Lancamento;
import br.edu.ifpr.gastobot.model.ResumoFinanceiro;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.LocalDate;

/** Contrato de persistência independente do banco. */
public interface LancamentoRepository {
    /** Persiste um novo lançamento ou lança uma exceção em caso de falha. */
    void salvar(Lancamento lancamento);

    /** Não revela registros de outros usuários. */
    Optional<Lancamento> buscarPorId(UUID id, String usuarioId);

    List<Lancamento> listarPorPeriodo(String usuarioId, LocalDate inicio, LocalDate fim, int limite, int offset);

    ResumoFinanceiro resumirPorPeriodo(String usuarioId, LocalDate inicio, LocalDate fim);
}
