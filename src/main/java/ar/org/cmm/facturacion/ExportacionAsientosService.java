package ar.org.cmm.facturacion;

import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exportación de asientos contables para FEBBO (ENT-04), sin factura fiscal (H2):
 * <ul>
 *   <li>Cuota emitida: Debe "Deudores por cuotas" / Haber la cuenta de cada código de servicio.</li>
 *   <li>Pago imputado: Debe la cuenta del medio de cobro / Haber "Deudores por cuotas".</li>
 * </ul>
 * Formato PROVISORIO (CSV ';'), hasta que IT entregue el diseño de importación de FEBBO:
 * {@code nro_asiento;fecha;cuenta;debe;haber;concepto;referencia}
 */
@Service
public class ExportacionAsientosService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ComprobanteRepository comprobantes;
    private final MovimientoCuentaCorrienteRepository movimientos;
    private final ParametroService parametros;
    private final AuditoriaService auditoria;

    public ExportacionAsientosService(ComprobanteRepository comprobantes, MovimientoCuentaCorrienteRepository movimientos,
                                      ParametroService parametros, AuditoriaService auditoria) {
        this.comprobantes = comprobantes;
        this.movimientos = movimientos;
        this.parametros = parametros;
        this.auditoria = auditoria;
    }

    public record Exportacion(String nombreArchivo, String contenido, int asientos, BigDecimal totalDebe, BigDecimal totalHaber) {
    }

    @Transactional
    public Exportacion exportar(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new ReglaNegocioException("La fecha 'hasta' es anterior a 'desde'.");
        }
        String cuentaDeudores = parametros.texto(ParametroService.CUENTA_DEUDORES);
        StringBuilder sb = new StringBuilder("nro_asiento;fecha;cuenta;debe;haber;concepto;referencia\n");
        int nro = 0;
        BigDecimal totalDebe = BigDecimal.ZERO, totalHaber = BigDecimal.ZERO;

        for (Comprobante c : comprobantes.findByFechaEmisionBetweenOrderByFechaEmisionAscIdAsc(desde, hasta)) {
            if (Comprobante.ANULADO.equals(c.getEstado())) {
                continue;
            }
            nro++;
            String concepto = "Cuota " + c.getPeriodo().substring(4) + "/" + c.getPeriodo().substring(0, 4);
            linea(sb, nro, c.getFechaEmision(), cuentaDeudores, c.getTotal(), BigDecimal.ZERO, concepto, c.getNumero());
            totalDebe = totalDebe.add(c.getTotal());
            Map<String, BigDecimal> porCuenta = new LinkedHashMap<>();
            for (ComprobanteItem i : c.getItems()) {
                porCuenta.merge(i.getCodigoServicio().getCuentaContable().getCodigo(), i.getImporte(), BigDecimal::add);
            }
            for (Map.Entry<String, BigDecimal> e : porCuenta.entrySet()) {
                linea(sb, nro, c.getFechaEmision(), e.getKey(), BigDecimal.ZERO, e.getValue(), concepto, c.getNumero());
                totalHaber = totalHaber.add(e.getValue());
            }
        }

        List<MovimientoCuentaCorriente> pagos = movimientos.findByTipoAndFechaBetweenAndPagoIdIsNotNullOrderByFechaAscIdAsc(
                MovimientoCuentaCorriente.HABER, desde, hasta);
        for (MovimientoCuentaCorriente m : pagos) {
            if (m.getCuentaContrapartida() == null) {
                continue;
            }
            nro++;
            String ref = "PAGO-" + m.getPagoId();
            linea(sb, nro, m.getFecha(), m.getCuentaContrapartida(), m.getImporte(), BigDecimal.ZERO, m.getConcepto(), ref);
            linea(sb, nro, m.getFecha(), cuentaDeudores, BigDecimal.ZERO, m.getImporte(), m.getConcepto(), ref);
            totalDebe = totalDebe.add(m.getImporte());
            totalHaber = totalHaber.add(m.getImporte());
        }
        if (nro == 0) {
            throw new ReglaNegocioException("No hay cuotas ni pagos entre " + desde.format(FECHA) + " y " + hasta.format(FECHA) + ".");
        }
        auditoria.registrar("asientos", 0L, "EXPORTACION_FEBBO", desde + " a " + hasta + ": " + nro + " asientos");
        String nombre = "asientos-febbo-" + desde + "-a-" + hasta + ".csv";
        return new Exportacion(nombre, sb.toString(), nro, totalDebe, totalHaber);
    }

    private static void linea(StringBuilder sb, int nro, LocalDate fecha, String cuenta, BigDecimal debe, BigDecimal haber,
                              String concepto, String referencia) {
        sb.append(nro).append(';').append(fecha.format(FECHA)).append(';').append(cuenta).append(';')
                .append(debe.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()).append(';')
                .append(haber.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()).append(';')
                .append(concepto.replace(';', ',')).append(';').append(referencia).append('\n');
    }
}
