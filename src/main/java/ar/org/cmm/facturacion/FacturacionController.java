package ar.org.cmm.facturacion;

import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;
import ar.org.cmm.facturacion.FacturacionDtos.ActualizacionCuotaRequest;
import ar.org.cmm.facturacion.FacturacionDtos.AvisoRequest;
import ar.org.cmm.facturacion.FacturacionDtos.CodigoServicioDto;
import ar.org.cmm.facturacion.FacturacionDtos.CodigoServicioRequest;
import ar.org.cmm.facturacion.FacturacionDtos.ComprobanteDto;
import ar.org.cmm.facturacion.FacturacionDtos.EstadoCuentaDto;
import ar.org.cmm.facturacion.FacturacionDtos.GeneracionRequest;
import ar.org.cmm.facturacion.FacturacionDtos.MorosoDto;
import ar.org.cmm.facturacion.FacturacionDtos.ParametroRequest;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoActualizacion;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoAvisos;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoGeneracion;
import ar.org.cmm.facturacion.FacturacionDtos.ResumenFacturacionDto;
import ar.org.cmm.facturacion.FacturacionDtos.ValorCuotaPlanDto;
import ar.org.cmm.facturacion.FacturacionDtos.ValorCuotaRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/facturacion")
@RequiereRol({Rol.LIQUIDACIONES, Rol.ADMINISTRACION})
public class FacturacionController {

    private final ValorCuotaService valores;
    private final GeneracionCuotasService generacion;
    private final CuentaCorrienteServicioImpl cuentaCorriente;
    private final ReportesFacturacionService reportes;
    private final ExportacionAsientosService exportacion;
    private final NotificacionService notificaciones;
    private final ParametroService parametros;
    private final CodigoServicioRepository codigos;
    private final CuentaContableRepository cuentas;

    public FacturacionController(ValorCuotaService valores, GeneracionCuotasService generacion,
                                 CuentaCorrienteServicioImpl cuentaCorriente, ReportesFacturacionService reportes,
                                 ExportacionAsientosService exportacion, NotificacionService notificaciones,
                                 ParametroService parametros, CodigoServicioRepository codigos, CuentaContableRepository cuentas) {
        this.valores = valores;
        this.generacion = generacion;
        this.cuentaCorriente = cuentaCorriente;
        this.reportes = reportes;
        this.exportacion = exportacion;
        this.notificaciones = notificaciones;
        this.parametros = parametros;
        this.codigos = codigos;
        this.cuentas = cuentas;
    }

    // ---- Valores de cuota
    @GetMapping("/valores-cuota")
    public List<ValorCuotaPlanDto> valoresCuota(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return valores.listar(fecha != null ? fecha : LocalDate.now());
    }

    @PostMapping("/valores-cuota/actualizar")
    public ResultadoActualizacion actualizarCuotas(@Valid @RequestBody ActualizacionCuotaRequest req) {
        return valores.actualizarPorPorcentaje(req);
    }

    @PostMapping("/valores-cuota")
    public void valorPuntual(@Valid @RequestBody ValorCuotaRequest req) {
        valores.guardarValorPuntual(req);
    }

    // ---- Generación de cuotas
    @PostMapping("/cuotas/simular")
    public ResultadoGeneracion simular(@Valid @RequestBody GeneracionRequest req) {
        return generacion.simular(req.periodo(), req.fechaEmision());
    }

    @PostMapping("/cuotas/generar")
    public ResultadoGeneracion generar(@Valid @RequestBody GeneracionRequest req) {
        return generacion.generar(req.periodo(), req.fechaEmision());
    }

    @GetMapping("/periodos")
    public List<String> periodos() {
        return reportes.periodosGenerados();
    }

    @GetMapping("/resumen")
    public ResumenFacturacionDto resumen(@RequestParam String periodo) {
        return reportes.resumen(periodo);
    }

    // ---- Comprobantes y cuenta corriente
    @GetMapping("/comprobantes/{id}")
    public ComprobanteDto comprobante(@PathVariable Long id) {
        return reportes.comprobante(id);
    }

    @GetMapping("/cuenta-corriente/{afiliadoId}")
    public EstadoCuentaDto estadoDeCuenta(@PathVariable Long afiliadoId,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return cuentaCorriente.estadoDeCuenta(afiliadoId, desde, hasta);
    }

    // ---- Morosos y avisos
    @GetMapping("/morosos")
    public List<MorosoDto> morosos(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate alDia) {
        return reportes.morosos(alDia != null ? alDia : LocalDate.now());
    }

    @PostMapping("/morosos/avisos")
    public ResultadoAvisos avisar(@Valid @RequestBody AvisoRequest req) {
        return notificaciones.avisarDeuda(req.afiliadoIds());
    }

    @GetMapping("/notificaciones")
    public Map<String, Object> notificaciones() {
        return Map.of("smtpConfigurado", notificaciones.smtpConfigurado(), "notificaciones", notificaciones.recientes());
    }

    // ---- Exportación a FEBBO
    @GetMapping("/asientos/exportar")
    public ResponseEntity<byte[]> exportarAsientos(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        ExportacionAsientosService.Exportacion e = exportacion.exportar(desde, hasta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(e.nombreArchivo()).build().toString())
                .header("X-Asientos", String.valueOf(e.asientos()))
                .header("X-Total-Debe", e.totalDebe().toPlainString())
                .header("X-Total-Haber", e.totalHaber().toPlainString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(e.contenido().getBytes(StandardCharsets.UTF_8));
    }

    // ---- Configuración
    @GetMapping("/parametros")
    public List<Parametro> parametros() {
        return parametros.todos();
    }

    @PutMapping("/parametros/{clave}")
    @RequiereRol({Rol.LIQUIDACIONES, Rol.IT})
    public void actualizarParametro(@PathVariable String clave, @Valid @RequestBody ParametroRequest req) {
        parametros.actualizar(clave, req.valor());
    }

    @GetMapping("/codigos-servicio")
    @Transactional(readOnly = true)
    public List<CodigoServicioDto> codigosServicio() {
        return codigos.findAllByOrderByCodigoAsc().stream()
                .map(c -> new CodigoServicioDto(c.getId(), c.getCodigo(), c.getDescripcion(), c.getTipo(),
                        c.getCuentaContable().getCodigo(), c.getCuentaContable().getNombre(), c.getImporteFijo(),
                        c.isIncluirEnCuota(), c.isActivo()))
                .toList();
    }

    @PutMapping("/codigos-servicio/{id}")
    @Transactional
    public void modificarCodigo(@PathVariable Long id, @Valid @RequestBody CodigoServicioRequest req) {
        CodigoServicio c = codigos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Código de servicio", id));
        c.setCuentaContable(cuentas.findById(req.cuentaContableId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable", req.cuentaContableId())));
        c.setDescripcion(req.descripcion().trim());
        c.setImporteFijo(req.importeFijo());
        c.setIncluirEnCuota(req.incluirEnCuota());
        c.setActivo(req.activo());
    }

    @GetMapping("/cuentas-contables")
    public List<CuentaContable> cuentasContables() {
        return cuentas.findAllByOrderByCodigoAsc();
    }
}
