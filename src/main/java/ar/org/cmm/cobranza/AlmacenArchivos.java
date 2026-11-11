package ar.org.cmm.cobranza;

import ar.org.cmm.comun.ReglaNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Guarda los comprobantes de pago (JPG, PNG o PDF) en disco, organizados por año/mes.
 * En producción {@code app.uploads-dir} debe apuntar a un volumen con respaldo.
 */
@Component
public class AlmacenArchivos {

    private static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "application/pdf");

    private final Path raiz;

    public AlmacenArchivos(@Value("${app.uploads-dir}") String dir) {
        this.raiz = Path.of(dir).toAbsolutePath().normalize();
    }

    public record ArchivoGuardado(String nombreOriginal, String rutaRelativa, String tipo) {
    }

    public ArchivoGuardado guardarComprobante(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Adjunte el comprobante del pago (JPG, PNG o PDF).");
        }
        String tipo = detectarTipo(archivo);
        if (!TIPOS.contains(tipo)) {
            throw new ReglaNegocioException("El comprobante debe ser una imagen JPG/PNG o un PDF.");
        }
        String ext = switch (tipo) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> ".pdf";
        };
        LocalDate hoy = LocalDate.now();
        Path relativa = Path.of("comprobantes", String.valueOf(hoy.getYear()), String.format("%02d", hoy.getMonthValue()),
                UUID.randomUUID() + ext);
        Path destino = raiz.resolve(relativa);
        try (InputStream in = archivo.getInputStream()) {
            Files.createDirectories(destino.getParent());
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el comprobante", e);
        }
        String original = archivo.getOriginalFilename() == null ? "comprobante" + ext : archivo.getOriginalFilename();
        return new ArchivoGuardado(original, relativa.toString().replace('\\', '/'), tipo);
    }

    public Path resolver(String rutaRelativa) {
        Path p = raiz.resolve(rutaRelativa).normalize();
        if (!p.startsWith(raiz)) {
            throw new ReglaNegocioException("Ruta inválida");
        }
        return p;
    }

    /** Verifica el tipo por la firma del archivo, no solo por la extensión declarada. */
    private static String detectarTipo(MultipartFile archivo) {
        try (InputStream in = archivo.getInputStream()) {
            byte[] h = in.readNBytes(8);
            if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }
            if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
                return "image/png";
            }
            if (h.length >= 4 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F') {
                return "application/pdf";
            }
            return "desconocido";
        } catch (IOException e) {
            return "desconocido";
        }
    }
}
