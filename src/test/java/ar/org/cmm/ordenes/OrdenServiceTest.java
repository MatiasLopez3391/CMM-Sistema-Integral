
package ar.org.cmm.ordenes;

import ar.org.cmm.comun.NumeradorService;
import ar.org.cmm.comun.UsuarioActualProvider;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.BloqueoServicio;
import ar.org.cmm.contratos.CargoCuotaServicio;
import ar.org.cmm.contratos.CoseguroServicio;
import ar.org.cmm.contratos.ParametroConsulta;
import ar.org.cmm.contratos.PracticaConsulta;
import ar.org.cmm.contratos.PrestadorConsulta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenServiceTest {

    @Mock
    private OrdenRepository ordenRepository;

    @Mock
    private OrdenEstadoHistorialRepository historialRepository;

    @Mock
    private AfiliadoConsulta afiliados;

    @Mock
    private BloqueoServicio bloqueos;

    @Mock
    private PrestadorConsulta prestadores;

    @Mock
    private PracticaConsulta practicas;

    @Mock
    private CoseguroServicio coseguros;

    @Mock
    private CargoCuotaServicio cargos;

    @Mock
    private NumeradorService numerador;

    @Mock
    private ParametroConsulta parametros;

    @Mock
    private UsuarioActualProvider usuarioActual;

    @InjectMocks
    private OrdenService ordenService;

    @BeforeEach
    void preparar() {
        lenient().when(afiliados.obtener(1L)).thenReturn(
            new AfiliadoConsulta.AfiliadoResumen(
                1L, "AF-001", "Afiliado Prueba",
                "12345678", "test@correo.com",
                null, 10L, "PLAN", "Plan Prueba",
                "ACTIVO", false
            )
        );

        lenient().when(prestadores.obtener(2L)).thenReturn(
            new PrestadorConsulta.PrestadorResumen(
                2L, "Prestador Prueba", "20123456789",
                "MEDICO", true
            )
        );

        // Parámetros con su valor por defecto (tope 3 órdenes por mes, vencimiento a 1 mes)
        lenient().when(parametros.entero(anyString(), anyInt()))
                .thenAnswer(i -> i.getArgument(1));

        lenient().when(usuarioActual.usuario())
            .thenReturn("daniela");

        lenient().when(numerador.siguiente("ORDEN"))
            .thenReturn(1L);

        lenient().when(ordenRepository.saveAndFlush(any(Orden.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private OrdenDtos.EmitirOrdenRequest solicitud() {
        return new OrdenDtos.EmitirOrdenRequest(
            1L,
            2L,
            List.of(new OrdenDtos.ItemRequest(3L, 2))
        );
    }

    private void prepararPractica(boolean requiereAuditoria) {
        when(practicas.obtener(3L)).thenReturn(
            new PracticaConsulta.PracticaResumen(
                3L, "P001", "N001",
                "Consulta médica", "CONSULTAS",
                requiereAuditoria
            )
        );

        when(coseguros.calcular(
                eq(2L), eq(3L), eq(10L), any(LocalDate.class)
        )).thenReturn(
            new CoseguroServicio.CalculoCoseguro(
                2L, 3L, 10L,
                "PLAN", "Plan Prueba", "CONSULTAS",
                LocalDate.now(),
                new BigDecimal("10000.00"),
                new BigDecimal("80.00"),
                new BigDecimal("8000.00"),
                new BigDecimal("2000.00")
            )
        );
    }

    @Test
    void emitirOrdenCalculaCoseguroYRegistraCargo() {
        prepararPractica(false);

        OrdenDtos.OrdenResponse respuesta =
            ordenService.emitir(solicitud());

        assertEquals("ORD-00000001", respuesta.numero());
        assertEquals("EMITIDA", respuesta.estado());

        assertEquals(
            0,
            new BigDecimal("20000.00")
                .compareTo(respuesta.totalValorConvenido())
        );

        assertEquals(
            0,
            new BigDecimal("16000.00")
                .compareTo(respuesta.totalCargoEntidad())
        );

        assertEquals(
            0,
            new BigDecimal("4000.00")
                .compareTo(respuesta.totalCoseguro())
        );

        ArgumentCaptor<CargoCuotaServicio.CoseguroParaCuota> captor =
            ArgumentCaptor.forClass(
                CargoCuotaServicio.CoseguroParaCuota.class
            );

        verify(cargos).registrarCoseguro(captor.capture());

        assertEquals(
            "ORD-00000001",
            captor.getValue().origenRef()
        );

        assertEquals(
            0,
            new BigDecimal("4000.00")
                .compareTo(captor.getValue().importe())
        );

        verify(ordenRepository).saveAndFlush(any(Orden.class));
        verify(historialRepository).save(any(OrdenEstadoHistorial.class));
    }

    @Test
    void afiliadoBloqueadoNoPuedeEmitirOrden() {
        when(bloqueos.estaBloqueado(1L))
            .thenReturn(true);

        assertThrows(
            ar.org.cmm.comun.ReglaNegocioException.class,
            () -> ordenService.emitir(solicitud())
        );

        verify(ordenRepository, never())
            .saveAndFlush(any(Orden.class));

        verifyNoInteractions(cargos);
    }

    @Test
    void practicaConAuditoriaQuedaPendiente() {
        prepararPractica(true);

        OrdenDtos.OrdenResponse respuesta =
            ordenService.emitir(solicitud());

        assertEquals(
            "PENDIENTE_AUDITORIA",
            respuesta.estado()
        );

        ArgumentCaptor<Orden> captor =
            ArgumentCaptor.forClass(Orden.class);

        verify(ordenRepository)
            .saveAndFlush(captor.capture());

        assertTrue(
            captor.getValue()
                .getItems()
                .get(0)
                .isRequiereAuditoria()
        );
    }
}
