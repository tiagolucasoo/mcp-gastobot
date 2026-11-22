package br.edu.ifpr.gastobot.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import br.edu.ifpr.gastobot.controller.LancamentoController;
import br.edu.ifpr.gastobot.dto.ConsultarLancamentosRequest;
import br.edu.ifpr.gastobot.model.*;
import br.edu.ifpr.gastobot.repository.LancamentoRepository;

class ConsultaLancamentosTest {
    private final LancamentoRepository repository = mock(LancamentoRepository.class);
    private final LancamentoService service = new LancamentoService(repository);
    private final LocalDate data = LocalDate.of(2026, 11, 22);

    @Test
    void paginaUsaUsuarioNormalizadoELookaheadSemExporRegistroExtra() {
        var registros = IntStream.range(0, 11).mapToObj(i -> new Lancamento(UUID.randomUUID(), "u", TipoLancamento.SAIDA,
                BigDecimal.ONE, FormaPagamento.PIX, "registro", data)).toList();
        when(repository.listarPorPeriodo("u", data, data, 11, 10)).thenReturn(registros);
        var result = new LancamentoController(service).consultar(new ConsultarLancamentosRequest(" u ", data, data, 2));
        assertEquals(10, result.lancamentos().size());
        assertTrue(result.temProximaPagina());
        assertEquals(2, result.pagina());
        assertEquals(registros.getFirst().id(), result.lancamentos().getFirst().id());
        verify(repository).listarPorPeriodo("u", data, data, 11, 10);
    }

    @Test
    void consultaVaziaRetornaListaVaziaSemErro() {
        when(repository.listarPorPeriodo("u", data, data, 11, 0)).thenReturn(List.of());
        var result = new LancamentoController(service).consultar(new ConsultarLancamentosRequest("u", data, data, 1));
        assertTrue(result.lancamentos().isEmpty());
        assertFalse(result.temProximaPagina());
    }

    @Test
    void parametrosInvalidosNaoConsultamBanco() {
        assertThrows(NullPointerException.class, () -> service.consultar(null));
        for (var request : List.of(new ConsultarLancamentosRequest(null, data, data, 1),
                new ConsultarLancamentosRequest(" ", data, data, 1),
                new ConsultarLancamentosRequest("u", null, data, 1),
                new ConsultarLancamentosRequest("u", data, null, 1),
                new ConsultarLancamentosRequest("u", data, data.minusDays(1), 1),
                new ConsultarLancamentosRequest("u", data, data, 0),
                new ConsultarLancamentosRequest("u", data, data, Integer.MAX_VALUE))) {
            assertThrows(IllegalArgumentException.class, () -> service.consultar(request));
        }
        verifyNoInteractions(repository);
    }
}
