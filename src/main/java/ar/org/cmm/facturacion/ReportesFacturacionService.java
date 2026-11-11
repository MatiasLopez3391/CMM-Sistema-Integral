package ar.org.cmm.facturacion;

import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.AfiliadoConsulta.AfiliadoResumen;
import ar.org.cmm.contratos.PlanConsulta;
import ar.org.cmm.contratos.PlanConsulta.PlanResumen;
import ar.org.cmm.facturacion.FacturacionDtos.ComprobanteDto;
import ar.org.cmm.facturacion.FacturacionDtos.ImputacionDto;
import ar.org.cmm.facturacion.FacturacionDtos.ItemDto;
import ar.org.cmm.facturacion.FacturacionDtos.MorosoDto;
import ar.org.cmm.facturacion.FacturacionDtos.ResumenConceptoDto;
import ar.org.cmm.facturacion.FacturacionDtos.ResumenFacturacionDto;
import ar.org.cmm.facturacion.FacturacionDtos.ResumenPlanDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Morosos, resumen de facturación del período y detalle de comprobantes. */
@Service
public class ReportesFacturacionService {

    private final ComprobanteRepository comprobantes;
    private final ImputacionRepository imputaciones;
    private final AfiliadoConsulta afiliados;
    private final PlanConsulta planes;
    private final ParametroService parametros;

    public ReportesFacturacionService(ComprobanteRepository comprobantes, ImputacionRepository imputaciones,
                                      AfiliadoConsulta afiliados, PlanConsulta planes, ParametroService parametros) {
        this.comprobantes = comprobantes;
        this.imputaciones = imputaciones;
        this.afiliados = afiliados;
        this.planes = planes;
        this.parametros = parametros;
    }

    /** Afiliados con cuotas vencidas impagas al día indicado, de mayor a menor deuda. */
    @Transactional(readOnly = true)
    public List<MorosoDto> morosos(LocalDate alDia) {
        int minimo = parametros.entero(ParametroService.CUOTAS_MINIMAS_MOROSO);
        Map<Long, List<Comprobante>> porAfiliado = new LinkedHashMap<>();
        for (Comprobante c : comprobantes.todosVencidosImpagos(alDia)) {
            porAfiliado.computeIfAbsent(c.getAfiliadoId(), k -> new ArrayList<>()).add(c);
        }
        List<MorosoDto> res = new ArrayList<>();
        for (Map.Entry<Long, List<Comprobante>> e : porAfiliado.entrySet()) {
            List<Comprobante> lista = e.getValue();
            if (lista.size() < minimo) {
                continue;
            }
            AfiliadoResumen a = afiliados.obtener(e.getKey());
            BigDecimal deuda = lista.stream().map(Comprobante::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
            LocalDate masAntiguo = lista.get(0).getFechaVencimiento();
            res.add(new MorosoDto(a.id(), a.nroAfiliado(), a.apellidoNombre(), a.planCodigo(), a.email(), lista.size(), deuda,
                    masAntiguo, ChronoUnit.DAYS.between(masAntiguo, alDia), a.bloqueado()));
        }
        res.sort(Comparator.comparing(MorosoDto::deudaVencida).reversed());
        return res;
    }

    @Transactional(readOnly = true)
    public ResumenFacturacionDto resumen(String periodo) {
        String per = GeneracionCuotasService.parsearPeriodo(periodo).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        List<Comprobante> lista = comprobantes.findByPeriodoAndTipo(per, Comprobante.TIPO_CUOTA);
        BigDecimal total = BigDecimal.ZERO, pendiente = BigDecimal.ZERO;
        Map<String, ResumenConceptoAcum> conceptos = new LinkedHashMap<>();
        Map<Long, ResumenPlanAcum> porPlan = new HashMap<>();
        for (Comprobante c : lista) {
            total = total.add(c.getTotal());
            pendiente = pendiente.add(c.getSaldo());
            for (ComprobanteItem i : c.getItems()) {
                CodigoServicio cs = i.getCodigoServicio();
                conceptos.computeIfAbsent(cs.getCodigo(), k -> new ResumenConceptoAcum(cs)).sumar(i.getCantidad(), i.getImporte());
            }
            Long planId = afiliados.obtener(c.getAfiliadoId()).planId();
            porPlan.computeIfAbsent(planId, k -> new ResumenPlanAcum()).sumar(c.getTotal(), c.getSaldo());
        }
        BigDecimal cobrado = total.subtract(pendiente);
        BigDecimal pct = total.signum() == 0 ? BigDecimal.ZERO
                : cobrado.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
        List<ResumenPlanDto> planesDto = new ArrayList<>();
        for (Map.Entry<Long, ResumenPlanAcum> e : porPlan.entrySet()) {
            PlanResumen p = planes.obtener(e.getKey());
            planesDto.add(new ResumenPlanDto(p.codigo(), p.nombre(), e.getValue().cantidad, e.getValue().total, e.getValue().pendiente));
        }
        planesDto.sort(Comparator.comparing(ResumenPlanDto::planCodigo));
        return new ResumenFacturacionDto(per, lista.size(), total, cobrado, pendiente, pct,
                conceptos.values().stream().map(ResumenConceptoAcum::dto).toList(), planesDto);
    }

    @Transactional(readOnly = true)
    public ComprobanteDto comprobante(Long id) {
        Comprobante c = comprobantes.findWithItemsById(id).orElseThrow(() -> new RecursoNoEncontradoException("Comprobante", id));
        AfiliadoResumen a = afiliados.obtener(c.getAfiliadoId());
        List<ItemDto> items = c.getItems().stream()
                .map(i -> new ItemDto(i.getCodigoServicio().getCodigo(), i.getDescripcion(), i.getCantidad(), i.getPrecioUnitario(),
                        i.getImporte(), i.getCodigoServicio().getCuentaContable().getCodigo()))
                .toList();
        List<ImputacionDto> imps = imputaciones.findByComprobanteId(id).stream()
                .map(i -> new ImputacionDto(i.getPagoId(), i.getFecha(), i.getImporte())).toList();
        return new ComprobanteDto(c.getId(), c.getTipo(), c.getNumero(), a.id(), a.nroAfiliado(), a.apellidoNombre(), c.getPeriodo(),
                c.getFechaEmision(), c.getFechaVencimiento(), c.getTotal(), c.getSaldo(), c.getEstado(), items, imps);
    }

    @Transactional(readOnly = true)
    public List<String> periodosGenerados() {
        return comprobantes.periodosGenerados();
    }

    private static final class ResumenConceptoAcum {
        private final CodigoServicio cs;
        private long cantidad;
        private BigDecimal importe = BigDecimal.ZERO;

        ResumenConceptoAcum(CodigoServicio cs) {
            this.cs = cs;
        }

        void sumar(int cant, BigDecimal imp) {
            cantidad += cant;
            importe = importe.add(imp);
        }

        ResumenConceptoDto dto() {
            return new ResumenConceptoDto(cs.getCodigo(), cs.getDescripcion(), cs.getCuentaContable().getCodigo(), cantidad, importe);
        }
    }

    private static final class ResumenPlanAcum {
        private int cantidad;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal pendiente = BigDecimal.ZERO;

        void sumar(BigDecimal t, BigDecimal p) {
            cantidad++;
            total = total.add(t);
            pendiente = pendiente.add(p);
        }
    }
}
