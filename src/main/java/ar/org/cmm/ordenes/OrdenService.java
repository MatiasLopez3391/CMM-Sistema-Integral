
package ar.org.cmm.ordenes;

import ar.org.cmm.comun.NumeradorService;
import ar.org.cmm.comun.UsuarioActualProvider;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.BloqueoServicio;
import ar.org.cmm.contratos.CargoCuotaServicio;
import ar.org.cmm.contratos.CoseguroServicio;
import ar.org.cmm.contratos.PracticaConsulta;
import ar.org.cmm.contratos.PrestadorConsulta;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class OrdenService {

    private final OrdenRepository ordenRepository;
    private final OrdenEstadoHistorialRepository historialRepository;
    private final AfiliadoConsulta afiliados;
    private final BloqueoServicio bloqueos;
    private final PrestadorConsulta prestadores;
    private final PracticaConsulta practicas;
    private final CoseguroServicio coseguros;
    private final CargoCuotaServicio cargos;
    private final NumeradorService numerador;
    private final UsuarioActualProvider usuarioActual;

    public OrdenService(
            OrdenRepository ordenRepository,
            OrdenEstadoHistorialRepository historialRepository,
            AfiliadoConsulta afiliados,
            BloqueoServicio bloqueos,
            PrestadorConsulta prestadores,
            PracticaConsulta practicas,
            CoseguroServicio coseguros,
            CargoCuotaServicio cargos,
            NumeradorService numerador,
            UsuarioActualProvider usuarioActual) {

        this.ordenRepository = ordenRepository;
        this.historialRepository = historialRepository;
        this.afiliados = afiliados;
        this.bloqueos = bloqueos;
        this.prestadores = prestadores;
        this.practicas = practicas;
        this.coseguros = coseguros;
        this.cargos = cargos;
        this.numerador = numerador;
        this.usuarioActual = usuarioActual;
    }

    @Transactional
    public OrdenDtos.OrdenResponse emitir(
            OrdenDtos.EmitirOrdenRequest request) {

        if (request == null
                || request.afiliadoId() == null
                || request.prestadorId() == null
                || request.items() == null
                || request.items().isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe indicar afiliado, prestador y prácticas");
        }

        var afiliado = afiliados.obtener(request.afiliadoId());

        if (!"ACTIVO".equalsIgnoreCase(afiliado.estado())) {
            throw new IllegalArgumentException(
                    "El afiliado no está activo");
        }

        if (bloqueos.estaBloqueado(afiliado.id())) {
            throw new IllegalArgumentException(
                    "El grupo familiar está bloqueado");
        }

        if (afiliado.planId() == null) {
            throw new IllegalArgumentException(
                    "El afiliado no tiene un plan asignado");
        }

        var prestador = prestadores.obtener(request.prestadorId());

        if (!prestador.activo()) {
            throw new IllegalArgumentException(
                    "El prestador está inactivo");
        }

        LocalDate fecha = LocalDate.now();

        Orden orden = new Orden();
        orden.setAfiliadoId(afiliado.id());
        orden.setPrestadorId(prestador.id());
        orden.setPlanId(afiliado.planId());
        orden.setFechaEmision(fecha);
        orden.setEmitidaPor(usuarioActual.usuario());

        BigDecimal totalValor = BigDecimal.ZERO;
        BigDecimal totalEntidad = BigDecimal.ZERO;
        BigDecimal totalCoseguro = BigDecimal.ZERO;

        boolean requiereAuditoria = false;
        Set<Long> practicasIncluidas = new HashSet<>();

        for (var solicitado : request.items()) {

            if (solicitado == null
                    || solicitado.practicaId() == null
                    || solicitado.cantidad() == null
                    || solicitado.cantidad() <= 0) {
                throw new IllegalArgumentException(
                        "Cada práctica debe tener ID y cantidad positiva");
            }

            if (!practicasIncluidas.add(solicitado.practicaId())) {
                throw new IllegalArgumentException(
                        "No se puede repetir una práctica en la misma orden");
            }

            var practica = practicas.obtener(
                    solicitado.practicaId());

            var calculo = coseguros.calcular(
                    prestador.id(),
                    practica.id(),
                    afiliado.planId(),
                    fecha
            );

            BigDecimal cantidad = BigDecimal.valueOf(
                    solicitado.cantidad());

            BigDecimal valor = calculo.valorConvenido()
                    .multiply(cantidad);

            BigDecimal cargoEntidad = calculo.aCargoEntidad()
                    .multiply(cantidad);

            BigDecimal coseguro = calculo.coseguroAfiliado()
                    .multiply(cantidad);

            OrdenItem item = new OrdenItem();
            item.setPracticaId(practica.id());
            item.setCantidad(solicitado.cantidad());
            item.setValorConvenido(calculo.valorConvenido());
            item.setPorcentajeCobertura(
                    calculo.porcentajeCobertura());
            item.setCargoEntidad(cargoEntidad);
            item.setCoseguroAfiliado(coseguro);
            item.setRequiereAuditoria(
                    practica.requiereAuditoria());

            orden.agregarItem(item);

            totalValor = totalValor.add(valor);
            totalEntidad = totalEntidad.add(cargoEntidad);
            totalCoseguro = totalCoseguro.add(coseguro);

            if (practica.requiereAuditoria()) {
                requiereAuditoria = true;
            }
        }

        OrdenEstado estado = requiereAuditoria
                ? OrdenEstado.PENDIENTE_AUDITORIA
                : OrdenEstado.EMITIDA;

        orden.setEstado(estado.name());
        orden.setTotalValorConvenido(totalValor);
        orden.setTotalCargoEntidad(totalEntidad);
        orden.setTotalCoseguro(totalCoseguro);

        long siguiente = numerador.siguiente("ORDEN");

        orden.setNumero(
                String.format("ORD-%08d", siguiente));

        orden = ordenRepository.saveAndFlush(orden);

        OrdenEstadoHistorial historial =
                new OrdenEstadoHistorial();

        historial.setOrden(orden);
        historial.setEstadoAnterior(null);
        historial.setEstadoNuevo(estado.name());
        historial.setFecha(LocalDateTime.now());
        historial.setUsuario(usuarioActual.usuario());
        historial.setComentario("Emisión inicial de la orden");

        historialRepository.save(historial);

        // Regla acordada con F3:
        // el coseguro se cobra en la cuota del mes siguiente,
        // incluso cuando la orden queda pendiente de auditoría.
        if (totalCoseguro.signum() > 0) {

            cargos.registrarCoseguro(
                    new CargoCuotaServicio.CoseguroParaCuota(
                            afiliado.id(),
                            fecha,
                            totalCoseguro,
                            "Orden médica " + orden.getNumero(),
                            orden.getNumero()
                    )
            );
        }

        return new OrdenDtos.OrdenResponse(
                orden.getId(),
                orden.getNumero(),
                orden.getAfiliadoId(),
                orden.getPrestadorId(),
                orden.getFechaEmision(),
                orden.getEstado(),
                orden.getTotalValorConvenido(),
                orden.getTotalCargoEntidad(),
                orden.getTotalCoseguro()
        );
    }
}

