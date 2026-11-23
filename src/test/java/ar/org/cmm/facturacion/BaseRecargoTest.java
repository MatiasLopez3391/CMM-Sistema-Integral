package ar.org.cmm.facturacion;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** El recargo se calcula sobre el valor de las cuotas vencidas, sin recargo sobre recargo (respuesta del CMM del 23/11). */
class BaseRecargoTest {

    private static Comprobante comprobante(String tipo, String total, String recargo, String saldo) {
        Comprobante c = new Comprobante();
        c.setTipo(tipo);
        c.setTotal(new BigDecimal(total));
        c.setSaldo(new BigDecimal(saldo));
        c.agregarItem(item("CUOTA", new BigDecimal(total).subtract(new BigDecimal(recargo))));
        if (new BigDecimal(recargo).signum() > 0) {
            c.agregarItem(item("RECARGO", new BigDecimal(recargo)));
        }
        return c;
    }

    private static ComprobanteItem item(String tipo, BigDecimal importe) {
        CodigoServicio cod = new CodigoServicio();
        cod.setTipo(tipo);
        ComprobanteItem i = new ComprobanteItem();
        i.setCodigoServicio(cod);
        i.setImporte(importe);
        return i;
    }

    @Test
    void noSeCobraRecargoSobreElRecargoAnterior() {
        // Cuota de $100.000 que ya traía $4.200 de recargo, impaga: la base es $100.000
        assertThat(GeneracionCuotasService.baseRecargo(List.of(comprobante("CUOTA", "104200", "4200", "104200"))))
                .isEqualByComparingTo("100000");
    }

    @Test
    void conPagoParcialElPagoCancelaPrimeroElRecargo() {
        // Pagó $10.000 de esa cuota: quedan $94.200 de capital
        assertThat(GeneracionCuotasService.baseRecargo(List.of(comprobante("CUOTA", "104200", "4200", "94200"))))
                .isEqualByComparingTo("94200");
    }

    @Test
    void sumaVariasCuotasYNotasDeDebitoPeroNoNotasDeCredito() {
        assertThat(GeneracionCuotasService.baseRecargo(List.of(
                comprobante("CUOTA", "50000", "0", "50000"),
                comprobante("NOTA_DEBITO", "1500", "0", "1500"),
                comprobante("NOTA_CREDITO", "800", "0", "800"))))
                .isEqualByComparingTo("51500");
    }
}
