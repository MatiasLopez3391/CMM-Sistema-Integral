package ar.org.cmm.tablero;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Indicadores de la pantalla de inicio. Es una capa de solo lectura que consulta varias tablas
 * con SQL agregado (reporte transversal); no modifica datos de ningún módulo.
 */
@RestController
@RequestMapping("/api/tablero")
public class TableroController {

    private final JdbcTemplate jdbc;

    public TableroController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public Map<String, Object> indicadores() {
        Date hoy = Date.valueOf(LocalDate.now());
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("fecha", LocalDate.now());
        r.put("afiliadosActivos", jdbc.queryForObject("select count(*) from afiliado where estado = 'ACTIVO'", Long.class));
        r.put("gruposActivos", jdbc.queryForObject("select count(*) from afiliado where estado = 'ACTIVO' and titular_id is null", Long.class));
        r.put("gruposBloqueados", jdbc.queryForObject(
                "select count(*) from afiliado where estado = 'ACTIVO' and titular_id is null and bloqueado = true", Long.class));
        r.put("deudaVencida", jdbc.queryForObject(
                "select coalesce(sum(saldo), 0) from comprobante where saldo > 0 and estado <> 'ANULADO' and fecha_vencimiento < ?",
                BigDecimal.class, hoy));
        r.put("gruposMorosos", jdbc.queryForObject(
                "select count(distinct afiliado_id) from comprobante where saldo > 0 and estado <> 'ANULADO' and fecha_vencimiento < ?",
                Long.class, hoy));
        String ultimoPeriodo = jdbc.query("select max(periodo) from comprobante", rs -> rs.next() ? rs.getString(1) : null);
        r.put("ultimoPeriodo", ultimoPeriodo);
        if (ultimoPeriodo != null) {
            r.put("ultimoPeriodoTotal", jdbc.queryForObject("select coalesce(sum(total), 0) from comprobante where periodo = ?",
                    BigDecimal.class, ultimoPeriodo));
            r.put("ultimoPeriodoCobrado", jdbc.queryForObject("select coalesce(sum(total - saldo), 0) from comprobante where periodo = ?",
                    BigDecimal.class, ultimoPeriodo));
        }
        List<Map<String, Object>> evolucion = jdbc.queryForList("""
                select periodo, count(*) as comprobantes, sum(total) as facturado, sum(total - saldo) as cobrado
                from comprobante group by periodo order by periodo desc limit 6""");
        java.util.Collections.reverse(evolucion);
        r.put("evolucion", evolucion);
        r.put("cobradoHoy", jdbc.queryForObject(
                "select coalesce(sum(importe), 0) from pago where estado = 'IMPUTADO' and fecha_pago = ?", BigDecimal.class, hoy));
        r.put("lotesPendientes", jdbc.queryForObject("select count(*) from lote_cobro where estado = 'PREVISUALIZADO'", Long.class));
        return r;
    }
}
