
package ar.org.cmm.ordenes;

import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.PracticaConsulta;
import ar.org.cmm.contratos.PrestadorConsulta;
import ar.org.cmm.facturacion.Notificacion;
import ar.org.cmm.facturacion.NotificacionRepository;

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
class OrdenNotificacionIntegracionTest {

    @Autowired
    private OrdenService ordenService;

    @Autowired
    private AuditoriaMedicaService auditoriaService;

    @Autowired
    private OrdenConsultaController consultaController;

    @Autowired
    private NotificacionRepository notificaciones;

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
    void debeRegistrarNotificacionAlAutorizar() {

        var orden = emitirPendiente();

        auditoriaService.decidir(
                orden.id(),
                "AUTORIZADA",
                "Práctica autorizada"
        );

        List<Notificacion> registros = notificaciones
                .findTop200ByOrderByFechaCreacionDesc();

        var notificacion = registros.stream()
                .filter(n ->
                        "AUDITORIA_ORDEN".equals(n.getTipo())
                        && orden.afiliadoId().equals(n.getAfiliadoId())
                        && n.getAsunto().contains(orden.numero())
                )
                .findFirst()
                .orElseThrow();

        assertTrue(
                List.of("SIN_SMTP", "SIN_EMAIL", "ENVIADA", "ERROR")
                        .contains(notificacion.getEstado())
        );

        assertTrue(
                notificacion.getCuerpo().contains("AUTORIZADA")
        );
    }

    @Test
    void debeRegistrarNotificacionAlRechazar() {

        var orden = emitirPendiente();

        auditoriaService.decidir(
                orden.id(),
                "RECHAZADA",
                "Práctica rechazada"
        );

        var notificacion = notificaciones
                .findTop200ByOrderByFechaCreacionDesc()
                .stream()
                .filter(n ->
                        "AUDITORIA_ORDEN".equals(n.getTipo())
                        && n.getAsunto().contains(orden.numero())
                )
                .findFirst()
                .orElseThrow();

        assertTrue(
                notificacion.getCuerpo().contains("RECHAZADA")
        );
    }

    @Test
    void debeConsultarHistorialCompleto() {

        var orden = emitirPendiente();

        auditoriaService.decidir(
                orden.id(),
                "AUTORIZADA",
                "Auditoría aprobada"
        );

        var historial = consultaController.historial(orden.id());

        assertEquals(2, historial.size());

        assertEquals(
                "PENDIENTE_AUDITORIA",
                historial.get(0).estadoNuevo()
        );

        assertEquals(
                "AUTORIZADA",
                historial.get(1).estadoNuevo()
        );

        assertEquals(
                "PENDIENTE_AUDITORIA",
                historial.get(1).estadoAnterior()
        );

        assertEquals(
                "Auditoría aprobada",
                historial.get(1).comentario()
        );
    }

    @Test
    void noDebeConsultarHistorialDeOrdenInexistente() {

        assertThrows(
                IllegalArgumentException.class,
                () -> consultaController.historial(-999L)
        );
    }
}
