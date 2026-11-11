package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.CoberturaDtos.DerivadoraDto;
import ar.org.cmm.cobertura.CoberturaDtos.DerivadoraRequest;
import ar.org.cmm.cobertura.CoberturaDtos.PlanDto;
import ar.org.cmm.cobertura.CoberturaDtos.PlanRequest;
import ar.org.cmm.cobertura.CoberturaDtos.PracticaDto;
import ar.org.cmm.cobertura.CoberturaDtos.PracticaRequest;
import ar.org.cmm.cobertura.CoberturaDtos.PrestadorDto;
import ar.org.cmm.cobertura.CoberturaDtos.PrestadorRequest;
import ar.org.cmm.cobertura.CoberturaDtos.ResultadoImportacion;
import ar.org.cmm.cobertura.CoberturaDtos.ValorRequest;
import ar.org.cmm.cobertura.CoberturaDtos.ValorVigenteDto;
import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;
import ar.org.cmm.contratos.CoseguroServicio.CalculoCoseguro;
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
import java.util.Map;

@RestController
@RequestMapping("/api/cobertura")
@RequiereRol({Rol.ADMINISTRACION, Rol.LIQUIDACIONES, Rol.IT})
public class CoberturaController {

    private final CoberturaService servicio;
    private final CoseguroServicioImpl coseguro;
    private final ImportadorValores importador;

    public CoberturaController(CoberturaService servicio, CoseguroServicioImpl coseguro, ImportadorValores importador) {
        this.servicio = servicio;
        this.coseguro = coseguro;
        this.importador = importador;
    }

    // ---- Planes
    @GetMapping("/planes")
    public List<PlanDto> planes() {
        return servicio.listarPlanes();
    }

    @PostMapping("/planes")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanDto crearPlan(@Valid @RequestBody PlanRequest req) {
        return servicio.guardarPlan(null, req);
    }

    @PutMapping("/planes/{id}")
    public PlanDto modificarPlan(@PathVariable Long id, @Valid @RequestBody PlanRequest req) {
        return servicio.guardarPlan(id, req);
    }

    // ---- Derivadoras
    @GetMapping("/derivadoras")
    public List<DerivadoraDto> derivadoras() {
        return servicio.listarDerivadoras();
    }

    @PostMapping("/derivadoras")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> crearDerivadora(@Valid @RequestBody DerivadoraRequest req) {
        return Map.of("id", servicio.guardarDerivadora(null, req));
    }

    @PutMapping("/derivadoras/{id}")
    public Map<String, Long> modificarDerivadora(@PathVariable Long id, @Valid @RequestBody DerivadoraRequest req) {
        return Map.of("id", servicio.guardarDerivadora(id, req));
    }

    // ---- Prestadores
    @GetMapping("/prestadores")
    public List<PrestadorDto> prestadores() {
        return servicio.listarPrestadores();
    }

    @PostMapping("/prestadores")
    @ResponseStatus(HttpStatus.CREATED)
    public PrestadorDto crearPrestador(@Valid @RequestBody PrestadorRequest req) {
        return servicio.guardarPrestador(null, req);
    }

    @PutMapping("/prestadores/{id}")
    public PrestadorDto modificarPrestador(@PathVariable Long id, @Valid @RequestBody PrestadorRequest req) {
        return servicio.guardarPrestador(id, req);
    }

    @GetMapping("/prestadores/{id}/valores")
    public List<ValorVigenteDto> valores(@PathVariable Long id,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return servicio.valoresVigentes(id, fecha != null ? fecha : LocalDate.now());
    }

    @PostMapping("/prestadores/{id}/valores")
    @ResponseStatus(HttpStatus.CREATED)
    public void cargarValor(@PathVariable Long id, @Valid @RequestBody ValorRequest req) {
        servicio.guardarValor(id, req, "Carga manual");
    }

    @PostMapping(value = "/prestadores/{id}/valores/importar", consumes = "multipart/form-data")
    public ResultadoImportacion importar(@PathVariable Long id,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vigenteDesde,
                                         @RequestPart("archivo") MultipartFile archivo) {
        return importador.importar(id, vigenteDesde, archivo);
    }

    // ---- Prácticas
    @GetMapping("/practicas")
    public List<PracticaDto> practicas() {
        return servicio.listarPracticas();
    }

    @PostMapping("/practicas")
    @ResponseStatus(HttpStatus.CREATED)
    public PracticaDto crearPractica(@Valid @RequestBody PracticaRequest req) {
        return servicio.crearPractica(req);
    }

    // ---- Coseguro (H12)
    @GetMapping("/coseguro")
    public CalculoCoseguro calcularCoseguro(@RequestParam Long prestadorId, @RequestParam Long practicaId, @RequestParam Long planId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return coseguro.calcular(prestadorId, practicaId, planId, fecha != null ? fecha : LocalDate.now());
    }

    @GetMapping("/coseguro/simulador")
    public List<CalculoCoseguro> simulador(@RequestParam Long prestadorId, @RequestParam Long practicaId,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return coseguro.simularTodosLosPlanes(prestadorId, practicaId, fecha != null ? fecha : LocalDate.now());
    }
}
