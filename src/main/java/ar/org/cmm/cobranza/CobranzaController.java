package ar.org.cmm.cobranza;

import ar.org.cmm.cobranza.CobranzaDtos.CajaDiariaDto;
import ar.org.cmm.cobranza.CobranzaDtos.LayoutDto;
import ar.org.cmm.cobranza.CobranzaDtos.LoteDto;
import ar.org.cmm.cobranza.CobranzaDtos.MedioDto;
import ar.org.cmm.cobranza.CobranzaDtos.PagoDto;
import ar.org.cmm.cobranza.CobranzaDtos.ResultadoConfirmacion;
import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cobranza")
@RequiereRol({Rol.LIQUIDACIONES, Rol.ADMINISTRACION})
public class CobranzaController {

    private final CobranzaService servicio;

    public CobranzaController(CobranzaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/medios")
    public List<MedioDto> medios() {
        return servicio.medios();
    }

    // ---- Archivos de entes de cobro
    @PostMapping(value = "/lotes/previsualizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LoteDto previsualizar(@RequestParam Long medioId, @RequestPart("archivo") MultipartFile archivo) {
        return servicio.previsualizar(medioId, archivo);
    }

    @PostMapping("/lotes/{id}/confirmar")
    public ResultadoConfirmacion confirmar(@PathVariable Long id) {
        return servicio.confirmar(id);
    }

    @PostMapping("/lotes/{id}/descartar")
    public void descartar(@PathVariable Long id) {
        servicio.descartar(id);
    }

    @GetMapping("/lotes")
    public List<LoteDto> lotes() {
        return servicio.lotesRecientes();
    }

    @GetMapping("/lotes/{id}")
    public LoteDto lote(@PathVariable Long id) {
        return servicio.lote(id);
    }

    // ---- Ventanilla
    @PostMapping(value = "/pagos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PagoDto pagoManual(@RequestParam Long afiliadoId,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                              @RequestParam BigDecimal importe,
                              @RequestParam(required = false) String medio,
                              @RequestParam(required = false) String observaciones,
                              @RequestPart("comprobante") MultipartFile comprobante) {
        return servicio.registrarPagoManual(afiliadoId, fecha, importe, medio, observaciones, comprobante);
    }

    @GetMapping("/pagos")
    public List<PagoDto> pagosManuales() {
        return servicio.pagosManualesRecientes();
    }

    @GetMapping("/pagos/afiliado/{afiliadoId}")
    public List<PagoDto> pagosDeAfiliado(@PathVariable Long afiliadoId) {
        return servicio.pagosDeAfiliado(afiliadoId);
    }

    @GetMapping("/pagos/{id}/comprobante")
    public ResponseEntity<Resource> comprobante(@PathVariable Long id) {
        Pago p = servicio.pago(id);
        Path ruta = servicio.rutaAdjunto(p);
        if (!Files.exists(ruta)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(p.getAdjuntoTipo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(p.getAdjuntoNombre()).build().toString())
                .body(new FileSystemResource(ruta));
    }

    // ---- Caja diaria
    @GetMapping("/caja")
    public CajaDiariaDto caja(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return servicio.cajaDiaria(fecha != null ? fecha : LocalDate.now());
    }

    // ---- Diseños de archivo
    @GetMapping("/layouts")
    public List<LayoutDto> layouts() {
        return servicio.layouts();
    }

    @PutMapping("/layouts/{id}")
    @RequiereRol({Rol.IT, Rol.LIQUIDACIONES})
    public LayoutDto guardarLayout(@PathVariable Long id, @RequestBody LayoutDto dto) {
        return servicio.guardarLayout(id, dto);
    }
}
