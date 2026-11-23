package ar.org.cmm.facturacion;

import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.facturacion.FacturacionDtos.FeriadoDto;
import ar.org.cmm.facturacion.FacturacionDtos.FeriadoRequest;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoImportacionFeriados;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Calendario de días hábiles (T45). La cuota vence el día configurado o, si cae sábado, domingo o feriado,
 * el día hábil siguiente (respuesta L4 del CMM).
 */
@Service
public class CalendarioHabilService {

    private final FeriadoRepository repo;
    private final ClienteDatosPublicos http;
    private final ObjectMapper json;
    private final AuditoriaService auditoria;
    private final String urlFeriados;

    public CalendarioHabilService(FeriadoRepository repo, ClienteDatosPublicos http, ObjectMapper json, AuditoriaService auditoria,
                                  @Value("${app.feriados.url}") String urlFeriados) {
        this.repo = repo;
        this.http = http;
        this.json = json;
        this.auditoria = auditoria;
        this.urlFeriados = urlFeriados;
    }

    /** La misma fecha si es hábil; si no, el primer día hábil posterior. */
    @Transactional(readOnly = true)
    public LocalDate habilDesde(LocalDate fecha) {
        return habilDesde(fecha, repo::existsById);
    }

    /** Regla pura (sin base), para poder probarla aislada. */
    static LocalDate habilDesde(LocalDate fecha, Predicate<LocalDate> esFeriado) {
        LocalDate d = fecha;
        for (int i = 0; i < 31; i++) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY && !esFeriado.test(d)) {
                return d;
            }
            d = d.plusDays(1);
        }
        throw new ReglaNegocioException("No hay un día hábil en el mes siguiente a " + fecha + ": revise los feriados cargados.");
    }

    @Transactional(readOnly = true)
    public List<FeriadoDto> listar(int anio) {
        return repo.findByFechaBetweenOrderByFechaAsc(LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31)).stream()
                .map(f -> new FeriadoDto(f.getFecha(), f.getDescripcion(), f.getOrigen())).toList();
    }

    @Transactional
    public void guardar(FeriadoRequest req) {
        Feriado f = repo.findById(req.fecha()).orElseGet(Feriado::new);
        f.setFecha(req.fecha());
        f.setDescripcion(req.descripcion().trim());
        f.setOrigen("MANUAL");
        repo.save(f);
        auditoria.registrar("feriado", 0L, "ALTA", req.fecha() + " " + f.getDescripcion());
    }

    @Transactional
    public void eliminar(LocalDate fecha) {
        if (repo.existsById(fecha)) {
            repo.deleteById(fecha);
            auditoria.registrar("feriado", 0L, "BAJA", fecha.toString());
        }
    }

    /** Trae los feriados del año desde la API pública y agrega los que falten (no pisa los cargados a mano). */
    @Transactional
    public ResultadoImportacionFeriados importar(int anio) {
        if (anio < 2020 || anio > 2100) {
            throw new ReglaNegocioException("Año inválido: " + anio);
        }
        String cuerpo = http.get(urlFeriados.replace("{anio}", String.valueOf(anio)), "los feriados de " + anio);
        List<Feriado> leidos = parsear(cuerpo, json);
        int agregados = 0, existentes = 0;
        Set<LocalDate> vistos = new java.util.HashSet<>();
        for (Feriado f : leidos) {
            if (f.getFecha().getYear() != anio || !vistos.add(f.getFecha())) {
                continue;
            }
            if (repo.existsById(f.getFecha())) {
                existentes++;
            } else {
                repo.save(f);
                agregados++;
            }
        }
        auditoria.registrar("feriado", 0L, "IMPORTACION", anio + ": " + agregados + " agregados, " + existentes + " ya estaban");
        return new ResultadoImportacionFeriados(anio, agregados, existentes);
    }

    /** Formato de ArgentinaDatos: [{"fecha":"2026-01-01","tipo":"inamovible","nombre":"Año Nuevo"}, ...]. */
    static List<Feriado> parsear(String cuerpo, ObjectMapper json) {
        try {
            JsonNode raiz = json.readTree(cuerpo);
            if (!raiz.isArray()) {
                throw new ReglaNegocioException("La respuesta de feriados no tiene el formato esperado.");
            }
            List<Feriado> res = new ArrayList<>();
            for (JsonNode n : raiz) {
                if (!n.hasNonNull("fecha")) {
                    continue;
                }
                Feriado f = new Feriado();
                f.setFecha(LocalDate.parse(n.get("fecha").asText().substring(0, 10)));
                String nombre = n.hasNonNull("nombre") ? n.get("nombre").asText() : "Feriado";
                f.setDescripcion(nombre.length() > 150 ? nombre.substring(0, 150) : nombre);
                f.setOrigen("API");
                res.add(f);
            }
            return res;
        } catch (ReglaNegocioException e) {
            throw e;
        } catch (Exception e) {
            throw new ReglaNegocioException("No se pudo leer la respuesta de feriados: " + e.getMessage());
        }
    }
}
