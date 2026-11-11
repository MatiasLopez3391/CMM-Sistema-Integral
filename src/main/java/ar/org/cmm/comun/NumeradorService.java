package ar.org.cmm.comun;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Numeración correlativa segura ante usuarios concurrentes (bloqueo pesimista de fila). */
@Service
public class NumeradorService {

    private final EntityManager em;

    public NumeradorService(EntityManager em) {
        this.em = em;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public long siguiente(String clave) {
        Numerador n = em.find(Numerador.class, clave, LockModeType.PESSIMISTIC_WRITE);
        if (n == null) {
            throw new IllegalStateException("No existe el numerador " + clave);
        }
        n.setUltimo(n.getUltimo() + 1);
        return n.getUltimo();
    }
}
