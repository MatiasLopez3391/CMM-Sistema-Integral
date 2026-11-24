

package ar.org.cmm.ordenes;

import ar.org.cmm.afiliados.AfiliadoRepository;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.BloqueoServicio;
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

    @Autowired
    private BloqueoServicio bloqueos;

    @Autowired
    private AfiliadoRepository afiliadoRepository;

    // T27: Emision de orden y registro del coseguro en F3
    @Test
    void emitirOrdenDebeGuardarOrdenYCoseguroEnF3() {

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

        var request = new OrdenDtos.EmitirOrdenRequest(
                afiliado.id(),
                prestador.id(),
                List.of(new OrdenDtos.ItemRequest(practica.id(), 1))
        );

        var respuesta = ordenService.emitir(request);

        assertNotNull(respuesta.id());
        assertNotNull(respuesta.numero());
        assertEquals("EMITIDA", respuesta.estado());

        var orden = ordenRepository
                .findByNumero(respuesta.numero())
                .orElseThrow();

        assertEquals(afiliado.id(), orden.getAfiliadoId());
        assertEquals(prestador.id(), orden.getPrestadorId());
        assertEquals(1, orden.getItems().size());

        assertEquals(
                0,
                new BigDecimal("22911.24")
                        .compareTo(orden.getTotalValorConvenido())
        );

        assertNotNull(orden.getTotalCoseguro());

        assertTrue(
                orden.getTotalCoseguro().signum() > 0,
                "La orden debe generar un coseguro positivo para verificar F3"
        );

        var historial = historialRepository
                .findByOrdenIdOrderByFechaAsc(orden.getId());

        assertEquals(1, historial.size());
        assertEquals("EMITIDA", historial.get(0).getEstadoNuevo());

        var cargo = cargoRepository
                .findByOrigenAndOrigenRef("ORDEN", orden.getNumero())
                .orElseThrow();

        assertEquals(
                0,
                orden.getTotalCoseguro().compareTo(cargo.getImporte())
        );
    }

    // T29: Bloquear al titular afecta a todo el grupo
    @Test
    void bloqueoDelTitularDebeAfectarATodaLaFamilia() {

        var titular = afiliados
                .porNroAfiliado("131090000100")
                .orElseThrow();

        var conyuge = afiliados
                .porNroAfiliado("131090000101")
                .orElseThrow();

        var hijo = afiliados
                .porNroAfiliado("131090000102")
                .orElseThrow();

        var entidad = afiliadoRepository
                .findById(titular.id())
                .orElseThrow();

        entidad.setBloqueado(true);
        afiliadoRepository.saveAndFlush(entidad);

        assertTrue(bloqueos.estaBloqueado(titular.id()));
        assertTrue(bloqueos.estaBloqueado(conyuge.id()));
        assertTrue(bloqueos.estaBloqueado(hijo.id()));
    }

    // T29: No permitir ordenes para familiares bloqueados
    @Test
    void noDebeEmitirOrdenParaFamiliarBloqueado() {

        var titular = afiliados
                .porNroAfiliado("131090000100")
                .orElseThrow();

        var hijo = afiliados
                .porNroAfiliado("131090000102")
                .orElseThrow();

        var prestador = prestadores.todos().stream()
                .filter(p -> p.nombre().contains("Santa Rosa"))
                .findFirst()
                .orElseThrow();

        var practica = practicas
                .porCodigoCmm("420101")
                .orElseThrow();

        var entidad = afiliadoRepository
                .findById(titular.id())
                .orElseThrow();

        entidad.setBloqueado(true);
        afiliadoRepository.saveAndFlush(entidad);

        var request = new OrdenDtos.EmitirOrdenRequest(
                hijo.id(),
                prestador.id(),
                List.of(new OrdenDtos.ItemRequest(practica.id(), 1))
        );

        var error = assertThrows(
                ar.org.cmm.comun.ReglaNegocioException.class,
                () -> ordenService.emitir(request)
        );

        assertTrue(
                error.getMessage().contains("está bloqueado")
        );
    }
}
