package ar.org.cmm.facturacion;

import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;
import ar.org.cmm.facturacion.FacturacionDtos.AplicarIpcRequest;
import ar.org.cmm.facturacion.FacturacionDtos.FeriadoDto;
import ar.org.cmm.facturacion.FacturacionDtos.FeriadoRequest;
import ar.org.cmm.facturacion.FacturacionDtos.IpcDto;
import ar.org.cmm.facturacion.FacturacionDtos.IpcRequest;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoActualizacion;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoImportacionFeriados;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoImportacionIpc;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Feriados (vencimiento hábil, T45) e IPC mensual para actualizar la cuota (T44). */
@RestController
@RequestMapping("/api/facturacion")
@RequiereRol({Rol.LIQUIDACIONES, Rol.ADMINISTRACION})
public class CalendarioIpcController {

    private final CalendarioHabilService calendario;
    private final IpcService ipc;

    public CalendarioIpcController(CalendarioHabilService calendario, IpcService ipc) {
        this.calendario = calendario;
        this.ipc = ipc;
    }

    // ---- Feriados
    @GetMapping("/feriados")
    public List<FeriadoDto> feriados(@RequestParam int anio) {
        return calendario.listar(anio);
    }

    @GetMapping("/feriados/vencimiento")
    public Map<String, LocalDate> vencimiento(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return Map.of("fecha", fecha, "habil", calendario.habilDesde(fecha));
    }

    @PostMapping("/feriados")
    @RequiereRol({Rol.LIQUIDACIONES, Rol.ADMINISTRACION, Rol.IT})
    public void guardarFeriado(@Valid @RequestBody FeriadoRequest req) {
        calendario.guardar(req);
    }

    @DeleteMapping("/feriados/{fecha}")
    @RequiereRol({Rol.LIQUIDACIONES, Rol.ADMINISTRACION, Rol.IT})
    public void eliminarFeriado(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        calendario.eliminar(fecha);
    }

    @PostMapping("/feriados/importar")
    @RequiereRol({Rol.LIQUIDACIONES, Rol.ADMINISTRACION, Rol.IT})
    public ResultadoImportacionFeriados importarFeriados(@RequestParam int anio) {
        return calendario.importar(anio);
    }

    // ---- IPC
    @GetMapping("/ipc")
    public List<IpcDto> ipc() {
        return ipc.ultimos();
    }

    @PostMapping("/ipc")
    public void cargarIpc(@Valid @RequestBody IpcRequest req) {
        ipc.cargarManual(req);
    }

    @PostMapping("/ipc/traer")
    public ResultadoImportacionIpc traerIpc() {
        return ipc.traerDeApi();
    }

    @PostMapping("/ipc/{periodo}/aplicar")
    public ResultadoActualizacion aplicarIpc(@PathVariable String periodo, @Valid @RequestBody AplicarIpcRequest req) {
        return ipc.aplicar(periodo, req);
    }
}
