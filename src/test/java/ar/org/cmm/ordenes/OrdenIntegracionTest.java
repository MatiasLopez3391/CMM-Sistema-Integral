
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

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrdenIntegracionTest {

    @Autowired
    private OrdenService ordenService;

    @Autowired
    private OrdenRepository ordenRepository;

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

    @Test
    void emitirOrdenDebeGuardarOrdenYCoseguroEnF3() {

        // 1. Buscar los datos reales de demostracion.
        var afiliado = afiliados
                .porNroAfiliado("131090000100")
                .orElseThrow();

        var prestador = prestadores.todos().stream()
                .filter(p -> p.nombre().contains("Santa Rosa"))
                .findFirst()
                .orElseThrow();

        var practica = practicas
                .porCodigoCmm("420101")
                .orElseThrow();

        // 2. Preparar una orden con una consulta.
        var request = new OrdenDtos.EmitirOrdenRequest(
                afiliado.id(),
                prestador.id(),
                List.of(new OrdenDtos.ItemRequest(
                        practica.id(),
                        1
                ))
        );

        // 3. Emitir la orden usando los servicios reales.
        var respuesta = ordenService.emitir(request);

        // 4. Verificar que la orden fue creada.
        assertNotNull(respuesta.id());
        assertNotNull(respuesta.numero());
        assertEquals("EMITIDA", respuesta.estado());

        var orden = ordenRepository
                .findByNumero(respuesta.numero())
                .orElseThrow();

        assertEquals(afiliado.id(), orden.getAfiliadoId());
        assertEquals(prestador.id(), orden.getPrestadorId());
        assertEquals(1, orden.getItems().size());

        // 5. Comprobar el valor convenido vigente.
        assertEquals(
                0,
                new BigDecimal("22911.24")
                        .compareTo(orden.getTotalValorConvenido())
        );

        // 6. Verificar que el coseguro fue calculado.
        assertNotNull(orden.getTotalCoseguro());
        assertTrue(
                orden.getTotalCoseguro().compareTo(BigDecimal.ZERO) >= 0
        );

        // 7. Verificar el historial inicial.
        var historial = historialRepository
                .findByOrdenIdOrderByFechaAsc(orden.getId());

        assertEquals(1, historial.size());
        assertEquals("EMITIDA", historial.get(0).getEstadoNuevo());

        // 8. Verificar que F3 registro el coseguro.
        assertTrue(
        orden.getTotalCoseguro().signum() > 0,
        "La orden debe generar un coseguro positivo para verificar F3"
        );
        if (orden.getTotalCoseguro().signum() > 0) {

            var cargo = cargoRepository
                    .findByOrigenAndOrigenRef(
                            "ORDEN",
                            orden.getNumero()
                    )
                    .orElseThrow();

            assertEquals(
                    0,
                    orden.getTotalCoseguro()
                            .compareTo(cargo.getImporte())
            );
        }
    }
}
