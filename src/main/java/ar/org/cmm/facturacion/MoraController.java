package ar.org.cmm.facturacion;

import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;
import ar.org.cmm.facturacion.FacturacionDtos.BloqueoMoraRequest;
import ar.org.cmm.facturacion.FacturacionDtos.ProcesoMoraDto;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoBloqueoMora;
import ar.org.cmm.facturacion.FacturacionDtos.TextoAvisoRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/** Proceso de mora (bloqueo confirmado, baja manual) y texto del aviso de deuda (T51, T52). */
@RestController
@RequestMapping("/api/facturacion/mora")
@RequiereRol({Rol.LIQUIDACIONES, Rol.ADMINISTRACION})
public class MoraController {

    private final MoraService mora;
    private final ParametroService parametros;

    public MoraController(MoraService mora, ParametroService parametros) {
        this.mora = mora;
        this.parametros = parametros;
    }

    @GetMapping
    public ProcesoMoraDto proceso(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return mora.proceso(fecha != null ? fecha : LocalDate.now());
    }

    @PostMapping("/bloquear")
    public ResultadoBloqueoMora bloquear(@Valid @RequestBody BloqueoMoraRequest req) {
        return mora.bloquear(req.afiliadoIds(), req.fecha() != null ? req.fecha() : LocalDate.now());
    }

    @GetMapping("/texto-aviso")
    public Map<String, String> textoAviso() {
        return Map.of("asunto", parametros.texto(ParametroService.AVISO_ASUNTO), "texto", parametros.textoLargo(ParametroService.AVISO_TEXTO));
    }

    @PutMapping("/texto-aviso")
    public void guardarTextoAviso(@Valid @RequestBody TextoAvisoRequest req) {
        parametros.actualizar(ParametroService.AVISO_ASUNTO, req.asunto());
        parametros.actualizar(ParametroService.AVISO_TEXTO, req.texto().replace("\r", "").replace("\n", "\\n"));
    }
}
