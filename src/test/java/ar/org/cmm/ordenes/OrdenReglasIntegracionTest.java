package ar.org.cmm.ordenes;

import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.PracticaConsulta;
import ar.org.cmm.contratos.PrestadorConsulta;
import ar.org.cmm.facturacion.CargoCuota;
import ar.org.cmm.facturacion.CargoCuotaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas de la orden y del buzón del auditor: coseguro al quedar válida, autorización parcial, tope, anulación. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrdenReglasIntegracionTest {

    @Autowired private OrdenService ordenes;
    @Autowired private AuditoriaMedicaService auditoria;
    @Autowired private OrdenConsultaService consultas;
    @Autowired private OrdenRepository ordenRepository;
    @Autowired private CargoCuotaRepository cargos;
    @Autowired private AfiliadoConsulta afiliados;
    @Autowired private PrestadorConsulta prestadores;
    @Autowired private PracticaConsulta practicas;

    private Long afiliado;
    private Long santaRosa;
    private Long consulta;
    private Long medicacion;

    @BeforeEach
    void preparar() {
        afiliado = afiliados.porNroAfiliado("131090000200").orElseThrow().id(); // FERNÁNDEZ, plan B1 70/100
        santaRosa = prestadores.todos().stream().filter(p -> p.nombre().contains("Santa Rosa")).findFirst().orElseThrow().id();
        consulta = practicas.porCodigoCmm("420101").orElseThrow().id();
        medicacion = practicas.porCodigoCmm("MED001").orElseThrow().id();
    }

    private OrdenDtos.OrdenResponse emitir(Long practica, int cantidad) {
        return ordenes.emitir(new OrdenDtos.EmitirOrdenRequest(afiliado, santaRosa,
                List.of(new OrdenDtos.ItemRequest(practica, cantidad))));
    }

    private CargoCuota cargo(String numero) {
        return cargos.findByOrigenAndOrigenRef("ORDEN", numero).orElseThrow();
    }

    @Test
    void ordenSinAuditoriaRegistraElCoseguroYVenceAlMes() {
        var o = emitir(consulta, 1);

        assertThat(o.estado()).isEqualTo("EMITIDA");
        assertThat(cargo(o.numero()).getImporte()).isEqualByComparingTo(o.totalCoseguro());
        assertThat(cargo(o.numero()).getConcepto()).contains("Consulta").contains("Santa Rosa").contains(o.numero());
        assertThat(ordenRepository.findById(o.id()).orElseThrow().getFechaVencimiento())
                .isEqualTo(LocalDate.now().plusMonths(1));
    }

    @Test
    void pendienteNoGeneraCoseguroHastaQueSeAutoriza() {
        var o = emitir(medicacion, 1);

        assertThat(o.estado()).isEqualTo("PENDIENTE_AUDITORIA");
        assertThat(cargos.findByOrigenAndOrigenRef("ORDEN", o.numero())).isEmpty();
        assertThat(ordenRepository.findById(o.id()).orElseThrow().getMotivoAuditoria()).contains("requiere auditoría");

        auditoria.decidir(o.id(), "AUTORIZADA", null);

        Orden autorizada = ordenRepository.findById(o.id()).orElseThrow();
        assertThat(autorizada.getEstado()).isEqualTo("AUTORIZADA");
        assertThat(autorizada.getFechaVencimiento()).isEqualTo(LocalDate.now().plusMonths(1));
        assertThat(autorizada.getFechaResolucion()).isNotNull();
        // B1 70/100: 50.000 de medicación -> 15.000 de coseguro, a la cuota del mes siguiente
        assertThat(cargo(o.numero()).getImporte()).isEqualByComparingTo("15000.00");
    }

    @Test
    void autorizacionParcialRecalculaElCoseguroYExigeMotivo() {
        var o = emitir(medicacion, 2);
        Long item = ordenRepository.findById(o.id()).orElseThrow().getItems().get(0).getId();
        var mitad = List.of(new OrdenDtos.ItemDecision(item, 1));

        assertThatThrownBy(() -> auditoria.decidir(o.id(), "AUTORIZADA", " ", mitad))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("motivo");
        assertThatThrownBy(() -> auditoria.decidir(o.id(), "AUTORIZADA", "x", List.of(new OrdenDtos.ItemDecision(item, 3))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("entre 0 y 2");
        assertThatThrownBy(() -> auditoria.decidir(o.id(), "AUTORIZADA", "x", List.of(new OrdenDtos.ItemDecision(item, 0))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("rechace");

        auditoria.decidir(o.id(), "AUTORIZADA", "Se autoriza un mes de tratamiento", mitad);

        var detalle = consultas.detalle(o.id());
        assertThat(detalle.orden().estado()).isEqualTo("AUTORIZADA");
        assertThat(detalle.items().get(0).cantidadSolicitada()).isEqualTo(2);
        assertThat(detalle.items().get(0).cantidad()).isEqualTo(1);
        assertThat(detalle.orden().totalCoseguro()).isEqualByComparingTo("15000.00");
        assertThat(detalle.auditorias().get(0).decision()).isEqualTo("AUTORIZADA_PARCIAL");
        assertThat(detalle.auditorias().get(0).comentario()).contains("2 → 1");
        assertThat(cargo(o.numero()).getImporte()).isEqualByComparingTo("15000.00");
    }

    @Test
    void rechazoExigeMotivo() {
        var o = emitir(medicacion, 1);
        assertThatThrownBy(() -> auditoria.decidir(o.id(), "RECHAZADA", null))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("motivo del rechazo");
    }

    @Test
    void laOrdenQueSuperaElTopeMensualVaAAuditoria() {
        emitir(consulta, 1);
        emitir(consulta, 1);
        emitir(consulta, 1);

        var simulada = ordenes.simular(new OrdenDtos.EmitirOrdenRequest(afiliado, santaRosa,
                List.of(new OrdenDtos.ItemRequest(consulta, 1))));
        assertThat(simulada.vaAAuditoria()).isTrue();
        assertThat(simulada.ordenesEnElMes()).isEqualTo(3);

        var cuarta = emitir(consulta, 1);
        assertThat(cuarta.estado()).isEqualTo("PENDIENTE_AUDITORIA");
        assertThat(ordenRepository.findById(cuarta.id()).orElseThrow().getMotivoAuditoria())
                .contains("upera el tope de 3 órdenes");
        assertThat(cargos.findByOrigenAndOrigenRef("ORDEN", cuarta.numero())).isEmpty();
    }

    @Test
    void elMedicoPuedePedirAuditoriaConMotivo() {
        var sinMotivo = new OrdenDtos.EmitirOrdenRequest(afiliado, santaRosa,
                List.of(new OrdenDtos.ItemRequest(consulta, 1)), "Dr. Pérez", null, true, " ");
        assertThatThrownBy(() -> ordenes.emitir(sinMotivo)).isInstanceOf(ReglaNegocioException.class);

        var o = ordenes.emitir(new OrdenDtos.EmitirOrdenRequest(afiliado, santaRosa,
                List.of(new OrdenDtos.ItemRequest(consulta, 1)), "Dr. Pérez", "Control", true, "Dudas sobre la indicación"));
        Orden guardada = ordenRepository.findById(o.id()).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo("PENDIENTE_AUDITORIA");
        assertThat(guardada.getMotivoAuditoria()).isEqualTo("Pedido del médico: Dudas sobre la indicación");
        assertThat(guardada.getMedicoSolicitante()).isEqualTo("Dr. Pérez");
    }

    @Test
    void anularUnaOrdenAnulaSuCoseguro() {
        var o = emitir(consulta, 1);

        var r = ordenes.anular(o.id(), "El afiliado no la va a usar");

        assertThat(r.aviso()).isNull();
        assertThat(ordenRepository.findById(o.id()).orElseThrow().getEstado()).isEqualTo("ANULADA");
        assertThat(cargo(o.numero()).getEstado()).isEqualTo(CargoCuota.ANULADO);
        assertThatThrownBy(() -> ordenes.anular(o.id(), "otra vez"))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("no se puede anular");
        assertThatThrownBy(() -> ordenes.anular(o.id(), ""))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("motivo");
    }

    @Test
    void anularConElCoseguroYaFacturadoAvisaQueHaceFaltaNotaDeCredito() {
        var o = emitir(consulta, 1);
        CargoCuota c = cargo(o.numero());
        c.setEstado(CargoCuota.FACTURADO);
        cargos.saveAndFlush(c);

        var r = ordenes.anular(o.id(), "Error de carga");

        assertThat(ordenRepository.findById(o.id()).orElseThrow().getEstado()).isEqualTo("ANULADA");
        assertThat(r.aviso()).contains("ya se cobró").contains("nota de crédito");
        assertThat(cargo(o.numero()).getEstado()).isEqualTo(CargoCuota.FACTURADO);
    }

    @Test
    void mesaDeEntradasRecibeElAvisoDeLaOrdenResuelta() {
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
        var o = emitir(medicacion, 1);
        assertThat(consultas.resueltasDesde(antes)).noneMatch(a -> a.id().equals(o.id()));

        auditoria.decidir(o.id(), "RECHAZADA", "Falta el estudio previo");

        assertThat(consultas.resueltasDesde(antes))
                .anySatisfy(a -> {
                    assertThat(a.id()).isEqualTo(o.id());
                    assertThat(a.estado()).isEqualTo("RECHAZADA");
                    assertThat(a.comentario()).isEqualTo("Falta el estudio previo");
                });
    }

    @Test
    void elListadoFiltraPorAfiliadoYEstado() {
        var emitida = emitir(consulta, 1);
        var pendiente = emitir(medicacion, 1);

        assertThat(consultas.buscar(afiliado, null, null, null, null)).extracting(OrdenDtos.OrdenResumen::id)
                .contains(emitida.id(), pendiente.id());
        assertThat(consultas.buscar(afiliado, "pendiente_auditoria", null, null, null)).extracting(OrdenDtos.OrdenResumen::id)
                .contains(pendiente.id()).doesNotContain(emitida.id());
        assertThat(consultas.buscar(null, null, emitida.numero(), null, null)).singleElement()
                .satisfies(r -> assertThat(r.afiliado()).contains("FERNÁNDEZ"));
        assertThat(consultas.detalle(pendiente.id()).items().get(0).coseguro()).isEqualByComparingTo(new BigDecimal("15000.00"));
    }
}
