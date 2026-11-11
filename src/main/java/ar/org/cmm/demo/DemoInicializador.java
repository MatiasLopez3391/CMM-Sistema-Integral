package ar.org.cmm.demo;

import ar.org.cmm.afiliados.BloqueoServicioImpl;
import ar.org.cmm.cobranza.CobranzaDtos.LoteDto;
import ar.org.cmm.cobranza.CobranzaDtos.MedioDto;
import ar.org.cmm.cobranza.CobranzaService;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.BloqueoServicio;
import ar.org.cmm.facturacion.ComprobanteRepository;
import ar.org.cmm.facturacion.GeneracionCuotasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

/**
 * SOLO perfil "demo": la primera vez que arranca con la base vacía, reproduce tres meses de operación
 * (septiembre a noviembre 2026) ejecutando los servicios reales según {@code demo/guion.csv}:
 * genera cuotas, importa archivos de PagoMisCuentas y Rapipago, registra pagos en caja y bloquea un moroso.
 * Así el prototipo muestra cuentas corrientes, recargos y morosos reales sin datos fijos en el código.
 */
@Component
@Profile("demo")
public class DemoInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoInicializador.class);

    private final ComprobanteRepository comprobantes;
    private final GeneracionCuotasService generacion;
    private final CobranzaService cobranza;
    private final AfiliadoConsulta afiliados;
    private final BloqueoServicio bloqueo;

    public DemoInicializador(ComprobanteRepository comprobantes, GeneracionCuotasService generacion, CobranzaService cobranza,
                             AfiliadoConsulta afiliados, BloqueoServicio bloqueo) {
        this.comprobantes = comprobantes;
        this.generacion = generacion;
        this.cobranza = cobranza;
        this.afiliados = afiliados;
        this.bloqueo = bloqueo;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (comprobantes.count() > 0) {
            return;
        }
        log.info("Cargando datos iniciales...");
        List<String> pagosCaja = leer("demo/pagos-caja.csv");
        for (String linea : leer("demo/guion.csv")) {
            if (linea.isBlank() || linea.startsWith("#")) {
                continue;
            }
            String[] p = linea.split(";");
            switch (p[0]) {
                case "GENERAR" -> {
                    var r = generacion.generar(p[1], LocalDate.parse(p[2]));
                    log.info("  Cuotas {}: {} comprobantes, total {}", p[1], r.generados(), r.totalGeneral());
                }
                case "IMPORTAR" -> {
                    Long medioId = cobranza.medios().stream().filter(m -> m.codigo().equals(p[1])).map(MedioDto::id)
                            .findFirst().orElseThrow();
                    LoteDto lote = cobranza.previsualizar(medioId, new ArchivoDemo(p[2], bytes("demo/" + p[2])));
                    var c = cobranza.confirmar(lote.id());
                    log.info("  Archivo {}: {} pagos imputados ({} rechazados)", p[2], c.pagosImputados(), lote.cantidadRechazados());
                }
                case "CAJA" -> {
                    byte[] pdf = bytes("demo/comprobante-caja-demo.pdf");
                    for (String pc : pagosCaja.subList(1, pagosCaja.size())) {
                        String[] c = pc.split(";");
                        if (!c[1].startsWith(p[1])) {
                            continue;
                        }
                        Long afiliadoId = afiliados.porNroAfiliado(c[0]).orElseThrow().id();
                        cobranza.registrarPagoManual(afiliadoId, LocalDate.parse(c[1]), new BigDecimal(c[2]), "CAJA", c[3],
                                new ArchivoDemo("recibo-caja.pdf", pdf));
                    }
                }
                case "BLOQUEAR" -> {
                    Long afiliadoId = afiliados.porNroAfiliado(p[1]).orElseThrow().id();
                    bloqueo.bloquear(afiliadoId, BloqueoServicioImpl.MARCA_FALTA_PAGO, p[2]);
                }
                default -> log.warn("Acción desconocida en guion: {}", p[0]);
            }
        }
        log.info("Datos iniciales cargados.");
    }

    private static List<String> leer(String recurso) throws IOException {
        return new String(bytes(recurso), StandardCharsets.UTF_8).lines().toList();
    }

    private static byte[] bytes(String recurso) throws IOException {
        try (InputStream in = new ClassPathResource(recurso).getInputStream()) {
            return in.readAllBytes();
        }
    }

    /** MultipartFile en memoria para reutilizar los mismos servicios que usa la API. */
    record ArchivoDemo(String nombre, byte[] contenido) implements MultipartFile {
        @Override public String getName() { return "archivo"; }
        @Override public String getOriginalFilename() { return nombre; }
        @Override public String getContentType() { return "application/octet-stream"; }
        @Override public boolean isEmpty() { return contenido.length == 0; }
        @Override public long getSize() { return contenido.length; }
        @Override public byte[] getBytes() { return contenido; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(contenido); }
        @Override public void transferTo(File dest) throws IOException { Files.write(dest.toPath(), contenido); }
    }
}
