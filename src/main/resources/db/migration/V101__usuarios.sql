-- =====================================================================
-- F1 · Usuarios y roles para el login (T36). Rango V100-V199.
-- Solo la estructura: los usuarios reales los carga el CMM (ver README,
-- sección "Login"). Los usuarios de demostración están en db/demo.
-- =====================================================================

CREATE TABLE usuario (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(40)  NOT NULL,
    nombre     VARCHAR(120) NOT NULL,
    clave_hash VARCHAR(100) NOT NULL,
    activo     BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_usuario_username UNIQUE (username)
);

CREATE TABLE usuario_rol (
    usuario_id BIGINT      NOT NULL,
    rol        VARCHAR(30) NOT NULL,
    PRIMARY KEY (usuario_id, rol),
    CONSTRAINT fk_usuario_rol_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);
