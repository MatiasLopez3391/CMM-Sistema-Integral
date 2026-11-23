package ar.org.cmm.facturacion;

import ar.org.cmm.comun.reportes.Reporte;
import ar.org.cmm.comun.reportes.Reporte.Tipo;
import ar.org.cmm.facturacion.FacturacionDtos.CargoCuotaDto;
import ar.org.cmm.facturacion.FacturacionDtos.ComprobanteDto;
import ar.org.cmm.facturacion.FacturacionDtos.EstadoCuentaDto;
import ar.org.cmm.facturacion.FacturacionDtos.HistorialCosegurosDto;
import ar.org.cmm.facturacion.FacturacionDtos.ImputacionDto;
import ar.org.cmm.facturacion.FacturacionDtos.ItemDto;
import ar.org.cmm.facturacion.FacturacionDtos.MorosoDto;
import ar.org.cmm.facturacion.FacturacionDtos.MovimientoDto;
import ar.org.cmm.facturacion.FacturacionDtos.ResumenConceptoDto;
import ar.org.cmm.facturacion.FacturacionDtos.ResumenFacturacionDto;
import ar.org.cmm.facturacion.FacturacionDtos.ResumenPlanDto;
import ar.org.cmm.facturacion.FacturacionDtos.TotalPeriodoCosegurosDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Versiones imprimibles (PDF) y descargables (Excel) de las pantallas de F3 (T47, observación del CMM:
 * "todo lo que sean cuentas corrientes, historias clínicas, etc., que puedan imprimirse").
 */
@Service
public class ImpresionFacturacionService {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CuentaCorrienteServicioImpl cuentaCorriente;
    private final ReportesFacturacionService reportes;
    private final CargoCuotaService cargos;

    public ImpresionFacturacionService(CuentaCorrienteServicioImpl cuentaCorriente, ReportesFacturacionService reportes,
                                       CargoCuotaService cargos) {
        this.cuentaCorriente = cuentaCorriente;
        this.reportes = reportes;
        this.cargos = cargos;
    }

    @Transactional(readOnly = true)
    public Reporte estadoDeCuenta(Long afiliadoId, LocalDate desde, LocalDate hasta) {
        EstadoCuentaDto ec = cuentaCorriente.estadoDeCuenta(afiliadoId, desde, hasta);
        Reporte.Builder b = Reporte.de("Estado de cuenta")
                .dato("Titular", ec.nroAfiliado() + " · " + ec.apellidoNombre())
                .dato("Plan", ec.planCodigo())
                .dato("Movimientos", "del " + ec.desde().format(F) + " al " + ec.hasta().format(F))
                .dato("Saldo inicial", moneda(ec.saldoInicial()))
                .dato("Saldo final", moneda(ec.saldoFinal()) + " (positivo = adeuda)")
                .dato("Deuda vencida", moneda(ec.deudaVencida()) + (ec.bloqueado() ? " · Grupo bloqueado" : ""))
                .tabla("Movimientos")
                .columna("Fecha", Tipo.FECHA, 1.2f).columna("Concepto", Tipo.TEXTO, 5f)
                .columna("Debe", Tipo.MONEDA, 1.6f).columna("Haber", Tipo.MONEDA, 1.6f).columna("Saldo", Tipo.MONEDA, 1.6f);
        BigDecimal debe = BigDecimal.ZERO, haber = BigDecimal.ZERO;
        for (MovimientoDto m : ec.movimientos()) {
            b.fila(m.fecha(), m.concepto(), cero(m.debe()), cero(m.haber()), m.saldo());
            debe = debe.add(m.debe());
            haber = haber.add(m.haber());
        }
        b.totales(null, "Totales del período", debe, haber, ec.saldoFinal());
        b.tabla("Comprobantes")
                .columna("Número", Tipo.TEXTO, 1.6f).columna("Tipo", Tipo.TEXTO, 1.6f).columna("Período", Tipo.TEXTO, 1f)
                .columna("Vencimiento", Tipo.FECHA, 1.2f).columna("Total", Tipo.MONEDA, 1.5f).columna("Saldo", Tipo.MONEDA, 1.5f)
                .columna("Estado", Tipo.TEXTO, 1.2f);
        ec.comprobantes().forEach(c -> b.fila(c.numero(), tipo(c.tipo()), periodo(c.periodo()),
                Comprobante.TIPO_NOTA_CREDITO.equals(c.tipo()) ? null : c.fechaVencimiento(), c.total(),
                Comprobante.TIPO_NOTA_CREDITO.equals(c.tipo()) ? null : c.saldo(), estado(c.estado())));
        List<CargoCuotaDto> pendientes = cargos.delGrupo(afiliadoId).stream()
                .filter(c -> CargoCuota.PENDIENTE.equals(c.estado())).toList();
        if (!pendientes.isEmpty()) {
            b.tabla("Coseguros pendientes (se suman a la próxima cuota)")
                    .columna("Cuota", Tipo.TEXTO, 1f).columna("Integrante", Tipo.TEXTO, 3f).columna("Fecha", Tipo.FECHA, 1.2f)
                    .columna("Concepto", Tipo.TEXTO, 4f).columna("Importe", Tipo.MONEDA, 1.5f);
            pendientes.forEach(c -> b.fila(periodo(c.periodoDestino()), c.afiliadoNombre(), c.fecha(), c.concepto(), c.importe()));
        }
        return b.nota("Comprobantes internos, no válidos como factura.").build();
    }

    @Transactional(readOnly = true)
    public Reporte morosos(LocalDate alDia) {
        List<MorosoDto> lista = reportes.morosos(alDia);
        Reporte.Builder b = Reporte.de("Listado de deudores")
                .dato("Deuda vencida al", alDia.format(F))
                .dato("Grupos", lista.size())
                .columna("Nro. afiliado", Tipo.TEXTO, 1.6f).columna("Titular", Tipo.TEXTO, 3.2f).columna("Plan", Tipo.TEXTO, 0.8f)
                .columna("Cuotas vencidas", Tipo.NUMERO, 1f).columna("Deuda vencida", Tipo.MONEDA, 1.6f)
                .columna("Vencimiento más antiguo", Tipo.FECHA, 1.4f).columna("Días de atraso", Tipo.NUMERO, 1f)
                .columna("Email", Tipo.TEXTO, 2.6f).columna("Bloqueado", Tipo.TEXTO, 0.9f);
        BigDecimal total = BigDecimal.ZERO;
        for (MorosoDto m : lista) {
            b.fila(m.nroAfiliado(), m.apellidoNombre(), m.planCodigo(), m.cuotasVencidas(), m.deudaVencida(), m.vencimientoMasAntiguo(),
                    m.diasAtraso(), m.email(), m.bloqueado() ? "Sí" : "No");
            total = total.add(m.deudaVencida());
        }
        return b.totales(null, "Total", null, null, total, null, null, null, null).build();
    }

    @Transactional(readOnly = true)
    public Reporte resumen(String periodo) {
        ResumenFacturacionDto r = reportes.resumen(periodo);
        Reporte.Builder b = Reporte.de("Resumen de facturación " + periodo(r.periodo()))
                .dato("Comprobantes", r.comprobantes())
                .dato("Facturado", moneda(r.total()))
                .dato("Cobrado", moneda(r.cobrado()) + " (" + r.porcentajeCobrado().toPlainString().replace('.', ',') + " %)")
                .dato("Pendiente", moneda(r.pendiente()))
                .tabla("Por concepto")
                .columna("Código", Tipo.TEXTO, 1.2f).columna("Concepto", Tipo.TEXTO, 4f).columna("Cuenta contable", Tipo.TEXTO, 1.5f)
                .columna("Cantidad", Tipo.NUMERO, 1f).columna("Importe", Tipo.MONEDA, 1.6f);
        BigDecimal t = BigDecimal.ZERO;
        for (ResumenConceptoDto c : r.porConcepto()) {
            b.fila(c.codigo(), c.descripcion(), c.cuentaContable(), c.cantidad(), c.importe());
            t = t.add(c.importe());
        }
        b.totales(null, "Total", null, null, t);
        b.tabla("Por plan")
                .columna("Plan", Tipo.TEXTO, 3f).columna("Grupos", Tipo.NUMERO, 1f).columna("Facturado", Tipo.MONEDA, 1.6f)
                .columna("Pendiente", Tipo.MONEDA, 1.6f);
        for (ResumenPlanDto p : r.porPlan()) {
            b.fila(p.planCodigo() + " " + p.planNombre(), p.comprobantes(), p.total(), p.pendiente());
        }
        return b.totales("Total", r.comprobantes(), r.total(), r.pendiente()).build();
    }

    @Transactional(readOnly = true)
    public Reporte comprobante(Long id) {
        ComprobanteDto c = reportes.comprobante(id);
        Reporte.Builder b = Reporte.de(tipo(c.tipo()) + " " + c.numero())
                .dato("Afiliado titular", c.nroAfiliado() + " · " + c.apellidoNombre())
                .dato("Período", periodo(c.periodo()))
                .dato("Emisión", c.fechaEmision().format(F));
        if (!Comprobante.TIPO_NOTA_CREDITO.equals(c.tipo())) {
            b.dato("Vencimiento", c.fechaVencimiento().format(F));
        }
        if (c.comprobanteAsociado() != null) {
            b.dato("Corrige", c.comprobanteAsociado());
        }
        b.dato("Estado", estado(c.estado()))
                .tabla("Detalle")
                .columna("Código", Tipo.TEXTO, 1.2f).columna("Descripción", Tipo.TEXTO, 5f).columna("Cant.", Tipo.NUMERO, 0.7f)
                .columna("Unitario", Tipo.MONEDA, 1.5f).columna("Importe", Tipo.MONEDA, 1.5f);
        for (ItemDto i : c.items()) {
            b.fila(i.codigo(), i.descripcion(), i.cantidad(), i.precioUnitario(), i.importe());
        }
        b.totales(null, "Total", null, null, c.total());
        if (!Comprobante.TIPO_NOTA_CREDITO.equals(c.tipo()) && !c.imputaciones().isEmpty()) {
            b.tabla("Pagos y créditos aplicados").columna("Origen", Tipo.TEXTO, 4f).columna("Fecha", Tipo.FECHA, 1.2f)
                    .columna("Importe", Tipo.MONEDA, 1.5f);
            for (ImputacionDto i : c.imputaciones()) {
                b.fila(i.origen(), i.fecha(), i.importe());
            }
            b.totales("Saldo pendiente", null, c.saldo());
        }
        return b.nota("Comprobante interno del Círculo Médico de Mendoza. No válido como factura.").build();
    }

    @Transactional(readOnly = true)
    public Reporte coseguros(String desde, String hasta, String estado, Long afiliadoId) {
        HistorialCosegurosDto h = cargos.historial(desde, hasta, estado, afiliadoId);
        Reporte.Builder b = Reporte.de("Historial de coseguros")
                .dato("Cuotas", periodo(h.desde()) + " a " + periodo(h.hasta()))
                .dato("Pendiente de facturar", moneda(h.totalPendiente()))
                .dato("Facturado en cuotas", moneda(h.totalFacturado()));
        if (afiliadoId != null && !h.cargos().isEmpty()) {
            b.dato("Grupo", h.cargos().get(0).titularNro() + " · " + h.cargos().get(0).titularNombre());
        }
        if (estado != null && !estado.isBlank()) {
            b.dato("Estado", estado(estado.toUpperCase()));
        }
        b.tabla("Por mes de cuota").columna("Cuota", Tipo.TEXTO, 1f).columna("Coseguros", Tipo.NUMERO, 1f)
                .columna("Pendiente", Tipo.MONEDA, 1.5f).columna("Facturado", Tipo.MONEDA, 1.5f).columna("Anulado", Tipo.MONEDA, 1.5f);
        for (TotalPeriodoCosegurosDto t : h.porPeriodo()) {
            b.fila(periodo(t.periodo()), t.cantidad(), t.pendiente(), t.facturado(), cero(t.anulado()));
        }
        b.tabla("Detalle").columna("Cuota", Tipo.TEXTO, 0.9f).columna("Grupo", Tipo.TEXTO, 2.6f).columna("Integrante", Tipo.TEXTO, 2.6f)
                .columna("Fecha", Tipo.FECHA, 1.1f).columna("Concepto", Tipo.TEXTO, 3.6f).columna("Importe", Tipo.MONEDA, 1.4f)
                .columna("Estado", Tipo.TEXTO, 1.1f).columna("Comprobante", Tipo.TEXTO, 1.4f);
        for (CargoCuotaDto c : h.cargos()) {
            b.fila(periodo(c.periodoDestino()), c.titularNombre(), c.afiliadoNro() + " " + c.afiliadoNombre(), c.fecha(),
                    c.concepto() + (c.origenRef() != null ? " (orden " + c.origenRef() + ")" : ""), c.importe(), estado(c.estado()),
                    c.comprobanteNumero());
        }
        return b.build();
    }

    // ------------------------------------------------------------------

    private static BigDecimal cero(BigDecimal v) {
        return v == null || v.signum() == 0 ? null : v;
    }

    private static String periodo(String p) {
        return p == null || p.length() != 6 ? p : p.substring(4) + "/" + p.substring(0, 4);
    }

    private static String moneda(BigDecimal v) {
        return java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("es-AR")).format(v == null ? BigDecimal.ZERO : v);
    }

    private static String tipo(String t) {
        return switch (t) {
            case Comprobante.TIPO_NOTA_CREDITO -> "Nota de crédito";
            case Comprobante.TIPO_NOTA_DEBITO -> "Nota de débito";
            default -> "Comprobante de cuota";
        };
    }

    private static String estado(String e) {
        return e == null ? "" : e.charAt(0) + e.substring(1).toLowerCase().replace('_', ' ');
    }
}
