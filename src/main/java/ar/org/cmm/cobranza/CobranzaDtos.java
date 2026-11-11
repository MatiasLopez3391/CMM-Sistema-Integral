package ar.org.cmm.cobranza;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class CobranzaDtos {

    private CobranzaDtos() {
    }

    public record MedioDto(Long id, String codigo, String nombre, String cuentaContable, boolean admiteArchivo,
                           Long layoutId, String layoutNombre, boolean layoutProvisorio) {
    }

    public record LoteDto(Long id, String medio, String nombreArchivo, String estado, int cantidadRegistros, int cantidadOk,
                          int cantidadRechazados, BigDecimal importeOk, LocalDateTime creadoEl, String creadoPor,
                          LocalDateTime confirmadoEl, List<PagoDto> pagos) {
    }

    public record PagoDto(Long id, Integer linea, String medio, Long afiliadoId, String nroAfiliado, String afiliado,
                          LocalDate fechaPago, BigDecimal importe, String referencia, String estado, String motivoRechazo,
                          boolean tieneAdjunto, String adjuntoNombre, String observaciones, String registradoPor,
                          LocalDateTime registradoEl) {
    }

    public record ResultadoConfirmacion(int pagosImputados, BigDecimal importeImputado, BigDecimal importeACuenta,
                                        List<String> comprobantesCancelados) {
    }

    public record CajaDiariaDto(LocalDate fecha, BigDecimal total, int cantidad, List<CajaMedioDto> porMedio, List<PagoDto> pagos) {
    }

    public record CajaMedioDto(String medio, int cantidad, BigDecimal total) {
    }

    public record LayoutDto(Long id, Long medioCobroId, String medio, String nombre, String formato, String delimitador,
                            String prefijoRegistro, int lineasEncabezado, int afiliadoPos, Integer afiliadoLargo,
                            int fechaPos, Integer fechaLargo, String fechaFormato, int importePos, Integer importeLargo,
                            int importeDecimalesImpl, Integer referenciaPos, Integer referenciaLargo, boolean provisorio,
                            String observaciones) {
        static LayoutDto de(LayoutArchivo l) {
            return new LayoutDto(l.getId(), l.getMedioCobro().getId(), l.getMedioCobro().getNombre(), l.getNombre(), l.getFormato(),
                    l.getDelimitador(), l.getPrefijoRegistro(), l.getLineasEncabezado(), l.getAfiliadoPos(), l.getAfiliadoLargo(),
                    l.getFechaPos(), l.getFechaLargo(), l.getFechaFormato(), l.getImportePos(), l.getImporteLargo(),
                    l.getImporteDecimalesImpl(), l.getReferenciaPos(), l.getReferenciaLargo(), l.isProvisorio(), l.getObservaciones());
        }
    }
}
