package ar.org.cmm.contratos;

import java.util.Optional;

/**
 * Consulta al padrón de AFIP (ws_sr_padron_a13) para validar CUIT, razón social y
 * condición ante IVA.
 *
 * <p>NO está implementado contra AFIP: requiere el certificado digital del CMM, autorizar el
 * servicio en el portal de AFIP y autenticar con WSAA (primero en homologación). Hasta entonces
 * se usa {@code PadronFiscalNoConfigurado}, que siempre responde vacío.
 */
public interface PadronFiscalServicio {

    boolean disponible();

    Optional<DatosFiscales> consultar(String cuit);

    record DatosFiscales(String cuit, String razonSocial, String condicionIva, String domicilioFiscal) {
    }
}
