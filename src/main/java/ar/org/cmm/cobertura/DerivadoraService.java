package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.CoberturaDtos.DerivadoraDto;
import ar.org.cmm.cobertura.CoberturaDtos.DerivadoraRequest;
import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.contratos.AfiliadoConsulta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** ABM de derivadoras (obras sociales de origen de los monotributistas). */
@Service
public class DerivadoraService {

    private final DerivadoraRepository derivadoras;
    private final AfiliadoConsulta afiliados; // contrato de F1: no se consulta su tabla directamente
    private final AuditoriaService auditoria;

    public DerivadoraService(DerivadoraRepository derivadoras, AfiliadoConsulta afiliados, AuditoriaService auditoria) {
        this.derivadoras = derivadoras;
        this.afiliados = afiliados;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<DerivadoraDto> listarDerivadoras() {
        Map<Long, Long> activos = afiliados.activosPorDerivadora();
        return derivadoras.findAllByOrderByNombreAsc().stream()
                .map(d -> new DerivadoraDto(d.getId(), d.getNombre(), d.getCuit(), d.getConvenio(), d.getVigenteDesde(),
                        d.getVigenteHasta(), d.isActiva(), activos.getOrDefault(d.getId(), 0L)))
                .toList();
    }

    @Transactional
    public Long guardarDerivadora(Long id, DerivadoraRequest req) {
        Derivadora d = id == null ? new Derivadora() : derivadoras.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Derivadora", id));
        if (req.vigenteDesde() != null && req.vigenteHasta() != null && req.vigenteHasta().isBefore(req.vigenteDesde())) {
            throw new ReglaNegocioException("La vigencia 'hasta' no puede ser anterior a 'desde'.");
        }
        d.setNombre(req.nombre().trim());
        d.setCuit(Cuit.normalizar(req.cuit()));
        d.setConvenio(req.convenio());
        d.setVigenteDesde(req.vigenteDesde());
        d.setVigenteHasta(req.vigenteHasta());
        d.setActiva(id == null || req.activa());
        Long nuevoId = derivadoras.save(d).getId();
        auditoria.registrar("derivadora", nuevoId, id == null ? "ALTA" : "MODIFICACION", d.getNombre());
        return nuevoId;
    }
}
