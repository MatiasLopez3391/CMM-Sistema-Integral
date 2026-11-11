# Sistema Integral de Gestión — Círculo Médico de Mendoza

Prototipo de los **Sprints 1 y 2**:

- **Sprint 1 (Afiliados y base de datos):** afiliados y grupo familiar, planes con nivel de cobertura, prestadores con valores convenidos, coseguro según plan, derivadoras e historia clínica.
- **Sprint 2 (Facturación sin factura fiscal):** cuotas, cuenta corriente, cobranza por archivo .txt y por ventanilla, morosos y bloqueo, avisos por mail y exportación de asientos a FEBBO.

Stack del informe: **Java 17+ (compila en 17, recomendado 21) + Spring Boot 3.3 + MySQL 8**, con Flyway para las migraciones. El frontend es web (HTML, CSS y JavaScript con módulos ES, sin paso de compilación) y lo sirve la misma aplicación.

> Grupo 7 · TUDS ITU-UNCuyo · Prototipo para mostrar avances. Los datos personales del perfil de demostración son ficticios.

---

## 1. Cómo ejecutarlo

Requisito: **Java 17 o superior** (recomendado Java 21, por ejemplo [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21)).
**No hace falta instalar Maven**: el proyecto trae el Maven Wrapper, que lo descarga solo la primera vez.

**Windows**

- Doble clic en `iniciar.cmd`; o
- en PowerShell, desde la carpeta del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS**

```bash
./mvnw spring-boot:run
```

Para generar el .jar:

- Windows (PowerShell no acepta `&&`):

  ```powershell
  .\mvnw.cmd package; java -jar target\cmm-sistema-integral-0.2.0-SNAPSHOT.jar
  ```

- Linux / macOS:

  ```bash
  ./mvnw package && java -jar target/cmm-sistema-integral-0.2.0-SNAPSHOT.jar
  ```

**Windows con tildes o "ñ" en la ruta** (ej. `Tercer Año`): `spring-boot:run` puede fallar con *"no se ha encontrado o cargado la clase principal"*. Usar `iniciar.cmd` o el comando `package` + `java -jar` de arriba, que no tienen ese problema, o mover el proyecto a una ruta sin tildes (ej. `C:\proyectos\cmm-sistema-integral`).

Abrir **http://localhost:8080**.

| Dirección | Para qué |
| --- | --- |
| `/` | Aplicación |
| `/api-docs.html` | Documentación de la API (Swagger), es decir, los contratos entre frentes |
| `/h2-console` | Consola de la base de demostración (JDBC `jdbc:h2:file:./data/cmm-demo`, usuario `sa`) |

### Perfil `demo` (por defecto)

- Usa H2 en archivo (`./data/`) en modo compatible con MySQL.
- Al arrancar por primera vez, `DemoInicializador` **reproduce septiembre a noviembre de 2026 usando los servicios reales**, según `src/main/resources/demo/guion.csv`:
  - genera las cuotas de cada mes;
  - importa archivos de PagoMisCuentas y Rapipago;
  - registra pagos en caja con comprobante PDF;
  - bloquea a un moroso.
- No hay datos fijos en el código: todo sale de la base o de archivos de datos.
- Para empezar de cero, borrar la carpeta `data/`.

### Perfil `mysql`

```bash
docker compose up -d                       # MySQL 8.4 local (usuario/clave cmm/cmm)
SPRING_PROFILES_ACTIVE=mysql mvn spring-boot:run
# con datos de demostración:
SPRING_PROFILES_ACTIVE=mysql FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/demo mvn spring-boot:run
```

Variables de entorno: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `UPLOADS_DIR` y `SMTP_HOST` (más `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`).

> Las migraciones se escribieron para MySQL 8 y se probaron sobre H2 en modo MySQL. Antes de la primera demo con MySQL real, levantar el perfil `mysql` y verificar que Flyway termine sin errores.

### Pruebas

```bash
mvn test
```

Son 9 pruebas:

- lector de archivos de cobranza;
- importador de planillas;
- ciclo completo contra la base: coseguro, cuotas, idempotencia, importación con rechazos, recargo por mora, morosos y asientos balanceados.

---

## 2. Qué se puede mostrar

| Pantalla | Funcionalidad clave | Hallazgo del análisis |
| --- | --- | --- |
| Afiliados | Búsqueda; ficha con grupo familiar, cuenta corriente, pagos, historia clínica, bloqueos y auditoría; alta, integrantes, baja (la del titular da de baja al grupo) | Datos del afiliado |
| Planes y cobertura | Plan = código + nivel de cobertura (70/100, 80/100, 100%), con excepciones por rubro (ej. "MED 40%") | H1 |
| Prestadores y coseguro | Valores convenidos con vigencia e historial; importación del Excel del prestador; **simulador de coseguro por plan** | H12 |
| Derivadoras | Obra social de origen del afiliado (sin facturar aportes) | H11 |
| Cuotas | Actualización mensual por % **sin pedirla a sistemas**; simulación y generación masiva; recargo por mora automático; resumen de facturación | Dolor principal del relevamiento |
| Cobranza | Importación de .txt en dos pasos (previsualizar y confirmar) con rechazos y duplicados; pago en ventanilla con comprobante JPG/PNG/PDF; caja diaria | H3 |
| Morosos | Listado con deuda y días de atraso, bloqueo manual con motivo, aviso por mail | H4 (regla automática pendiente) |
| Exportación FEBBO | Asientos de cuotas y pagos a partir de la relación código de servicio → cuenta contable | H10 |
| Configuración | Parámetros (vencimiento, tasa de recargo) y diseños de archivo de cobranza editables | — |

Archivos de ejemplo para probar en vivo (se descargan desde las mismas pantallas, en `static/ejemplos/`):

- `pmc-ejemplo-2026-11-11.txt`: rendición de PagoMisCuentas con 2 pagos válidos, 1 duplicado y 1 afiliado inexistente.
- `planilla-prestador-ejemplo.xlsx`: valores de septiembre con el formato de la planilla de Clínica Santa Rosa.
- `comprobante-ejemplo.pdf`: comprobante para un pago en ventanilla.

---

## 3. Organización del código (frentes del plan de trabajo)

```
ar.org.cmm
├── comun/            Auditoría, usuario actual (preparado para login), errores, numeradores
├── contratos/        Interfaces entre frentes: AfiliadoConsulta, BloqueoServicio, PlanConsulta,
│                     CoseguroServicio, CuentaCorrienteServicio, PadronFiscalServicio
├── afiliados/        F1 · Núcleo y Afiliados
├── cobertura/        F2 · Planes, derivadoras, prestadores, valores, coseguro
├── facturacion/      F3 · Cuotas, cuenta corriente, morosos, avisos, asientos FEBBO
├── cobranza/         F4 · Archivos de entes de cobro, ventanilla, caja
├── historiaclinica/  F5 · (Órdenes y auditoría médica se suman en el Sprint 3)
├── tablero/          Indicadores de inicio (solo lectura)
└── demo/             Carga de demostración (solo perfil demo)
```

Reglas que el código ya respeta:

- **Ningún módulo usa clases internas de otro.** Solo se comunican por `contratos/`. Las referencias entre módulos se guardan por id (por ejemplo, `Comprobante.afiliadoId`).
- **Cada frente numera sus migraciones en su rango:** núcleo `V001`, F1 `V1xx`, F2 `V2xx`, F3 `V3xx`, F4 `V4xx`, catálogos `V900`, demo `V950`. Está activo `out-of-order` para que no choquen entre sí.
- **La API nunca expone entidades JPA:** usa DTOs (records). Los errores tienen un formato único (`ProblemDetail`), cuyo campo `detail` muestra el frontend.

---

## 4. Login (preparado, no implementado)

- Todo el código obtiene el usuario con `UsuarioActualProvider`.
- La implementación actual, `UsuarioDemoProvider`, toma el nombre del campo **"Operador"** de la barra superior (encabezado `X-Usuario`), así la auditoría ya registra nombres.
- Los endpoints están marcados con `@RequiereRol({...})` según los roles del informe.

Para activar Spring Security:

1. Agregar `spring-boot-starter-security`.
2. Crear `UsuarioSpringSecurityProvider implements UsuarioActualProvider` con `@Primary`, que lea `SecurityContextHolder`.
3. Reemplazar `@RequiereRol` por `@PreAuthorize("hasAnyRole(...)")` (búsqueda y reemplazo) y habilitar `@EnableMethodSecurity`.
4. Servir todo por **HTTPS**, indispensable para el médico auditor remoto.

---

## 5. Integraciones externas: estado y qué hace falta

| Integración | Estado | Qué se necesita |
| --- | --- | --- |
| Facturación electrónica AFIP / ALTEA | **Fuera de alcance por ahora (H2).** La cuota se emite como comprobante interno no fiscal. | Decidir más adelante entre generar el archivo para ALTEA o usar el web service de AFIP (WSFEv1). |
| Padrón AFIP (validar CUIT y condición ante IVA) | Interfaz `PadronFiscalServicio` lista; la implementación actual responde vacío. | Certificado digital del CMM, autorizar `ws_sr_padron_a13` en AFIP, autenticación WSAA y pruebas en homologación. |
| Archivo de PagoMisCuentas | Funciona con un **diseño de archivo provisorio** y configurable desde pantalla. | Un archivo real de ejemplo (anonimizado) para ajustar las posiciones en Configuración. |
| FEBBO | Exporta un CSV provisorio (`nro_asiento;fecha;cuenta;debe;haber;concepto;referencia`). | Diseño de importación de FEBBO y plan de cuentas. Las cuentas actuales dicen "A CONFIRMAR". |
| Correo (avisos de deuda) | Funciona con cualquier SMTP. Sin `SMTP_HOST` el aviso se registra como `SIN_SMTP` y se puede ver en pantalla. | Servidor o cuenta de correo del CMM. |

Datos marcados **"A CONFIRMAR"** en `V900__catalogos_iniciales.sql`:

- CUIT de las derivadoras;
- plan de cuentas;
- planes 7500, 7600 y 9200 (no se cargaron porque falta confirmar su cobertura);
- interpretación de "MED 40%" (40% de cobertura en medicamentos).
