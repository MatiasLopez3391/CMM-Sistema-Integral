package ar.org.cmm.facturacion;

import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exportación de asientos al sistema contable del CMM (ENT-04, T48). Sin factura fiscal (H2):
 * <ul>
 *   <li>Cuota emitida: Debe "Deudores por cuotas" / Haber la cuenta de cada código de servicio.</li>
 *   <li>Nota de débito: igual que una cuota. Nota de crédito: el asiento inverso.</li>
 *   <li>Pago imputado: Debe la cuenta del medio de cobro / Haber "Deudores por cuotas".</li>
 * </ul>
 * El formato del archivo (columnas, separador, fecha, decimales, encabezado) se configura en parámetros
 * para adaptarlo al sistema contable que se use (FEBBO u otro) sin tocar el código.
 */
@Service
public class ExportacionAsientosService {

    /** Columnas disponibles. "importe" es el valor sin signo y "dh" dice D (debe) o H (haber), para sistemas que lo piden así. */
    public static final List<String> COLUMNAS = List.of("lote", "nro_asiento", "fecha", "cuenta", "debe", "haber", "importe", "dh", "concepto",
            "referencia");

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

    /** Una línea de asiento, antes de darle formato. */
    public record Linea(String lote, int nroAsiento, LocalDate fecha, String cuenta, BigDecimal debe, BigDecimal haber,
                        String concepto, String referencia) {
    }

    /** Formato vigente del archivo. */
    public record Formato(List<String> columnas, String separador, String formatoFecha, String separadorDecimal, boolean encabezado,
                          String extension) {
    }

    @Transactional
    public Exportacion exportar(LocalDate desde, LocalDate hasta) {
        return exportar(desde, hasta, null);
    }

    /** @param lote si no es null, solo los asientos de ese lote del sistema contable */
    @Transactional
    public Exportacion exportar(LocalDate desde, LocalDate hasta, String lote) {
        if (hasta.isBefore(desde)) {
            throw new ReglaNegocioException("La fecha 'hasta' es anterior a 'desde'.");
        }
        Formato formato = formato();
        List<Linea> lineas = asientos(desde, hasta);
        if (lote != null && !lote.isBlank()) {
            lineas = lineas.stream().filter(l -> lote.trim().equals(l.lote())).toList();
        }
        int asientos = (int) lineas.stream().map(Linea::nroAsiento).distinct().count();
        if (asientos == 0) {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            throw new ReglaNegocioException("No hay comprobantes ni pagos" + (lote != null && !lote.isBlank() ? " del lote " + lote : "")
                    + " entre " + desde.format(f) + " y " + hasta.format(f) + ".");
        }
        BigDecimal debe = lineas.stream().map(Linea::debe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal haber = lineas.stream().map(Linea::haber).reduce(BigDecimal.ZERO, BigDecimal::add);
        auditoria.registrar("asientos", 0L, "EXPORTACION_CONTABLE", desde + " a " + hasta
                + (lote != null && !lote.isBlank() ? " lote " + lote : "") + ": " + asientos + " asientos");
        String nombre = "asientos-contables-" + (lote != null && !lote.isBlank() ? "lote" + lote.trim() + "-" : "") + desde + "-a-" + hasta
                + "." + formato.extension();
        return new Exportacion(nombre, renderizar(lineas, formato), asientos, debe, haber);
    }

    /** Dos asientos de ejemplo con el formato vigente, para revisarlo en pantalla antes de exportar. */
    @Transactional(readOnly = true)
    public Map<String, Object> ejemplo() {
        Formato f = formato();
        LocalDate d = LocalDate.of(2026, 11, 1);
        List<Linea> ej = List.of(
                new Linea(lote(Comprobante.TIPO_CUOTA), 1, d, "1.1.03.01", new BigDecimal("190527.67"), BigDecimal.ZERO, "Cuota 11/2026", "CI-00000001"),
                new Linea(lote(Comprobante.TIPO_CUOTA), 1, d, "4.1.01.01", BigDecimal.ZERO, new BigDecimal("188649.67"), "Cuota 11/2026", "CI-00000001"),
                new Linea(lote(Comprobante.TIPO_CUOTA), 1, d, "4.1.01.02", BigDecimal.ZERO, new BigDecimal("1878.00"), "Cuota 11/2026", "CI-00000001"));
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("formato", f);
        res.put("columnasDisponibles", COLUMNAS);
        res.put("ejemplo", renderizar(ej, f));
        res.put("letra", letra());
        res.put("lotes", lotes());
        return res;
    }

    Formato formato() {
        String sep = parametros.texto(ParametroService.EXPORT_SEPARADOR);
        String separador = sep.equalsIgnoreCase("TAB") ? "\t" : sep;
        String decimal = parametros.texto(ParametroService.EXPORT_DECIMAL);
        if (separador.equals(decimal)) {
            throw new ReglaNegocioException("El separador de columnas y el decimal no pueden ser iguales: revise el formato del archivo.");
        }
        return new Formato(List.of(parametros.texto(ParametroService.EXPORT_COLUMNAS).toLowerCase().replace(" ", "").split(",")),
                separador, parametros.texto(ParametroService.EXPORT_FORMATO_FECHA), decimal,
                parametros.texto(ParametroService.EXPORT_ENCABEZADO).equalsIgnoreCase("SI"),
                parametros.texto(ParametroService.EXPORT_EXTENSION).toLowerCase());
    }

    List<Linea> asientos(LocalDate desde, LocalDate hasta) {
        String cuentaDeudores = parametros.texto(ParametroService.CUENTA_DEUDORES);
        String lotePagos = sinGuion(parametros.textoOpcional("contable.lote.PAGO"));
        List<Linea> res = new ArrayList<>();
        int nro = 0;
        for (Comprobante c : comprobantes.findByFechaEmisionBetweenOrderByFechaEmisionAscIdAsc(desde, hasta)) {
            if (Comprobante.ANULADO.equals(c.getEstado())) {
                continue;
            }
            nro++;
            String lote = lote(c.getTipo());
            String concepto = switch (c.getTipo()) {
                case Comprobante.TIPO_NOTA_CREDITO -> "Nota de crédito " + c.getNumero();
                case Comprobante.TIPO_NOTA_DEBITO -> "Nota de débito " + c.getNumero();
                default -> "Cuota " + c.getPeriodo().substring(4) + "/" + c.getPeriodo().substring(0, 4);
            };
            Map<String, BigDecimal> porCuenta = new LinkedHashMap<>();
            for (ComprobanteItem i : c.getItems()) {
                porCuenta.merge(i.getCodigoServicio().getCuentaContable().getCodigo(), i.getImporte(), BigDecimal::add);
            }
            if (c.esNotaCredito()) {
                // Nota de crédito: asiento inverso (Debe la cuenta del concepto corregido / Haber deudores)
                for (Map.Entry<String, BigDecimal> e : porCuenta.entrySet()) {
                    res.add(new Linea(lote, nro, c.getFechaEmision(), e.getKey(), e.getValue(), BigDecimal.ZERO, concepto, c.getNumero()));
                }
                res.add(new Linea(lote, nro, c.getFechaEmision(), cuentaDeudores, BigDecimal.ZERO, c.getTotal(), concepto, c.getNumero()));
                continue;
            }
            res.add(new Linea(lote, nro, c.getFechaEmision(), cuentaDeudores, c.getTotal(), BigDecimal.ZERO, concepto, c.getNumero()));
            for (Map.Entry<String, BigDecimal> e : porCuenta.entrySet()) {
                res.add(new Linea(lote, nro, c.getFechaEmision(), e.getKey(), BigDecimal.ZERO, e.getValue(), concepto, c.getNumero()));
            }
        }
        for (MovimientoCuentaCorriente m : movimientos.findByTipoAndFechaBetweenAndPagoIdIsNotNullOrderByFechaAscIdAsc(
                MovimientoCuentaCorriente.HABER, desde, hasta)) {
            if (m.getCuentaContrapartida() == null) {
                continue;
            }
            nro++;
            String ref = "PAGO-" + m.getPagoId();
            res.add(new Linea(lotePagos, nro, m.getFecha(), m.getCuentaContrapartida(), m.getImporte(), BigDecimal.ZERO, m.getConcepto(), ref));
            res.add(new Linea(lotePagos, nro, m.getFecha(), cuentaDeudores, BigDecimal.ZERO, m.getImporte(), m.getConcepto(), ref));
        }
        return res;
    }

    /**
     * Lote del sistema contable según el tipo de comprobante y su letra. Por ahora todos los afiliados llevan la letra
     * por defecto (B, consumidor final); la A necesita la condición frente al IVA del titular (pendiente en F1).
     */
    String lote(String tipoComprobante) {
        return sinGuion(parametros.textoOpcional("contable.lote." + tipoComprobante + "." + letra()));
    }

    private String letra() {
        String l = parametros.textoOpcional(ParametroService.LETRA_POR_DEFECTO);
        return l.isBlank() ? "B" : l.trim().toUpperCase();
    }

    private Map<String, String> lotes() {
        Map<String, String> m = new LinkedHashMap<>();
        for (String tipo : List.of(Comprobante.TIPO_CUOTA, Comprobante.TIPO_NOTA_DEBITO, Comprobante.TIPO_NOTA_CREDITO)) {
            for (String letra : List.of("A", "B")) {
                m.put(tipo + "." + letra, parametros.textoOpcional("contable.lote." + tipo + "." + letra));
            }
        }
        m.put("PAGO", parametros.textoOpcional("contable.lote.PAGO"));
        return m;
    }

    private static String sinGuion(String v) {
        return v == null || v.isBlank() || v.trim().equals("-") ? "" : v.trim();
    }

    static String renderizar(List<Linea> lineas, Formato f) {
        DateTimeFormatter fecha = DateTimeFormatter.ofPattern(f.formatoFecha());
        StringBuilder sb = new StringBuilder();
        if (f.encabezado()) {
            sb.append(String.join(f.separador(), f.columnas())).append('\n');
        }
        for (Linea l : lineas) {
            List<String> valores = new ArrayList<>();
            for (String col : f.columnas()) {
                valores.add(switch (col) {
                    case "lote" -> l.lote() == null ? "" : l.lote();
                    case "nro_asiento" -> String.valueOf(l.nroAsiento());
                    case "fecha" -> l.fecha().format(fecha);
                    case "cuenta" -> l.cuenta();
                    case "debe" -> importe(l.debe(), f);
                    case "haber" -> importe(l.haber(), f);
                    case "importe" -> importe(l.debe().signum() != 0 ? l.debe() : l.haber(), f);
                    case "dh" -> l.debe().signum() != 0 ? "D" : "H";
                    case "concepto" -> limpiar(l.concepto(), f.separador());
                    case "referencia" -> limpiar(l.referencia(), f.separador());
                    default -> "";
                });
            }
            sb.append(String.join(f.separador(), valores)).append('\n');
        }
        return sb.toString();
    }

    private static String importe(BigDecimal v, Formato f) {
        String t = v.setScale(2, RoundingMode.HALF_UP).toPlainString();
        return ",".equals(f.separadorDecimal()) ? t.replace('.', ',') : t;
    }

    private static String limpiar(String texto, String separador) {
        String t = texto == null ? "" : texto.replace("\n", " ").replace("\r", " ");
        return t.replace(separador, ",".equals(separador) ? " " : ",");
    }
}
