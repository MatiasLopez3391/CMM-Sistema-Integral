package ar.org.cmm.cobertura.importacion;

import ar.org.cmm.cobertura.CoberturaDtos.ResultadoImportacion;
import ar.org.cmm.cobertura.CoberturaDtos.ValorRequest;
import ar.org.cmm.cobertura.Practica;
import ar.org.cmm.cobertura.PracticaRepository;
import ar.org.cmm.cobertura.PrestadorRepository;
import ar.org.cmm.cobertura.Rubro;
import ar.org.cmm.cobertura.ValorConvenidoRepository;
import ar.org.cmm.cobertura.ValorConvenidoService;
import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Importa la planilla de valores que envía cada prestador (Excel o CSV).
 * Solo orquesta: la lectura la hace un {@link LectorPlanilla}, las columnas las detecta
 * {@link DetectorColumnas}, el importe lo interpreta {@link ParserImporte} y el guardado lo hace
 * {@link ValorConvenidoService}.
 *
 * <p>Columnas reconocidas: código CMM (obligatoria), nomenclador, descripción (para crear prácticas
 * nuevas), valor (obligatoria) y rubro (opcional; por defecto OTROS). Las columnas "a cargo del
 * afiliado" se ignoran: el coseguro se calcula según el plan (H12).
 */
@Service
public class ImportadorValores {

    private final List<LectorPlanilla> lectores; // ordenados por @Order
    private final ValorConvenidoService valoresService;
    private final PracticaRepository practicas;
    private final PrestadorRepository prestadores;
    private final ValorConvenidoRepository valores;
    private final AuditoriaService auditoria;

    public ImportadorValores(List<LectorPlanilla> lectores, ValorConvenidoService valoresService, PracticaRepository practicas,
                             PrestadorRepository prestadores, ValorConvenidoRepository valores, AuditoriaService auditoria) {
        this.lectores = lectores;
        this.valoresService = valoresService;
        this.practicas = practicas;
        this.prestadores = prestadores;
        this.valores = valores;
        this.auditoria = auditoria;
    }

    @Transactional
    public ResultadoImportacion importar(Long prestadorId, LocalDate vigenteDesde, MultipartFile archivo) {
        prestadores.findById(prestadorId).orElseThrow(() -> new RecursoNoEncontradoException("Prestador", prestadorId));
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Seleccione un archivo.");
        }
        String nombre = archivo.getOriginalFilename() == null ? "archivo" : archivo.getOriginalFilename();
        List<List<String>> filas = leer(nombre, archivo);

        int filaEncabezado = -1;
        Map<String, Integer> columnas = Map.of();
        for (int i = 0; i < Math.min(filas.size(), 30) && filaEncabezado < 0; i++) {
            Map<String, Integer> c = DetectorColumnas.detectar(filas.get(i));
            if (c.containsKey("cmm") && c.containsKey("valor")) {
                filaEncabezado = i;
                columnas = c;
            }
        }
        if (filaEncabezado < 0) {
            throw new ReglaNegocioException("No se encontró la fila de encabezados. Se necesitan al menos las columnas "
                    + "'Código CMM' (o 'CMM') y 'Valor' (o 'Importe').");
        }

        int leidas = 0, creados = 0, actualizados = 0, practicasCreadas = 0;
        List<String> advertencias = new ArrayList<>();
        for (int i = filaEncabezado + 1; i < filas.size(); i++) {
            List<String> fila = filas.get(i);
            String codigo = celda(fila, columnas.get("cmm"));
            String valorTxt = celda(fila, columnas.get("valor"));
            if (codigo.isBlank() && valorTxt.isBlank()) {
                continue; // fila vacía o subtítulo (ej. "ENFERMERIA")
            }
            leidas++;
            if (codigo.isBlank()) {
                continue; // filas de título sin código
            }
            codigo = codigo.replaceAll("\\.0+$", "").trim();
            BigDecimal importe = ParserImporte.parsear(valorTxt);
            if (importe == null || importe.signum() <= 0) {
                advertencias.add("Fila " + (i + 1) + ": valor inválido para " + codigo + " ('" + valorTxt + "')");
                continue;
            }
            Practica practica = practicas.findByCodigoCmm(codigo).orElse(null);
            if (practica == null) {
                String desc = celda(fila, columnas.get("descripcion"));
                if (desc.isBlank()) {
                    advertencias.add("Fila " + (i + 1) + ": la práctica " + codigo + " no existe y no tiene descripción");
                    continue;
                }
                practica = practicas.save(nuevaPractica(codigo, desc, fila, columnas, i + 1, advertencias));
                practicasCreadas++;
            }
            boolean existia = valores.findByPrestadorIdAndPracticaIdAndVigenteDesde(prestadorId, practica.getId(), vigenteDesde).isPresent();
            valoresService.guardarValor(prestadorId, new ValorRequest(practica.getId(), vigenteDesde, importe), "Importación " + nombre);
            if (existia) {
                actualizados++;
            } else {
                creados++;
            }
        }
        auditoria.registrar("prestador", prestadorId, "IMPORTACION_VALORES",
                nombre + ": " + creados + " nuevos, " + actualizados + " actualizados, vigencia " + vigenteDesde);
        return new ResultadoImportacion(leidas, creados, actualizados, practicasCreadas, advertencias);
    }

    private List<List<String>> leer(String nombre, MultipartFile archivo) {
        LectorPlanilla lector = lectores.stream().filter(l -> l.soporta(nombre)).findFirst()
                .orElseThrow(() -> new ReglaNegocioException("Formato de archivo no soportado: " + nombre));
        try (InputStream in = archivo.getInputStream()) {
            return lector.leer(in);
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo leer el archivo: " + e.getMessage());
        }
    }

    private Practica nuevaPractica(String codigo, String descripcion, List<String> fila, Map<String, Integer> columnas,
                                   int numeroFila, List<String> advertencias) {
        Practica p = new Practica();
        p.setCodigoCmm(codigo);
        p.setCodigoNomenclador(blankToNull(celda(fila, columnas.get("nomenclador"))));
        p.setDescripcion(descripcion.trim());
        String rubro = celda(fila, columnas.get("rubro"));
        if (rubro.isBlank()) {
            p.setRubro(Rubro.OTROS.name());
        } else {
            try {
                p.setRubro(Rubro.normalizar(rubro));
            } catch (ReglaNegocioException e) {
                advertencias.add("Fila " + numeroFila + ": rubro '" + rubro + "' desconocido para " + codigo + "; se usó OTROS");
                p.setRubro(Rubro.OTROS.name());
            }
        }
        return p;
    }

    private static String celda(List<String> fila, Integer idx) {
        if (idx == null || idx >= fila.size()) {
            return "";
        }
        String v = fila.get(idx);
        return v == null ? "" : v.trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
