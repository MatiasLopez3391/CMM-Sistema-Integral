package ar.org.cmm.cobertura;

import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;
import ar.org.cmm.contratos.CoseguroServicio;
import ar.org.cmm.contratos.CoseguroServicio.CalculoCoseguro;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Coseguro (H12). Es solo lectura y lo consulta también Mesa de Entradas al emitir órdenes. */
@RestController
@RequestMapping("/api/cobertura/coseguro")
@RequiereRol({Rol.ADMINISTRACION, Rol.LIQUIDACIONES, Rol.MESA_ENTRADAS, Rol.MEDICO_AUDITOR, Rol.IT})
public class CoseguroController {

    private final CoseguroServicio coseguro; // el contrato, no la implementación

    public CoseguroController(CoseguroServicio coseguro) {
        this.coseguro = coseguro;
    }

    @GetMapping
    public CalculoCoseguro calcular(@RequestParam Long prestadorId, @RequestParam Long practicaId, @RequestParam Long planId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return coseguro.calcular(prestadorId, practicaId, planId, fecha != null ? fecha : LocalDate.now());
    }

    @GetMapping("/simulador")
    public List<CalculoCoseguro> simulador(@RequestParam Long prestadorId, @RequestParam Long practicaId,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return coseguro.simularTodosLosPlanes(prestadorId, practicaId, fecha != null ? fecha : LocalDate.now());
    }
}
