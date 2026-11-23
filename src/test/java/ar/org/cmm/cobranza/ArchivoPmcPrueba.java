package ar.org.cmm.cobranza;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Arma archivos de PagoMisCuentas (COB) con el diseño real de 100 caracteres, para las pruebas.
 * Usa números de afiliado de la demo: nunca se copian datos reales del CMM al repositorio.
 */
public final class ArchivoPmcPrueba {

    public record Pago(String nroAfiliado, String idFactura, String fechaAAAAMMDD, String importe) {
    }

    private ArchivoPmcPrueba() {
    }

    public static Pago pago(String nroAfiliado, String idFactura, String fechaAAAAMMDD, String importe) {
        return new Pago(nroAfiliado, idFactura, fechaAAAAMMDD, importe);
    }

    /** Archivo completo: encabezado, un detalle por pago y pie con cantidad y total correctos. */
    public static String archivo(Pago... pagos) {
        BigDecimal total = BigDecimal.ZERO;
        for (Pago p : pagos) {
            total = total.add(new BigDecimal(p.importe()));
        }
        return armar(pagos, pagos.length, total);
    }

    /** Igual que {@link #archivo} pero con un pie que informa otra cantidad y otro total. */
    public static String archivoConPie(int cantidadPie, String totalPie, Pago... pagos) {
        return armar(pagos, cantidadPie, new BigDecimal(totalPie));
    }

    /** Archivo cortado: sin el registro de pie. */
    public static String archivoSinPie(Pago... pagos) {
        List<String> lineas = new ArrayList<>(List.of(armar(pagos, 0, BigDecimal.ZERO).split("\n")));
        lineas.remove(lineas.size() - 1);
        return String.join("\n", lineas) + "\n";
    }

    private static String armar(Pago[] pagos, int cantidadPie, BigDecimal totalPie) {
        StringBuilder sb = new StringBuilder();
        sb.append(derecha("04004104" + "20261111", 100)).append('\n');
        for (Pago p : pagos) {
            sb.append("5")
                    .append(String.format("%-19s", p.nroAfiliado()))
                    .append(String.format("%-20s", p.idFactura()))
                    .append(p.fechaAAAAMMDD())
                    .append("0").append("20261111")
                    .append(izquierda(centavos(new BigDecimal(p.importe())), 11))
                    .append("2").append("20261111").append("HB0001").append("   ")
                    .append("0".repeat(14)).append('\n');
        }
        sb.append(derecha("9" + "4004104" + "20261111" + izquierda(String.valueOf(cantidadPie), 7)
                + izquierda(centavos(totalPie), 18), 100)).append('\n');
        return sb.toString();
    }

    private static String centavos(BigDecimal importe) {
        return importe.movePointRight(2).setScale(0).toPlainString();
    }

    private static String izquierda(String n, int largo) {
        return "0".repeat(largo - n.length()) + n;
    }

    private static String derecha(String s, int largo) {
        return s + "0".repeat(largo - s.length());
    }
}
