
package ar.org.cmm.ordenes;

import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.PracticaConsulta;
import ar.org.cmm.contratos.PrestadorConsulta;
import ar.org.cmm.facturacion.CargoCuotaRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditoriaMedicaIntegracionTest {

    @Autowired
    private OrdenService ordenService;

    @Autowired
    private AuditoriaMedicaService auditoriaService;

    @Autowired
    private AuditoriaMedicaRepository auditoriaRepository;

    @Autowired
    private OrdenEstadoHistorialRepository historialRepository;

    @Autowired
    private CargoCuotaRepository cargoRepository;

    @Autowired
    private AfiliadoConsulta afiliados;

    @Autowired
    private PrestadorConsulta prestadores;

    @Autowired
    private PracticaConsulta practicas;

    private OrdenDtos.OrdenResponse emitirPendiente() {

        var afiliado = afiliados
                .porNroAfiliado("131090000100")
                .orElseThrow();

        var prestador = prestadores.todos().stream()
                .filter(p -> p.nombre().contains("Santa Rosa"))
                .findFirst()
                .orElseThrow();

        // MED001 requiere auditoria medica.
        var practica = practicas
                .porCodigoCmm("MED001")
                .orElseThrow();

        return ordenService.emitir(
                new OrdenDtos.EmitirOrdenRequest(
                        afiliado.id(),
                        prestador.id(),
                        List.of(
                                new OrdenDtos.ItemRequest(
                                        practica.id(), 1
                                )
                        )
                )
        );
    }

    @Test
    void debeAutorizarOrdenPendiente() {

        var orden = emitirPendiente();

        assertEquals("PENDIENTE_AUDITORIA", orden.estado());

        assertTrue(
                auditoriaService.pendientes().stream()
                        .anyMatch(o -> o.id().equals(orden.id()))
        );

        var autorizada = auditoriaService.decidir(
                orden.id(),
                "AUTORIZADA",
                "Practica aprobada"
        );

        assertEquals("AUTORIZADA", autorizada.estado());

        var auditorias = auditoriaRepository
                .findByOrdenIdOrderByFechaDesc(orden.id());

        assertEquals(1, auditorias.size());
        assertEquals("AUTORIZADA", auditorias.get(0).getDecision());

        var historial = historialRepository
                .findByOrdenIdOrderByFechaAsc(orden.id());

        assertEquals(2, historial.size());
        assertEquals("PENDIENTE_AUDITORIA",
                historial.get(0).getEstadoNuevo());
        assertEquals("AUTORIZADA",
                historial.get(1).getEstadoNuevo());

        assertFalse(
                auditoriaService.pendientes().stream()
                        .anyMatch(o -> o.id().equals(orden.id()))
        );
    }

    @Test
    void debeRechazarOrdenSinGenerarCoseguro() {

        var orden = emitirPendiente();

        assertEquals("PENDIENTE_AUDITORIA", orden.estado());

        var rechazada = auditoriaService.decidir(
                orden.id(),
                "RECHAZADA",
                "Practica no autorizada"
        );

        assertEquals("RECHAZADA", rechazada.estado());

        var auditorias = auditoriaRepository
                .findByOrdenIdOrderByFechaDesc(orden.id());

        assertEquals(1, auditorias.size());
        assertEquals("RECHAZADA", auditorias.get(0).getDecision());

        var historial = historialRepository
                .findByOrdenIdOrderByFechaAsc(orden.id());

        assertEquals(2, historial.size());
        assertEquals("RECHAZADA",
                historial.get(1).getEstadoNuevo());

        // Una orden pendiente no genera coseguro: al rechazarla no queda ningún cargo en F3.
        assertTrue(
                cargoRepository
                        .findByOrigenAndOrigenRef("ORDEN", orden.numero())
                        .isEmpty()
        );
    }

    @Test
    void noDebeAuditarDosVecesLaMismaOrden() {

        var orden = emitirPendiente();

        auditoriaService.decidir(
                orden.id(),
                "AUTORIZADA",
                "Aprobada"
        );

        assertThrows(
                ar.org.cmm.comun.ReglaNegocioException.class,
                () -> auditoriaService.decidir(
                        orden.id(),
                        "RECHAZADA",
                        "Segundo intento"
                )
        );
    }
}
