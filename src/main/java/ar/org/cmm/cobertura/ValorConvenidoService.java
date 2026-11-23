package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.CoberturaDtos.ValorRequest;
import ar.org.cmm.cobertura.CoberturaDtos.ValorVigenteDto;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Valores convenidos con cada prestador, con vigencia. */
@Service
public class ValorConvenidoService {

    private final PrestadorRepository prestadores;
    private final PracticaRepository practicas;
    private final ValorConvenidoRepository valores;

    public ValorConvenidoService(PrestadorRepository prestadores, PracticaRepository practicas, ValorConvenidoRepository valores) {
        this.prestadores = prestadores;
        this.practicas = practicas;
        this.valores = valores;
    }

    /** Valores vigentes a una fecha para un prestador, con la vigencia anterior para mostrar el aumento. */
    @Transactional(readOnly = true)
    public List<ValorVigenteDto> valoresVigentes(Long prestadorId, LocalDate fecha) {
        prestadores.findById(prestadorId).orElseThrow(() -> new RecursoNoEncontradoException("Prestador", prestadorId));
        List<ValorConvenido> historial = valores.historial(prestadorId); // por práctica, vigencia desc
        List<ValorVigenteDto> resultado = new ArrayList<>();
        Set<Long> vistas = new HashSet<>();
        for (int i = 0; i < historial.size(); i++) {
            ValorConvenido v = historial.get(i);
            Long practicaId = v.getPractica().getId();
            if (vistas.contains(practicaId) || v.getVigenteDesde().isAfter(fecha)) {
                continue;
            }
            vistas.add(practicaId);
            ValorConvenido anterior = (i + 1 < historial.size() && historial.get(i + 1).getPractica().getId().equals(practicaId))
                    ? historial.get(i + 1) : null;
            BigDecimal variacion = null;
            if (anterior != null && anterior.getImporte().signum() > 0) {
                variacion = v.getImporte().subtract(anterior.getImporte())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(anterior.getImporte(), 2, RoundingMode.HALF_UP);
            }
            Practica pr = v.getPractica();
            resultado.add(new ValorVigenteDto(practicaId, pr.getCodigoCmm(), pr.getCodigoNomenclador(), pr.getDescripcion(),
                    pr.getRubro(), v.getVigenteDesde(), v.getImporte(),
                    anterior != null ? anterior.getVigenteDesde() : null,
                    anterior != null ? anterior.getImporte() : null, variacion));
        }
        return resultado;
    }

    @Transactional
    public void guardarValor(Long prestadorId, ValorRequest req, String origen) {
        Prestador prestador = prestadores.findById(prestadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Prestador", prestadorId));
        Practica practica = practicas.findById(req.practicaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Práctica", req.practicaId()));
        ValorConvenido v = valores.findByPrestadorIdAndPracticaIdAndVigenteDesde(prestadorId, practica.getId(), req.vigenteDesde())
                .orElseGet(ValorConvenido::new);
        v.setPrestador(prestador);
        v.setPractica(practica);
        v.setVigenteDesde(req.vigenteDesde());
        v.setImporte(req.importe().setScale(2, RoundingMode.HALF_UP));
        v.setOrigen(origen);
        valores.save(v);
    }
}
