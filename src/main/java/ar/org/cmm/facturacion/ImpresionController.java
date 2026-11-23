package ar.org.cmm.facturacion;

import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;
import ar.org.cmm.comun.reportes.ExportadorReportes;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Descargas en PDF (para imprimir) o Excel de las pantallas de cuotas, cuenta corriente y morosos (T47). */
@RestController
@RequestMapping("/api/facturacion/imprimir")
@RequiereRol({Rol.LIQUIDACIONES, Rol.ADMINISTRACION})
public class ImpresionController {

    private final ImpresionFacturacionService impresion;
    private final ExportadorReportes exportador;

    public ImpresionController(ImpresionFacturacionService impresion, ExportadorReportes exportador) {
        this.impresion = impresion;
        this.exportador = exportador;
    }

    @GetMapping("/cuenta-corriente/{afiliadoId}")
    public ResponseEntity<byte[]> cuentaCorriente(@PathVariable Long afiliadoId, @RequestParam(defaultValue = "pdf") String formato,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return exportador.descargar(impresion.estadoDeCuenta(afiliadoId, desde, hasta), formato, "estado-de-cuenta-" + afiliadoId);
    }

    @GetMapping("/morosos")
    public ResponseEntity<byte[]> morosos(@RequestParam(defaultValue = "pdf") String formato,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate alDia) {
        LocalDate d = alDia != null ? alDia : LocalDate.now();
        return exportador.descargar(impresion.morosos(d), formato, "deudores-" + d);
    }

    @GetMapping("/resumen")
    public ResponseEntity<byte[]> resumen(@RequestParam String periodo, @RequestParam(defaultValue = "pdf") String formato) {
        return exportador.descargar(impresion.resumen(periodo), formato, "resumen-facturacion-" + periodo.replace("-", ""));
    }

    @GetMapping("/comprobantes/{id}")
    public ResponseEntity<byte[]> comprobante(@PathVariable Long id, @RequestParam(defaultValue = "pdf") String formato) {
        return exportador.descargar(impresion.comprobante(id), formato, "comprobante-" + id);
    }

    @GetMapping("/coseguros")
    public ResponseEntity<byte[]> coseguros(@RequestParam String desde, @RequestParam String hasta,
                                            @RequestParam(required = false) String estado, @RequestParam(required = false) Long afiliadoId,
                                            @RequestParam(defaultValue = "pdf") String formato) {
        return exportador.descargar(impresion.coseguros(desde, hasta, estado, afiliadoId), formato,
                "coseguros-" + desde.replace("-", "") + "-" + hasta.replace("-", ""));
    }
}
