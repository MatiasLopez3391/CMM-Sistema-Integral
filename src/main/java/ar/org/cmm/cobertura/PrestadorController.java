package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.CoberturaDtos.PrestadorDto;
import ar.org.cmm.cobertura.CoberturaDtos.PrestadorRequest;
import ar.org.cmm.cobertura.CoberturaDtos.ResultadoImportacion;
import ar.org.cmm.cobertura.CoberturaDtos.ValorRequest;
import ar.org.cmm.cobertura.CoberturaDtos.ValorVigenteDto;
import ar.org.cmm.cobertura.importacion.ImportadorValores;
import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cobertura/prestadores")
@RequiereRol({Rol.ADMINISTRACION, Rol.LIQUIDACIONES, Rol.IT})
public class PrestadorController {

    private final PrestadorService prestadores;
    private final ValorConvenidoService valores;
    private final ImportadorValores importador;

    public PrestadorController(PrestadorService prestadores, ValorConvenidoService valores, ImportadorValores importador) {
        this.prestadores = prestadores;
        this.valores = valores;
        this.importador = importador;
    }

    @GetMapping
    @RequiereRol({Rol.ADMINISTRACION, Rol.LIQUIDACIONES, Rol.MESA_ENTRADAS, Rol.MEDICO_AUDITOR, Rol.IT})
    public List<PrestadorDto> listar() {
        return prestadores.listarPrestadores();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrestadorDto crear(@Valid @RequestBody PrestadorRequest req) {
        return prestadores.guardarPrestador(null, req);
    }

    @PutMapping("/{id}")
    public PrestadorDto modificar(@PathVariable Long id, @Valid @RequestBody PrestadorRequest req) {
        return prestadores.guardarPrestador(id, req);
    }

    // Los valores convenidos son información comercial: solo roles de administración, liquidaciones e IT.
    @GetMapping("/{id}/valores")
    public List<ValorVigenteDto> valores(@PathVariable Long id,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return valores.valoresVigentes(id, fecha != null ? fecha : LocalDate.now());
    }

    @PostMapping("/{id}/valores")
    @ResponseStatus(HttpStatus.CREATED)
    public void cargarValor(@PathVariable Long id, @Valid @RequestBody ValorRequest req) {
        valores.guardarValor(id, req, "Carga manual");
    }

    @PostMapping(value = "/{id}/valores/importar", consumes = "multipart/form-data")
    public ResultadoImportacion importar(@PathVariable Long id,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vigenteDesde,
                                         @RequestPart("archivo") MultipartFile archivo) {
        return importador.importar(id, vigenteDesde, archivo);
    }
}
