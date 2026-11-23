package ar.org.cmm.cobertura;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/** Adaptador provisorio (SQL de solo lectura). Reemplazar por AfiliadoConsulta cuando F1 lo publique. */
@Component
class ConteoAfiliadosPorDerivadoraJdbc implements ConteoAfiliadosPorDerivadora {

    private final JdbcTemplate jdbc;

    ConteoAfiliadosPorDerivadoraJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<Long, Long> activosPorDerivadora() {
        Map<Long, Long> conteo = new HashMap<>();
        jdbc.query("select derivadora_id, count(*) c from afiliado where derivadora_id is not null and estado = 'ACTIVO' group by derivadora_id",
                rs -> {
                    conteo.put(rs.getLong(1), rs.getLong(2));
                });
        return conteo;
    }
}
