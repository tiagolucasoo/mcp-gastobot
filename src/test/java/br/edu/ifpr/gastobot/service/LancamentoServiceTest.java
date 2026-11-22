package br.edu.ifpr.gastobot.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.gastobot.controller.LancamentoController;
import br.edu.ifpr.gastobot.dto.CriarLancamentoRequest;
import br.edu.ifpr.gastobot.dto.LancamentoResponse;
import br.edu.ifpr.gastobot.model.FormaPagamento;
import br.edu.ifpr.gastobot.model.Lancamento;
import br.edu.ifpr.gastobot.model.TipoLancamento;
import br.edu.ifpr.gastobot.repository.LancamentoRepository;

class LancamentoServiceTest {
    private final LancamentoRepository repository = mock(LancamentoRepository.class);
    private final LancamentoService service = new LancamentoService(repository);

    private CriarLancamentoRequest request(String valor, TipoLancamento tipo) {
        return new CriarLancamentoRequest(" 123 ", tipo, new BigDecimal(valor),
                FormaPagamento.PIX, " Almoço ", LocalDate.of(2026, 11, 21));
    }

    @ParameterizedTest
    @EnumSource(TipoLancamento.class)
    void registraEntradaESaidaComValorPositivo(TipoLancamento tipo) {
        Lancamento lancamento = service.registrar(request("35.90", tipo));
        assertNotNull(lancamento.id());
        assertEquals("123", lancamento.usuarioId());
        assertEquals(tipo, lancamento.tipo());
        assertEquals(new BigDecimal("35.90"), lancamento.valor());
        assertEquals("Almoço", lancamento.descricao());
        verify(repository).salvar(lancamento);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.001", "1.999", "92233720368547758.08"})
    void rejeitaValoresInvalidosSemPersistir(String valor) {
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar(request(valor, TipoLancamento.SAIDA)));
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "1.2", "1.230"})
    void normalizaValorSemArredondamento(String valor) {
        Lancamento lancamento = service.registrar(request(valor, TipoLancamento.ENTRADA));
        assertEquals(2, lancamento.valor().scale());
        assertEquals(0, new BigDecimal(valor).compareTo(lancamento.valor()));
    }

    @Test
    void permiteDescricaoOpcional() {
        var request = new CriarLancamentoRequest("123", TipoLancamento.ENTRADA,
                BigDecimal.ONE, FormaPagamento.DINHEIRO, null, LocalDate.now());
        assertEquals("", service.registrar(request).descricao());
    }

    @Test
    void rejeitaCamposObrigatoriosAusentes() {
        assertThrows(NullPointerException.class, () -> service.registrar(null));
        var valid = request("10", TipoLancamento.ENTRADA);
        for (String usuario : new String[]{null, "", " "}) {
            assertThrows(IllegalArgumentException.class, () -> service.registrar(
                    new CriarLancamentoRequest(usuario, valid.tipo(), valid.valor(),
                            valid.formaPagamento(), valid.descricao(), valid.data())));
        }
        assertThrows(NullPointerException.class, () -> service.registrar(
                new CriarLancamentoRequest("123", null, valid.valor(),
                        valid.formaPagamento(), null, valid.data())));
        assertThrows(NullPointerException.class, () -> service.registrar(
                new CriarLancamentoRequest("123", valid.tipo(), null,
                        valid.formaPagamento(), null, valid.data())));
        assertThrows(NullPointerException.class, () -> service.registrar(
                new CriarLancamentoRequest("123", valid.tipo(), valid.valor(),
                        null, null, valid.data())));
        assertThrows(NullPointerException.class, () -> service.registrar(
                new CriarLancamentoRequest("123", valid.tipo(), valid.valor(),
                        valid.formaPagamento(), null, null)));
        verifyNoInteractions(repository);
    }

    @Test
    void propagaFalhaDePersistencia() {
        var failure = new IllegalStateException("Banco indisponível");
        doThrow(failure).when(repository).salvar(any(Lancamento.class));
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.registrar(request("10", TipoLancamento.ENTRADA))));
    }

    @Test
    void controllerRetornaDadosDoLancamentoPersistido() {
        LancamentoController controller = new LancamentoController(service);
        LancamentoResponse response = controller.registrar(request("35.90", TipoLancamento.SAIDA));
        var captor = org.mockito.ArgumentCaptor.forClass(Lancamento.class);
        verify(repository).salvar(captor.capture());
        Lancamento saved = captor.getValue();
        assertEquals(saved.id(), response.id());
        assertEquals(saved.usuarioId(), response.usuarioId());
        assertEquals(saved.tipo(), response.tipo());
        assertEquals(saved.valor(), response.valor());
        assertEquals(saved.formaPagamento(), response.formaPagamento());
        assertEquals(saved.descricao(), response.descricao());
        assertEquals(saved.data(), response.data());
    }

    @Test
    void buscaUsaUsuarioNormalizadoERetornaAusencia() {
        var id = java.util.UUID.randomUUID();
        when(repository.buscarPorId(id, "123")).thenReturn(java.util.Optional.empty());
        assertTrue(service.buscarPorId(id, " 123 ").isEmpty());
        verify(repository).buscarPorId(id, "123");
    }

    @Test
    void buscaRejeitaIdOuUsuarioAusenteSemConsultarBanco() {
        var id = java.util.UUID.randomUUID();
        assertThrows(NullPointerException.class, () -> service.buscarPorId(null, "123"));
        for (String usuario : new String[]{null, "", " "}) {
            assertThrows(IllegalArgumentException.class, () -> service.buscarPorId(id, usuario));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void controllerMapeiaResultadoDaBusca() {
        var original = service.registrar(request("35.90", TipoLancamento.SAIDA));
        when(repository.buscarPorId(original.id(), "123")).thenReturn(java.util.Optional.of(original));
        var response = new LancamentoController(service).buscarPorId(original.id(), "123").orElseThrow();
        assertEquals(original.id(), response.id());
        assertEquals(original.valor(), response.valor());
        assertEquals(original.usuarioId(), response.usuarioId());
    }
}
