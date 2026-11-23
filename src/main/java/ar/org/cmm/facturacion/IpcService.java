package ar.org.cmm.facturacion;

import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.facturacion.FacturacionDtos.ActualizacionCuotaRequest;
import ar.org.cmm.facturacion.FacturacionDtos.AplicarIpcRequest;
import ar.org.cmm.facturacion.FacturacionDtos.IpcDto;
import ar.org.cmm.facturacion.FacturacionDtos.IpcRequest;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoActualizacion;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoImportacionIpc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * IPC mensual para actualizar la cuota (T44, respuesta L1 del CMM): se trae de la API de series de tiempo
 * del Estado nacional (INDEC) o se carga a mano, y Liquidaciones confirma antes de aplicarlo a los valores de cuota.
 *
 * <p>La variación se calcula con el índice de nivel general: (índice del mes / índice del mes anterior − 1) × 100.
 */
@Service
public class IpcService {

    private static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("yyyyMM");

    private final IndiceIpcRepository repo;
    private final ValorCuotaService valores;
    private final ClienteDatosPublicos http;
    private final ObjectMapper json;
    private final AuditoriaService auditoria;
    private final String url;

    public IpcService(IndiceIpcRepository repo, ValorCuotaService valores, ClienteDatosPublicos http, ObjectMapper json,
                      AuditoriaService auditoria, @Value("${app.ipc.url}") String url) {
        this.repo = repo;
        this.valores = valores;
        this.http = http;
        this.json = json;
        this.auditoria = auditoria;
        this.url = url;
    }

    @Transactional(readOnly = true)
    public List<IpcDto> ultimos() {
        return repo.findTop24ByOrderByPeriodoDesc().stream()
                .map(i -> new IpcDto(i.getPeriodo(), i.getVariacion(), i.getOrigen(), i.getAplicadoVigencia(),
                        i.getUpdatedBy() != null ? i.getUpdatedBy() : i.getCreatedBy()))
                .toList();
    }

    @Transactional
    public void cargarManual(IpcRequest req) {
        String per = GeneracionCuotasService.parsearPeriodo(req.periodo()).format(PERIODO);
        IndiceIpc i = repo.findById(per).orElseGet(IndiceIpc::new);
        if (i.getAplicadoVigencia() != null) {
            throw new ReglaNegocioException("El IPC de " + per + " ya se aplicó a las cuotas: no se puede modificar.");
        }
        i.setPeriodo(per);
        i.setVariacion(req.variacion().setScale(3, RoundingMode.HALF_UP));
        i.setOrigen("MANUAL");
        repo.save(i);
        auditoria.registrar("indice_ipc", 0L, "CARGA_MANUAL", per + ": " + req.variacion() + "%");
    }

    /** Consulta la API y agrega los meses que falten. No pisa lo cargado a mano ni lo ya aplicado. */
    @Transactional
    public ResultadoImportacionIpc traerDeApi() {
        Map<String, BigDecimal> variaciones = variacionesDesdeSerie(http.get(url, "el IPC del INDEC"), json);
        int agregados = 0, existentes = 0;
        String ultimo = null;
        for (Map.Entry<String, BigDecimal> e : variaciones.entrySet()) {
            if (repo.existsById(e.getKey())) {
                existentes++;
            } else {
                IndiceIpc i = new IndiceIpc();
                i.setPeriodo(e.getKey());
                i.setVariacion(e.getValue());
                i.setOrigen("API");
                repo.save(i);
                agregados++;
            }
            ultimo = e.getKey();
        }
        auditoria.registrar("indice_ipc", 0L, "IMPORTACION", agregados + " meses agregados, último publicado " + ultimo);
        return new ResultadoImportacionIpc(agregados, existentes, ultimo);
    }

    /** Aplica la variación de un mes a los valores de cuota, con la vigencia que confirma Liquidaciones. */
    @Transactional
    public ResultadoActualizacion aplicar(String periodo, AplicarIpcRequest req) {
        String per = GeneracionCuotasService.parsearPeriodo(periodo).format(PERIODO);
        IndiceIpc i = repo.findById(per).orElseThrow(() -> new RecursoNoEncontradoException("IPC del período", per));
        if (i.getAplicadoVigencia() != null) {
            throw new ReglaNegocioException("El IPC de " + mesAnio(per) + " ya se aplicó con vigencia desde "
                    + i.getAplicadoVigencia().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
        }
        if (i.getVariacion().signum() == 0) {
            throw new ReglaNegocioException("La variación del IPC de " + mesAnio(per) + " es 0: no hay nada que aplicar.");
        }
        String motivo = "IPC " + mesAnio(per) + ": " + i.getVariacion().stripTrailingZeros().toPlainString().replace('.', ',')
                + " % (INDEC)";
        ResultadoActualizacion r = valores.actualizarPorPorcentaje(
                new ActualizacionCuotaRequest(i.getVariacion(), req.vigenteDesde(), req.planIds(), motivo));
        if (r.planesActualizados() > 0) {
            i.setAplicadoVigencia(req.vigenteDesde());
        }
        return r;
    }

    /**
     * Lee la respuesta de la API de series de tiempo ({"data":[["2026-09-01", 9876.5], ...]}) y devuelve
     * la variación mensual en % por período AAAAMM, ordenada del más viejo al más nuevo.
     */
    static Map<String, BigDecimal> variacionesDesdeSerie(String cuerpo, ObjectMapper json) {
        TreeMap<YearMonth, BigDecimal> indices = new TreeMap<>();
        try {
            JsonNode data = json.readTree(cuerpo).get("data");
            if (data == null || !data.isArray()) {
                throw new ReglaNegocioException("La respuesta del IPC no tiene el formato esperado.");
            }
            for (JsonNode fila : data) {
                if (fila.size() < 2 || fila.get(1).isNull()) {
                    continue;
                }
                indices.put(YearMonth.from(LocalDate.parse(fila.get(0).asText().substring(0, 10))), fila.get(1).decimalValue());
            }
        } catch (ReglaNegocioException e) {
            throw e;
        } catch (Exception e) {
            throw new ReglaNegocioException("No se pudo leer la respuesta del IPC: " + e.getMessage());
        }
        Map<String, BigDecimal> res = new TreeMap<>();
        YearMonth anterior = null;
        for (Map.Entry<YearMonth, BigDecimal> e : indices.entrySet()) {
            if (anterior != null && anterior.plusMonths(1).equals(e.getKey()) && indices.get(anterior).signum() > 0) {
                BigDecimal v = e.getValue().divide(indices.get(anterior), 10, RoundingMode.HALF_UP)
                        .subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).setScale(3, RoundingMode.HALF_UP);
                res.put(e.getKey().format(PERIODO), v);
            }
            anterior = e.getKey();
        }
        if (res.isEmpty()) {
            throw new ReglaNegocioException("La API del IPC no devolvió dos meses consecutivos para calcular la variación.");
        }
        return res;
    }

    private static String mesAnio(String per) {
        return per.substring(4) + "/" + per.substring(0, 4);
    }
}
