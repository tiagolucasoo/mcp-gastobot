package br.edu.ifpr.gastobot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.edu.ifpr.gastobot.model.FormaPagamento;
import br.edu.ifpr.gastobot.model.TipoLancamento;

/** Dados de entrada. A descrição é opcional; os demais campos são obrigatórios. */
public record CriarLancamentoRequest(
        String usuarioId,
        TipoLancamento tipo,
        BigDecimal valor,
        FormaPagamento formaPagamento,
        String descricao,
        LocalDate data) {
}
