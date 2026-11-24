
package ar.org.cmm.ordenes;

import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.PracticaConsulta;
import ar.org.cmm.contratos.PrestadorConsulta;
import ar.org.cmm.facturacion.CargoCuota;
import ar.org.cmm.facturacion.CargoCuotaRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConsumosFacturacionIntegracionTest {

    @Autowired
    private OrdenService ordenService;

    @Autowired
    private OrdenRepository ordenes;

    @Autowired
    private InformeCosegurosService informes;

    @Autowired
    private CargoCuotaRepository cargos;

    @Autowired
    private AfiliadoConsulta afiliados;

    @Autowired
    private PrestadorConsulta prestadores;

    @Autowired
    private PracticaConsulta practicas;

    @Test
    void coseguroDeOrdenCoincideConCargoDeFacturacion() {

        var afiliado = afiliados
                .porNroAfiliado("131090000100")
                .orElseThrow();

        var prestador = prestadores.todos()
                .stream()
                .filter(p -> p.nombre().contains("Santa Rosa"))
                .findFirst()
                .orElseThrow();

        var practica = practicas
                .porCodigoCmm("420101")
                .orElseThrow();

        var request = new OrdenDtos.EmitirOrdenRequest(
                afiliado.id(),
                prestador.id(),
                List.of(
                        new OrdenDtos.ItemRequest(
                                practica.id(), 1
                        )
                )
        );

        var respuesta = ordenService.emitir(request);

        var orden = ordenes
                .findByNumero(respuesta.numero())
                .orElseThrow();

        var cargo = cargos.findByOrigenAndOrigenRef(
                CargoCuota.ORIGEN_ORDEN,
                orden.getNumero()
        ).orElseThrow();

        // El coseguro calculado por F5 debe coincidir con F3.
        assertThat(cargo.getImporte())
                .isEqualByComparingTo(
                        orden.getTotalCoseguro()
                );

        assertThat(cargo.getAfiliadoId())
                .isEqualTo(afiliado.id());

        assertThat(cargo.getTipo())
                .isEqualTo(CargoCuota.TIPO_COSEGURO);

        assertThat(cargo.getEstado())
                .isEqualTo(CargoCuota.PENDIENTE);

        // El informe de F5 debe reflejar la orden emitida.
        LocalDate fecha = orden.getFechaEmision();

        var informe = informes.generar(
                fecha,
                fecha,
                afiliado.id()
        );

        assertThat(informe.detalle())
                .anySatisfy(detalle -> {

                    assertThat(detalle.numero())
                            .isEqualTo(orden.getNumero());

                    assertThat(detalle.coseguro())
                            .isEqualByComparingTo(
                                    cargo.getImporte()
                            );
                });
    }
}
