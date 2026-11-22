package br.edu.ifpr.gastobot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.edu.ifpr.gastobot.model.FormaPagamento;
import br.edu.ifpr.gastobot.model.TipoLancamento;

public record LancamentoResponse(
        UUID id,
        String usuarioId,
        TipoLancamento tipo,
        BigDecimal valor,
        FormaPagamento formaPagamento,
        String descricao,
        LocalDate data) {
}
