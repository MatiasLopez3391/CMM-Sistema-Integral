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
 * Imputación automática: cada pago (o nota de crédito) cancela primero la deuda más antigua.
 */
@Service
public class CuentaCorrienteServicioImpl implements CuentaCorrienteServicio {

    private final MovimientoCuentaCorrienteRepository movimientos;
    private final ComprobanteRepository comprobantes;
    private final ImputacionRepository imputaciones;
    private final AplicacionCreditoRepository aplicaciones;
    private final AfiliadoConsulta afiliados;

    public CuentaCorrienteServicioImpl(MovimientoCuentaCorrienteRepository movimientos, ComprobanteRepository comprobantes,
                                       ImputacionRepository imputaciones, AplicacionCreditoRepository aplicaciones,
                                       AfiliadoConsulta afiliados) {
        this.movimientos = movimientos;
        this.comprobantes = comprobantes;
        this.imputaciones = imputaciones;
        this.aplicaciones = aplicaciones;
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
     * Aplica los créditos disponibles del grupo (pagos con remanente y notas de crédito sin aplicar) a los
     * comprobantes pendientes, del más antiguo al más nuevo. Se llama al registrar un pago, al emitir una
     * nota de crédito y al generar una cuota nueva (para consumir lo que quedó a favor).
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
        BigDecimal total = BigDecimal.ZERO;
        int idx = 0;
        for (Credito cr : creditosDisponibles(titularId)) {
            if (soloPagoId != null && !(cr.esPago() && soloPagoId.equals(cr.id()))) {
                continue;
            }
            BigDecimal disponible = cr.disponible();
            while (disponible.signum() > 0 && idx < pendientes.size()) {
                Comprobante c = pendientes.get(idx);
                BigDecimal aplicar = disponible.min(c.getSaldo());
                registrarAplicacion(cr, c, aplicar);
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

    /** Aplica una nota de crédito a un comprobante puntual (el que corrige), hasta su saldo. */
    @Transactional
    public BigDecimal aplicarNotaA(Comprobante nota, Comprobante destino, BigDecimal importe) {
        BigDecimal aplicar = importe.min(destino.getSaldo());
        if (aplicar.signum() <= 0 || Comprobante.ANULADO.equals(destino.getEstado())) {
            return BigDecimal.ZERO;
        }
        registrarAplicacion(new Credito(false, nota.getId(), nota.getFechaEmision(), importe), destino, aplicar);
        return aplicar;
    }

    private void registrarAplicacion(Credito cr, Comprobante c, BigDecimal aplicar) {
        c.aplicarPago(aplicar);
        // Fecha: la del crédito, o la de emisión del comprobante si el crédito es anterior.
        LocalDate fecha = cr.fecha().isBefore(c.getFechaEmision()) ? c.getFechaEmision() : cr.fecha();
        if (cr.esPago()) {
            Imputacion imp = new Imputacion();
            imp.setPagoId(cr.id());
            imp.setComprobanteId(c.getId());
            imp.setImporte(aplicar);
            imp.setFecha(fecha);
            imputaciones.save(imp);
        } else {
            AplicacionCredito ap = new AplicacionCredito();
            ap.setNotaCreditoId(cr.id());
            ap.setComprobanteId(c.getId());
            ap.setImporte(aplicar);
            ap.setFecha(fecha);
            aplicaciones.save(ap);
        }
    }

    /** Crédito a favor del grupo: un pago o una nota de crédito con importe todavía sin aplicar. */
    private record Credito(boolean esPago, Long id, LocalDate fecha, BigDecimal disponible) {
    }

    /** Pagos y notas de crédito del titular con importe sin aplicar, en orden cronológico. */
    private List<Credito> creditosDisponibles(Long titularId) {
        Map<String, Credito> creditos = new java.util.LinkedHashMap<>();
        for (MovimientoCuentaCorriente m : movimientos.findByAfiliadoIdOrderByFechaAscIdAsc(titularId)) {
            if (!MovimientoCuentaCorriente.HABER.equals(m.getTipo())) {
                continue;
            }
            boolean pago = m.getPagoId() != null;
            Long id = pago ? m.getPagoId() : m.getComprobanteId();
            if (id == null) {
                continue;
            }
            creditos.merge((pago ? "P" : "N") + id, new Credito(pago, id, m.getFecha(), m.getImporte()),
                    (a, b) -> new Credito(a.esPago(), a.id(), a.fecha(), a.disponible().add(b.disponible())));
        }
        if (creditos.isEmpty()) {
            return List.of();
        }
        Map<String, BigDecimal> usado = new HashMap<>();
        List<Long> pagos = creditos.values().stream().filter(Credito::esPago).map(Credito::id).toList();
        List<Long> notas = creditos.values().stream().filter(c -> !c.esPago()).map(Credito::id).toList();
        if (!pagos.isEmpty()) {
            imputaciones.findByPagoIdIn(pagos).forEach(i -> usado.merge("P" + i.getPagoId(), i.getImporte(), BigDecimal::add));
        }
        if (!notas.isEmpty()) {
            aplicaciones.findByNotaCreditoIdIn(notas).forEach(a -> usado.merge("N" + a.getNotaCreditoId(), a.getImporte(), BigDecimal::add));
        }
        List<Credito> res = new ArrayList<>();
        creditos.forEach((k, c) -> {
            BigDecimal resto = c.disponible().subtract(usado.getOrDefault(k, BigDecimal.ZERO));
            if (resto.signum() > 0) {
                res.add(new Credito(c.esPago(), c.id(), c.fecha(), resto));
            }
        });
        return res;
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
                .map(c -> new ComprobanteResumenDto(c.getId(), c.getTipo(), c.getNumero(), c.getPeriodo(), c.getFechaVencimiento(),
                        c.getTotal(), c.getSaldo(), c.getEstado()))
                .toList();
        BigDecimal deuda = comprobantes.vencidosImpagos(titular.id(), LocalDate.now()).stream()
                .map(Comprobante::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EstadoCuentaDto(titular.id(), titular.nroAfiliado(), titular.apellidoNombre(), titular.planCodigo(),
                titular.bloqueado(), d, h, saldoInicial, lista, saldo, deuda, comps);
    }
}
