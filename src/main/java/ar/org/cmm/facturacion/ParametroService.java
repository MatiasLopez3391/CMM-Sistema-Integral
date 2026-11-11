package ar.org.cmm.facturacion;

import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Parámetros de negocio editables desde pantalla (reemplazan valores fijos en el código). */
@Service
public class ParametroService {

    public static final String DIA_VENCIMIENTO = "cuota.dia_vencimiento";
    public static final String TASA_RECARGO = "mora.tasa_recargo_porcentaje";
    public static final String CUENTA_DEUDORES = "contable.cuenta_deudores";
    public static final String CUOTAS_MINIMAS_MOROSO = "morosos.cuotas_minimas";

    private final ParametroRepository repo;
    private final AuditoriaService auditoria;

    public ParametroService(ParametroRepository repo, AuditoriaService auditoria) {
        this.repo = repo;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public String texto(String clave) {
        return repo.findById(clave).map(Parametro::getValor)
                .orElseThrow(() -> new IllegalStateException("Falta el parámetro " + clave));
    }

    public BigDecimal decimal(String clave) {
        return new BigDecimal(texto(clave).trim().replace(',', '.'));
    }

    public int entero(String clave) {
        return Integer.parseInt(texto(clave).trim());
    }

    @Transactional(readOnly = true)
    public List<Parametro> todos() {
        return repo.findAllByOrderByClaveAsc();
    }

    @Transactional
    public void actualizar(String clave, String valor) {
        Parametro p = repo.findById(clave).orElseThrow(() -> new RecursoNoEncontradoException("Parámetro", clave));
        String v = valor == null ? "" : valor.trim();
        validar(clave, v);
        String anterior = p.getValor();
        p.setValor(v);
        auditoria.registrar("parametro", 0L, "MODIFICACION", clave + ": " + anterior + " -> " + v);
    }

    private void validar(String clave, String v) {
        try {
            switch (clave) {
                case DIA_VENCIMIENTO -> {
                    int d = Integer.parseInt(v);
                    if (d < 1 || d > 28) {
                        throw new ReglaNegocioException("El día de vencimiento debe estar entre 1 y 28.");
                    }
                }
                case TASA_RECARGO -> {
                    BigDecimal t = new BigDecimal(v.replace(',', '.'));
                    if (t.signum() < 0 || t.compareTo(BigDecimal.valueOf(20)) > 0) {
                        throw new ReglaNegocioException("La tasa de recargo debe estar entre 0 y 20 %.");
                    }
                }
                case CUOTAS_MINIMAS_MOROSO -> {
                    if (Integer.parseInt(v) < 1) {
                        throw new ReglaNegocioException("Debe ser al menos 1.");
                    }
                }
                default -> {
                    if (v.isEmpty()) {
                        throw new ReglaNegocioException("El valor no puede estar vacío.");
                    }
                }
            }
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException("Valor numérico inválido para " + clave);
        }
    }
}
