
package ar.org.cmm.ordenes;

import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.ConsumosConsulta;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConsumosInformeTest {

    @Autowired
    private OrdenRepository ordenes;

    @Autowired
    private AfiliadoConsulta afiliados;

    @Autowired
    private ConsumosConsulta consumos;

    @Autowired
    private InformeCosegurosService informes;

    private Orden crearOrden(
            Long afiliadoId,
            String estado,
            BigDecimal coseguro) {

        Orden orden = new Orden();

        orden.setNumero(
                "T32-" + UUID.randomUUID()
                        .toString().substring(0, 12)
        );

        orden.setAfiliadoId(afiliadoId);
        orden.setPrestadorId(1L);
        orden.setPlanId(1L);
        orden.setFechaEmision(LocalDate.of(2026, 11, 15));
        orden.setEstado(estado);
        orden.setEmitidaPor("TEST");

        orden.setTotalValorConvenido(
                new BigDecimal("10000.00")
        );

        orden.setTotalCargoEntidad(
                new BigDecimal("8000.00")
        );

        orden.setTotalCoseguro(coseguro);

        return ordenes.saveAndFlush(orden);
    }

    @Test
    void consumosSoloIncluyenOrdenesValidas() {

        Long afiliadoId = afiliados
                .porNroAfiliado("131090000100")
                .orElseThrow()
                .id();

        crearOrden(
                afiliadoId,
                "EMITIDA",
                new BigDecimal("2000.00")
        );

        crearOrden(
                afiliadoId,
                "AUTORIZADA",
                new BigDecimal("2000.00")
        );

        crearOrden(
                afiliadoId,
                "RECHAZADA",
                new BigDecimal("2000.00")
        );

        crearOrden(
                afiliadoId,
                "PENDIENTE_AUDITORIA",
                new BigDecimal("2000.00")
        );

        List<ConsumosConsulta.ConsumoResumen> resultado =
                consumos.obtenerPorAfiliado(afiliadoId);

        assertThat(resultado).hasSize(2);

        assertThat(resultado)
                .extracting(
                        ConsumosConsulta.ConsumoResumen::estado
                )
                .containsExactlyInAnyOrder(
                        "EMITIDA",
                        "AUTORIZADA"
                );
    }

    @Test
    void informeCalculaTotalesCorrectamente() {

        Long afiliadoId = afiliados
                .porNroAfiliado("131090000101")
                .orElseThrow()
                .id();

        crearOrden(
                afiliadoId,
                "EMITIDA",
                new BigDecimal("2000.00")
        );

        crearOrden(
                afiliadoId,
                "AUTORIZADA",
                new BigDecimal("3000.00")
        );

        crearOrden(
                afiliadoId,
                "RECHAZADA",
                new BigDecimal("4000.00")
        );

        var informe = informes.generar(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30),
                afiliadoId
        );

        assertThat(informe.cantidadOrdenes())
                .isEqualTo(2);

        assertThat(informe.totalValorConvenido())
                .isEqualByComparingTo("20000.00");

        assertThat(informe.totalCargoEntidad())
                .isEqualByComparingTo("16000.00");

        assertThat(informe.totalCoseguro())
                .isEqualByComparingTo("5000.00");
    }

    @Test
    void informeRechazaFechasInvalidas() {

        assertThatThrownBy(() -> informes.generar(
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 11, 1),
                null
        ))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
