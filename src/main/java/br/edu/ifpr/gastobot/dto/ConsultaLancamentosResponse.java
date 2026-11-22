package br.edu.ifpr.gastobot.dto;

import java.time.LocalDate;
import java.util.List;

public record ConsultaLancamentosResponse(LocalDate dataInicio, LocalDate dataFim, int pagina,
        boolean temProximaPagina, List<LancamentoResponse> lancamentos) {
    public ConsultaLancamentosResponse {
        lancamentos = List.copyOf(lancamentos);
    }
}
