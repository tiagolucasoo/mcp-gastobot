package br.edu.ifpr.gastobot.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Registro financeiro em reais. O tipo determina o efeito sobre o saldo. */
public record Lancamento(
        UUID id,
        String usuarioId,
        TipoLancamento tipo,
        BigDecimal valor,
        FormaPagamento formaPagamento,
        String descricao,
        LocalDate data) {

    public Lancamento {
        Objects.requireNonNull(id, "ID obrigatório");
        Objects.requireNonNull(tipo, "Tipo obrigatório");
        Objects.requireNonNull(valor, "Valor obrigatório");
        Objects.requireNonNull(formaPagamento, "Forma de pagamento obrigatória");
        Objects.requireNonNull(data, "Data obrigatória");
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new IllegalArgumentException("Usuário obrigatório");
        }
        usuarioId = usuarioId.trim();
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("Valor deve ser maior que zero");
        }
        try {
            valor = valor.setScale(2, RoundingMode.UNNECESSARY);
            valor.movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Valor deve caber em centavos inteiros de 64 bits e ter até duas casas decimais",
                    exception);
        }
        descricao = descricao == null ? "" : descricao.trim();
    }
}
