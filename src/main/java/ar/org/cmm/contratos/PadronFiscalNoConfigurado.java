package ar.org.cmm.contratos;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implementación por defecto mientras no haya certificado AFIP (ver {@link PadronFiscalServicio}).
 * La implementación real se agrega como otro bean anotado con {@code @Primary}.
 */
@Component
public class PadronFiscalNoConfigurado implements PadronFiscalServicio {

    @Override
    public boolean disponible() {
        return false;
    }

    @Override
    public Optional<DatosFiscales> consultar(String cuit) {
        return Optional.empty();
    }
}
