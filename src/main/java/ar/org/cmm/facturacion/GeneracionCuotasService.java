package ar.org.cmm.facturacion;

import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.NumeradorService;
import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.AfiliadoConsulta.AfiliadoResumen;
import ar.org.cmm.facturacion.FacturacionDtos.PropuestaComprobante;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoGeneracion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Generación masiva de la cuota mensual (comprobante interno por grupo familiar).
 *
 * <p>Por cada titular activo: cuota del plan × integrantes activos + adicionales configurados
 * (ej. Subsidio Supra Salud Alta Complejidad) + recargo por mora sobre la deuda vencida impaga.
 * Es idempotente: si el titular ya tiene la cuota del período, se omite.
 */
@Service
public class GeneracionCuotasService {

    static final String NUMERADOR = "COMPROBANTE_CUOTA";
    private static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("yyyyMM");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final AfiliadoConsulta afiliados;
    private final ValorCuotaService valores;
    private final CodigoServicioRepository codigos;
    private final ComprobanteRepository comprobantes;
    private final MovimientoCuentaCorrienteRepository movimientos;
    private final CuentaCorrienteServicioImpl cuentaCorriente;
    private final ParametroService parametros;
    private final NumeradorService numerador;
    private final AuditoriaService auditoria;

    public GeneracionCuotasService(AfiliadoConsulta afiliados, ValorCuotaService valores, CodigoServicioRepository codigos,
                                   ComprobanteRepository comprobantes, MovimientoCuentaCorrienteRepository movimientos,
                                   CuentaCorrienteServicioImpl cuentaCorriente, ParametroService parametros,
                                   NumeradorService numerador, AuditoriaService auditoria) {
        this.afiliados = afiliados;
        this.valores = valores;
        this.codigos = codigos;
        this.comprobantes = comprobantes;
        this.movimientos = movimientos;
        this.cuentaCorriente = cuentaCorriente;
        this.parametros = parametros;
        this.numerador = numerador;
        this.auditoria = auditoria;
    }

    /** Calcula sin guardar: permite revisar antes de generar. */
    @Transactional(readOnly = true)
    public ResultadoGeneracion simular(String periodo, LocalDate fechaEmision) {
        return procesar(periodo, fechaEmision, true);
    }

    @Transactional
    public ResultadoGeneracion generar(String periodo, LocalDate fechaEmision) {
        ResultadoGeneracion r = procesar(periodo, fechaEmision, false);
        auditoria.registrar("comprobante", 0L, "GENERACION_CUOTAS",
                "Período " + periodo + ": " + r.generados() + " comprobantes, total " + r.totalGeneral());
        return r;
    }

    private ResultadoGeneracion procesar(String periodoTxt, LocalDate fechaEmisionParam, boolean simulacion) {
        YearMonth periodo = parsearPeriodo(periodoTxt);
        LocalDate fechaEmision = fechaEmisionParam != null ? fechaEmisionParam : periodo.atDay(1);
        int dia = Math.min(parametros.entero(ParametroService.DIA_VENCIMIENTO), periodo.lengthOfMonth());
        LocalDate vencimiento = periodo.atDay(dia);
        if (vencimiento.isBefore(fechaEmision)) {
            throw new ReglaNegocioException("La fecha de emisión es posterior al vencimiento del período (" + vencimiento + ").");
        }
        BigDecimal tasa = parametros.decimal(ParametroService.TASA_RECARGO);
        CodigoServicio codCuota = codigos.findFirstByTipoAndActivoTrue("CUOTA")
                .orElseThrow(() -> new ReglaNegocioException("No hay un código de servicio activo de tipo CUOTA."));
        CodigoServicio codRecargo = codigos.findFirstByTipoAndActivoTrue("RECARGO").orElse(null);
        List<CodigoServicio> adicionales = codigos.findByIncluirEnCuotaTrueAndActivoTrue().stream()
                .filter(c -> c.getImporteFijo() != null && c.getImporteFijo().signum() > 0)
                .toList();

        String per = periodo.format(PERIODO);
        String mesAnio = String.format("%02d/%d", periodo.getMonthValue(), periodo.getYear());
        List<PropuestaComprobante> propuestas = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();
        int omitidos = 0, conRecargo = 0;
        BigDecimal totalCuotas = BigDecimal.ZERO, totalRecargos = BigDecimal.ZERO, totalGeneral = BigDecimal.ZERO;

        for (AfiliadoResumen titular : afiliados.titularesActivos()) {
            if (comprobantes.existsByAfiliadoIdAndPeriodoAndTipo(titular.id(), per, Comprobante.TIPO_CUOTA)) {
                omitidos++;
                continue;
            }
            Optional<BigDecimal> valor = valores.vigente(titular.planId(), periodo.atDay(1));
            if (valor.isEmpty()) {
                advertencias.add(titular.nroAfiliado() + " " + titular.apellidoNombre() + ": el plan " + titular.planCodigo()
                        + " no tiene valor de cuota vigente para " + mesAnio);
                continue;
            }
            int integrantes = afiliados.integrantesActivos(titular.id()).size();

            Comprobante c = new Comprobante();
            c.setTipo(Comprobante.TIPO_CUOTA);
            c.setAfiliadoId(titular.id());
            c.setPeriodo(per);
            c.setFechaEmision(fechaEmision);
            c.setFechaVencimiento(vencimiento);
            c.setEstado(Comprobante.PENDIENTE);

            BigDecimal cuota = valor.get().multiply(BigDecimal.valueOf(integrantes));
            c.agregarItem(item(codCuota, codCuota.getDescripcion() + " " + mesAnio, integrantes, valor.get()));

            BigDecimal sumaAdicionales = BigDecimal.ZERO;
            for (CodigoServicio ad : adicionales) {
                c.agregarItem(item(ad, ad.getDescripcion() + " " + mesAnio, integrantes, ad.getImporteFijo()));
                sumaAdicionales = sumaAdicionales.add(ad.getImporteFijo().multiply(BigDecimal.valueOf(integrantes)));
            }

            BigDecimal deudaVencida = comprobantes.vencidosImpagos(titular.id(), fechaEmision).stream()
                    .map(Comprobante::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal recargo = BigDecimal.ZERO;
            if (codRecargo != null && deudaVencida.signum() > 0 && tasa.signum() > 0) {
                recargo = deudaVencida.multiply(tasa).divide(CIEN, 2, RoundingMode.HALF_UP);
                c.agregarItem(item(codRecargo, codRecargo.getDescripcion() + " (" + tasa.stripTrailingZeros().toPlainString()
                        + "% s/ $" + deudaVencida.setScale(2, RoundingMode.HALF_UP).toPlainString() + ")", 1, recargo));
                conRecargo++;
            }

            BigDecimal total = c.getItems().stream().map(ComprobanteItem::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
            c.setTotal(total);
            c.setSaldo(total);

            totalCuotas = totalCuotas.add(cuota).add(sumaAdicionales);
            totalRecargos = totalRecargos.add(recargo);
            totalGeneral = totalGeneral.add(total);
            propuestas.add(new PropuestaComprobante(titular.id(), titular.nroAfiliado(), titular.apellidoNombre(), titular.planCodigo(),
                    integrantes, cuota, sumaAdicionales, recargo, deudaVencida, total,
                    titular.bloqueado() ? "Grupo bloqueado" : null));

            if (!simulacion) {
                c.setNumero(String.format("CI-%08d", numerador.siguiente(NUMERADOR)));
                Comprobante guardado = comprobantes.save(c);
                MovimientoCuentaCorriente m = new MovimientoCuentaCorriente();
                m.setAfiliadoId(titular.id());
                m.setFecha(fechaEmision);
                m.setTipo(MovimientoCuentaCorriente.DEBE);
                m.setConcepto("Cuota " + mesAnio + " - " + guardado.getNumero());
                m.setComprobanteId(guardado.getId());
                m.setImporte(total);
                movimientos.save(m);
                // Si había pagos a cuenta (saldo a favor), se aplican a la cuota recién generada.
                cuentaCorriente.imputarCreditoDisponible(titular.id(), null, null);
            }
        }
        if (propuestas.isEmpty() && omitidos > 0 && advertencias.isEmpty()) {
            advertencias.add("Todas las cuotas del período " + mesAnio + " ya estaban generadas.");
        }
        return new ResultadoGeneracion(per, fechaEmision, vencimiento, simulacion, propuestas.size(), omitidos, conRecargo,
                totalCuotas, totalRecargos, totalGeneral, advertencias, propuestas);
    }

    private static ComprobanteItem item(CodigoServicio cod, String descripcion, int cantidad, BigDecimal precio) {
        ComprobanteItem i = new ComprobanteItem();
        i.setCodigoServicio(cod);
        i.setDescripcion(descripcion);
        i.setCantidad(cantidad);
        i.setPrecioUnitario(precio.setScale(2, RoundingMode.HALF_UP));
        i.setImporte(precio.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP));
        return i;
    }

    static YearMonth parsearPeriodo(String texto) {
        if (texto == null) {
            throw new ReglaNegocioException("Indique el período (AAAAMM o AAAA-MM).");
        }
        String t = texto.trim().replace("-", "").replace("/", "");
        try {
            return YearMonth.parse(t, PERIODO);
        } catch (DateTimeParseException e) {
            throw new ReglaNegocioException("Período inválido: " + texto + " (use AAAAMM o AAAA-MM).");
        }
    }
}
