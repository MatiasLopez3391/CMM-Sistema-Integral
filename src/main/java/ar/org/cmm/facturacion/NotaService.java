package ar.org.cmm.facturacion;

import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.NumeradorService;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.AfiliadoConsulta.AfiliadoResumen;
import ar.org.cmm.facturacion.FacturacionDtos.NotaRequest;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoNota;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Notas de crédito y débito internas (T46, respuestas L6 y L7 del CMM). Corrigen errores sin tocar el
 * comprobante original, que queda como se emitió:
 * <ul>
 *   <li><b>Nota de débito:</b> suma deuda al grupo; vence junto con la próxima cuota.</li>
 *   <li><b>Nota de crédito:</b> cancela deuda. Si corrige un comprobante, se aplica primero a ese; lo que sobra
 *       cancela la deuda más antigua y, si no hay, queda a favor para las cuotas siguientes.</li>
 * </ul>
 */
@Service
public class NotaService {

    private static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("yyyyMM");

    private final ComprobanteRepository comprobantes;
    private final MovimientoCuentaCorrienteRepository movimientos;
    private final CodigoServicioRepository codigos;
    private final CuentaCorrienteServicioImpl cuentaCorriente;
    private final CalendarioHabilService calendario;
    private final ParametroService parametros;
    private final AfiliadoConsulta afiliados;
    private final NumeradorService numerador;
    private final AuditoriaService auditoria;
    private final AplicacionCreditoRepository aplicaciones;

    public NotaService(AplicacionCreditoRepository aplicaciones, ComprobanteRepository comprobantes, MovimientoCuentaCorrienteRepository movimientos,
                       CodigoServicioRepository codigos, CuentaCorrienteServicioImpl cuentaCorriente, CalendarioHabilService calendario,
                       ParametroService parametros, AfiliadoConsulta afiliados, NumeradorService numerador, AuditoriaService auditoria) {
        this.aplicaciones = aplicaciones;
        this.comprobantes = comprobantes;
        this.movimientos = movimientos;
        this.codigos = codigos;
        this.cuentaCorriente = cuentaCorriente;
        this.calendario = calendario;
        this.parametros = parametros;
        this.afiliados = afiliados;
        this.numerador = numerador;
        this.auditoria = auditoria;
    }

    @Transactional
    public ResultadoNota emitir(NotaRequest req) {
        boolean credito = switch (req.tipo().trim().toUpperCase()) {
            case "CREDITO", Comprobante.TIPO_NOTA_CREDITO -> true;
            case "DEBITO", Comprobante.TIPO_NOTA_DEBITO -> false;
            default -> throw new ReglaNegocioException("Tipo de nota inválido: use CREDITO o DEBITO.");
        };
        if (req.fecha().isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de la nota no puede ser futura.");
        }
        AfiliadoResumen titular = afiliados.obtener(afiliados.obtener(req.afiliadoId()).idTitular());
        CodigoServicio cod = codigos.findById(req.codigoServicioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Código de servicio", req.codigoServicioId()));
        BigDecimal importe = req.importe().setScale(2, RoundingMode.HALF_UP);
        String motivo = req.motivo().trim();

        Comprobante asociado = null;
        if (req.comprobanteAsociadoId() != null) {
            asociado = comprobantes.findById(req.comprobanteAsociadoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Comprobante", req.comprobanteAsociadoId()));
            if (!asociado.getAfiliadoId().equals(titular.id())) {
                throw new ReglaNegocioException("El comprobante " + asociado.getNumero() + " no es de este grupo familiar.");
            }
            if (asociado.esNotaCredito()) {
                throw new ReglaNegocioException("No se puede emitir una nota sobre otra nota de crédito.");
            }
            if (credito && importe.compareTo(asociado.getTotal().subtract(creditosPrevios(asociado))) > 0) {
                throw new ReglaNegocioException("La nota de crédito supera lo que queda por corregir del comprobante "
                        + asociado.getNumero() + ".");
            }
        }

        Comprobante n = new Comprobante();
        n.setTipo(credito ? Comprobante.TIPO_NOTA_CREDITO : Comprobante.TIPO_NOTA_DEBITO);
        n.setNumero(String.format(credito ? "NC-%08d" : "ND-%08d",
                numerador.siguiente(credito ? Comprobante.TIPO_NOTA_CREDITO : Comprobante.TIPO_NOTA_DEBITO)));
        n.setAfiliadoId(titular.id());
        n.setPeriodo(YearMonth.from(req.fecha()).format(PERIODO));
        n.setFechaEmision(req.fecha());
        n.setFechaVencimiento(credito ? req.fecha() : vencimientoProximaCuota(req.fecha()));
        n.setTotal(importe);
        n.setSaldo(credito ? BigDecimal.ZERO : importe);
        n.setEstado(credito ? Comprobante.EMITIDA : Comprobante.PENDIENTE);
        n.setObservaciones(motivo);
        n.setComprobanteAsociadoId(asociado != null ? asociado.getId() : null);
        ComprobanteItem item = new ComprobanteItem();
        item.setCodigoServicio(cod);
        String desc = (credito ? "Nota de crédito: " : "Nota de débito: ") + motivo
                + (asociado != null ? " (corrige " + asociado.getNumero() + ")" : "");
        item.setDescripcion(desc.length() > 200 ? desc.substring(0, 200) : desc);
        item.setCantidad(1);
        item.setPrecioUnitario(importe);
        item.setImporte(importe);
        n.agregarItem(item);
        Comprobante guardada = comprobantes.save(n);

        MovimientoCuentaCorriente m = new MovimientoCuentaCorriente();
        m.setAfiliadoId(titular.id());
        m.setFecha(req.fecha());
        m.setTipo(credito ? MovimientoCuentaCorriente.HABER : MovimientoCuentaCorriente.DEBE);
        m.setConcepto((credito ? "Nota de crédito " : "Nota de débito ") + guardada.getNumero() + " - " + motivo);
        m.setComprobanteId(guardada.getId());
        m.setImporte(importe);
        movimientos.save(m);

        if (credito && asociado != null) {
            cuentaCorriente.aplicarNotaA(guardada, asociado, importe);
        }
        // Lo que quede de la nota de crédito cancela la deuda más antigua; a una nota de débito se le aplica lo que haya a favor.
        cuentaCorriente.imputarCreditoDisponible(titular.id(), null, null);
        BigDecimal aplicado = credito
                ? aplicaciones.findByNotaCreditoIdIn(java.util.List.of(guardada.getId())).stream()
                        .map(AplicacionCredito::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add)
                : importe.subtract(guardada.getSaldo());
        auditoria.registrar("comprobante", guardada.getId(), credito ? "NOTA_CREDITO" : "NOTA_DEBITO",
                guardada.getNumero() + " $" + importe + " · " + titular.nroAfiliado()
                        + (asociado != null ? " · corrige " + asociado.getNumero() : "") + " · " + motivo);
        BigDecimal aFavor = credito ? importe.subtract(aplicado).max(BigDecimal.ZERO) : BigDecimal.ZERO;
        return new ResultadoNota(guardada.getId(), guardada.getNumero(), guardada.getTipo(), importe, aplicado, aFavor,
                guardada.getFechaVencimiento());
    }

    /** Lo ya corregido de un comprobante con notas de crédito anteriores. */
    private BigDecimal creditosPrevios(Comprobante c) {
        return comprobantes.findByComprobanteAsociadoIdOrderByIdAsc(c.getId()).stream()
                .filter(Comprobante::esNotaCredito).map(Comprobante::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** La nota de débito vence con la próxima cuota: día de vencimiento del mes siguiente (o el hábil posterior). */
    private LocalDate vencimientoProximaCuota(LocalDate fecha) {
        YearMonth siguiente = YearMonth.from(fecha).plusMonths(1);
        int dia = Math.min(parametros.entero(ParametroService.DIA_VENCIMIENTO), siguiente.lengthOfMonth());
        return calendario.habilDesde(siguiente.atDay(dia));
    }
}
