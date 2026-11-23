package ar.org.cmm.comun.reportes;

import java.util.ArrayList;
import java.util.List;

/**
 * Reporte listo para imprimir o descargar (PDF o Excel). Lo arma cada módulo con sus datos y lo convierte
 * {@link ExportadorReportes}, así todas las pantallas imprimen con el mismo formato.
 *
 * @param titulo título principal (ej. "Estado de cuenta")
 * @param datos  líneas de encabezado del tipo "Etiqueta: valor" (afiliado, período, filtros…)
 * @param tablas una o más tablas, cada una con su subtítulo opcional
 * @param notas  texto al pie (aclaraciones, leyendas)
 */
public record Reporte(String titulo, List<String> datos, List<Tabla> tablas, List<String> notas) {

    public enum Tipo { TEXTO, MONEDA, NUMERO, FECHA, PORCENTAJE }

    /** @param ancho peso relativo del ancho en el PDF */
    public record Columna(String titulo, Tipo tipo, float ancho) {
    }

    /** @param totales fila de totales (null si no tiene); null en las celdas sin total */
    public record Tabla(String subtitulo, List<Columna> columnas, List<List<Object>> filas, List<Object> totales) {
    }

    public static Builder de(String titulo) {
        return new Builder(titulo);
    }

    public static final class Builder {
        private final String titulo;
        private final List<String> datos = new ArrayList<>();
        private final List<Tabla> tablas = new ArrayList<>();
        private final List<String> notas = new ArrayList<>();
        private String subtitulo;
        private List<Columna> columnas = new ArrayList<>();
        private List<List<Object>> filas = new ArrayList<>();
        private List<Object> totales;

        private Builder(String titulo) {
            this.titulo = titulo;
        }

        public Builder dato(String etiqueta, Object valor) {
            datos.add(etiqueta + ": " + (valor == null ? "—" : valor));
            return this;
        }

        /** Empieza una tabla nueva (la primera no hace falta abrirla). */
        public Builder tabla(String subtitulo) {
            cerrarTabla();
            this.subtitulo = subtitulo;
            return this;
        }

        public Builder columna(String titulo, Tipo tipo, float ancho) {
            columnas.add(new Columna(titulo, tipo, ancho));
            return this;
        }

        public Builder fila(Object... valores) {
            filas.add(java.util.Arrays.asList(valores));
            return this;
        }

        public Builder totales(Object... valores) {
            totales = java.util.Arrays.asList(valores);
            return this;
        }

        public Builder nota(String texto) {
            notas.add(texto);
            return this;
        }

        private void cerrarTabla() {
            if (!columnas.isEmpty()) {
                tablas.add(new Tabla(subtitulo, List.copyOf(columnas), List.copyOf(filas), totales));
            }
            subtitulo = null;
            columnas = new ArrayList<>();
            filas = new ArrayList<>();
            totales = null;
        }

        public Reporte build() {
            cerrarTabla();
            return new Reporte(titulo, List.copyOf(datos), List.copyOf(tablas), List.copyOf(notas));
        }
    }
}
