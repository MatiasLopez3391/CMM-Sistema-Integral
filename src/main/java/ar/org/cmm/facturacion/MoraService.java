package ar.org.cmm.facturacion;

import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.contratos.BloqueoServicio;
import ar.org.cmm.facturacion.FacturacionDtos.MorosoDto;
import ar.org.cmm.facturacion.FacturacionDtos.ProcesoMoraDto;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoBloqueoMora;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Proceso de mora del mes (T52, respuesta L10 del CMM): desde el día 20 a quien no pagó la cuota se le dejan de
 * dar órdenes (bloqueo) y con 3 cuotas impagas se lo da de baja.
 *
 * <p>Decisión del equipo: el sistema arma las listas con lo que debe cada grupo; el bloqueo lo confirma un usuario
 * y la baja es siempre manual, desde la ficha del afiliado. Este servicio nunca da de baja.
 */
@Service
public class MoraService {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReportesFacturacionService reportes;
    private final ParametroService parametros;
    private final BloqueoServicio bloqueos;
    private final AuditoriaService auditoria;

    public MoraService(ReportesFacturacionService reportes, ParametroService parametros, BloqueoServicio bloqueos,
                       AuditoriaService auditoria) {
        this.reportes = reportes;
        this.parametros = parametros;
        this.bloqueos = bloqueos;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public ProcesoMoraDto proceso(LocalDate fecha) {
        int diaBloqueo = parametros.entero(ParametroService.DIA_BLOQUEO);
        int cuotasBaja = parametros.entero(ParametroService.CUOTAS_BAJA);
        List<MorosoDto> morosos = reportes.morosos(fecha);
        List<MorosoDto> paraBloquear = morosos.stream().filter(m -> !m.bloqueado()).toList();
        List<MorosoDto> paraBaja = morosos.stream().filter(m -> m.cuotasVencidas() >= cuotasBaja).toList();
        return new ProcesoMoraDto(fecha, diaBloqueo, cuotasBaja, fecha.getDayOfMonth() < diaBloqueo, paraBloquear, paraBaja);
    }

    /** Bloquea los grupos que confirmó el usuario. Solo los que siguen morosos y no estaban bloqueados. */
    @Transactional
    public ResultadoBloqueoMora bloquear(List<Long> afiliadoIds, LocalDate fecha) {
        if (afiliadoIds == null || afiliadoIds.isEmpty()) {
            throw new ReglaNegocioException("Seleccione al menos un grupo para bloquear.");
        }
        Map<Long, MorosoDto> vigentes = reportes.morosos(fecha).stream()
                .collect(Collectors.toMap(MorosoDto::afiliadoId, Function.identity()));
        NumberFormat moneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
        int bloqueados = 0;
        List<String> omitidos = new ArrayList<>();
        for (Long id : afiliadoIds) {
            MorosoDto m = vigentes.get(id);
            if (m == null) {
                omitidos.add("#" + id + ": ya no tiene deuda vencida");
                continue;
            }
            if (m.bloqueado() || bloqueos.estaBloqueado(id)) {
                omitidos.add(m.nroAfiliado() + ": ya estaba bloqueado");
                continue;
            }
            bloqueos.bloquear(id, "Bloqueado por falta de pago", "Proceso de mora del " + fecha.format(F) + ": "
                    + m.cuotasVencidas() + " cuota(s) vencida(s) por " + moneda.format(m.deudaVencida()));
            bloqueados++;
        }
        auditoria.registrar("mora", 0L, "BLOQUEO_PROCESO", fecha + ": " + bloqueados + " grupos bloqueados");
        return new ResultadoBloqueoMora(bloqueados, omitidos);
    }
}
