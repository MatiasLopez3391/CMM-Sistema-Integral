package ar.org.cmm.facturacion;

import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.AfiliadoConsulta.AfiliadoResumen;
import ar.org.cmm.contratos.CuentaCorrienteServicio;
import ar.org.cmm.facturacion.FacturacionDtos.ComprobanteResumenDto;
import ar.org.cmm.facturacion.FacturacionDtos.EstadoCuentaDto;
import ar.org.cmm.facturacion.FacturacionDtos.MovimientoDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cuenta corriente del grupo familiar (siempre sobre el titular).
 * Imputación automática: cada pago cancela primero la deuda más antigua.
 */
@Service
public class CuentaCorrienteServicioImpl implements CuentaCorrienteServicio {

    private final MovimientoCuentaCorrienteRepository movimientos;
    private final ComprobanteRepository comprobantes;
    private final ImputacionRepository imputaciones;
    private final AfiliadoConsulta afiliados;

    public CuentaCorrienteServicioImpl(MovimientoCuentaCorrienteRepository movimientos, ComprobanteRepository comprobantes,
                                       ImputacionRepository imputaciones, AfiliadoConsulta afiliados) {
        this.movimientos = movimientos;
        this.comprobantes = comprobantes;
        this.imputaciones = imputaciones;
        this.afiliados = afiliados;
    }

    @Override
    @Transactional
    public ResultadoImputacion registrarPago(PagoParaImputar pago) {
        if (pago.importe() == null || pago.importe().signum() <= 0) {
            throw new ReglaNegocioException("El importe del pago debe ser mayor a cero.");
        }
        Long titularId = afiliados.obtener(pago.afiliadoId()).idTitular();
        MovimientoCuentaCorriente m = new MovimientoCuentaCorriente();
        m.setAfiliadoId(titularId);
        m.setFecha(pago.fecha());
        m.setTipo(MovimientoCuentaCorriente.HABER);
        m.setConcepto(pago.concepto());
        m.setPagoId(pago.pagoId());
        m.setCuentaContrapartida(pago.cuentaContable());
        m.setImporte(pago.importe());
        movimientos.save(m);

        List<String> cancelados = new ArrayList<>();
        BigDecimal imputado = imputarCreditoDisponible(titularId, pago.pagoId(), cancelados);
        return new ResultadoImputacion(titularId, pago.importe(), imputado, pago.importe().subtract(imputado).max(BigDecimal.ZERO),
                cancelados);
    }

    /**
     * Aplica los pagos con remanente (saldo a favor) a los comprobantes pendientes, del más antiguo al más nuevo.
     * Se llama al registrar un pago y al generar una cuota nueva (para consumir pagos a cuenta).
     *
     * @param soloPagoId si no es null, solo imputa ese pago
     * @return importe imputado en esta llamada
     */
    @Transactional
    public BigDecimal imputarCreditoDisponible(Long titularId, Long soloPagoId, List<String> cancelados) {
        List<Comprobante> pendientes = comprobantes.pendientesParaImputar(titularId);
        if (pendientes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Map<Long, LocalDate> fechasPago = new HashMap<>();
        Map<Long, BigDecimal> remanentes = remanentesDePagos(titularId, fechasPago);
        BigDecimal total = BigDecimal.ZERO;
        int idx = 0;
        for (Map.Entry<Long, BigDecimal> e : remanentes.entrySet()) {
            Long pagoId = e.getKey();
            if (soloPagoId != null && !soloPagoId.equals(pagoId)) {
                continue;
            }
            BigDecimal disponible = e.getValue();
            while (disponible.signum() > 0 && idx < pendientes.size()) {
                Comprobante c = pendientes.get(idx);
                BigDecimal aplicar = disponible.min(c.getSaldo());
                c.aplicarPago(aplicar);
                Imputacion imp = new Imputacion();
                imp.setPagoId(pagoId);
                imp.setComprobanteId(c.getId());
                imp.setImporte(aplicar);
                // Fecha de imputación: la del pago, o la de emisión si el pago a cuenta es anterior a la cuota.
                LocalDate fechaPago = fechasPago.getOrDefault(pagoId, LocalDate.now());
                imp.setFecha(fechaPago.isBefore(c.getFechaEmision()) ? c.getFechaEmision() : fechaPago);
                imputaciones.save(imp);
                disponible = disponible.subtract(aplicar);
                total = total.add(aplicar);
                if (c.getSaldo().signum() <= 0) {
                    if (cancelados != null) {
                        cancelados.add(c.getNumero());
                    }
                    idx++;
                }
            }
        }
        return total;
    }

    /** Pagos del titular con importe todavía no imputado, en orden cronológico. */
    private Map<Long, BigDecimal> remanentesDePagos(Long titularId, Map<Long, LocalDate> fechasPago) {
        Map<Long, BigDecimal> remanentes = new java.util.LinkedHashMap<>();
        for (MovimientoCuentaCorriente m : movimientos.findByAfiliadoIdOrderByFechaAscIdAsc(titularId)) {
            if (MovimientoCuentaCorriente.HABER.equals(m.getTipo()) && m.getPagoId() != null) {
                remanentes.merge(m.getPagoId(), m.getImporte(), BigDecimal::add);
                fechasPago.put(m.getPagoId(), m.getFecha());
            }
        }
        if (remanentes.isEmpty()) {
            return remanentes;
        }
        Map<Long, BigDecimal> imputadoPorPago = new HashMap<>();
        for (Imputacion i : imputaciones.findByPagoIdIn(remanentes.keySet())) {
            imputadoPorPago.merge(i.getPagoId(), i.getImporte(), BigDecimal::add);
        }
        remanentes.replaceAll((pagoId, importe) -> importe.subtract(imputadoPorPago.getOrDefault(pagoId, BigDecimal.ZERO)));
        remanentes.values().removeIf(v -> v.signum() <= 0);
        return remanentes;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal saldo(Long afiliadoId) {
        return movimientos.saldo(afiliados.obtener(afiliadoId).idTitular());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal deudaVencida(Long afiliadoId, LocalDate alDia) {
        return comprobantes.vencidosImpagos(afiliados.obtener(afiliadoId).idTitular(), alDia).stream()
                .map(Comprobante::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public EstadoCuentaDto estadoDeCuenta(Long afiliadoId, LocalDate desde, LocalDate hasta) {
        AfiliadoResumen af = afiliados.obtener(afiliadoId);
        AfiliadoResumen titular = af.titularId() == null ? af : afiliados.obtener(af.titularId());
        LocalDate d = desde != null ? desde : LocalDate.now().minusMonths(6).withDayOfMonth(1);
        LocalDate h = hasta != null ? hasta : LocalDate.now().plusMonths(1);
        BigDecimal saldoInicial = movimientos.saldoAntesDe(titular.id(), d);
        BigDecimal saldo = saldoInicial;
        List<MovimientoDto> lista = new ArrayList<>();
        for (MovimientoCuentaCorriente m : movimientos.findByAfiliadoIdOrderByFechaAscIdAsc(titular.id())) {
            if (m.getFecha().isBefore(d) || m.getFecha().isAfter(h)) {
                continue;
            }
            boolean debe = MovimientoCuentaCorriente.DEBE.equals(m.getTipo());
            saldo = debe ? saldo.add(m.getImporte()) : saldo.subtract(m.getImporte());
            lista.add(new MovimientoDto(m.getId(), m.getFecha(), m.getConcepto(), debe ? m.getImporte() : BigDecimal.ZERO,
                    debe ? BigDecimal.ZERO : m.getImporte(), saldo, m.getComprobanteId(), m.getPagoId()));
        }
        List<ComprobanteResumenDto> comps = comprobantes.findByAfiliadoIdOrderByFechaEmisionDesc(titular.id()).stream()
                .map(c -> new ComprobanteResumenDto(c.getId(), c.getNumero(), c.getPeriodo(), c.getFechaVencimiento(), c.getTotal(),
                        c.getSaldo(), c.getEstado()))
                .toList();
        BigDecimal deuda = comprobantes.vencidosImpagos(titular.id(), LocalDate.now()).stream()
                .map(Comprobante::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EstadoCuentaDto(titular.id(), titular.nroAfiliado(), titular.apellidoNombre(), titular.planCodigo(),
                titular.bloqueado(), d, h, saldoInicial, lista, saldo, deuda, comps);
    }
}
