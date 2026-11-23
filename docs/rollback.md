# Procedimiento de rollback (T39 · ENT-05)

**Estado:** los pasos para el entorno de desarrollo (Docker, sección 8) se ensayaron el 22/11/2026 con resultado correcto (ver sección 9). Falta repetirlo en la integración (T34) y adaptarlo al servidor del CMM. Lo que depende del servidor del CMM figura como **A CONFIRMAR** (sección 2); no se supone nada.

## 1. Qué es un rollback en este sistema

Volver a la versión anterior de la aplicación **y** de sus datos, tal como estaban antes de un despliegue que salió mal.

Tres cosas forman el estado del sistema y las tres se vuelven atrás juntas:

| Qué | Dónde está | Cómo se vuelve atrás |
| --- | --- | --- |
| La aplicación | el `.jar` (`cmm-sistema-integral-<versión>.jar`) | volver a arrancar el `.jar` anterior |
| La base de datos | MySQL, esquema `cmm` | restaurar el respaldo tomado antes del despliegue |
| Los comprobantes de pago adjuntos | carpeta `UPLOADS_DIR` (por defecto `./data/uploads`) | restaurar la copia tomada antes del despliegue |

**Por qué se restaura un respaldo y no se "deshacen" migraciones:** las migraciones de Flyway solo avanzan (no hay migraciones de deshacer). Un `.jar` viejo contra una base ya migrada no es una combinación probada. Por eso la base se restaura al estado anterior y se arranca el `.jar` anterior.

**Qué se pierde:** todo lo que se haya cargado entre el respaldo y el rollback (pagos, afiliados, órdenes, etc.). Por eso el rollback se decide rápido y el respaldo se toma justo antes de desplegar (sección 3). Lo cargado después del respaldo debe volver a cargarse a mano; para saber qué fue, ver el paso 6 de la sección 5.

## 2. Datos a confirmar con IT del CMM

Sin estos datos el procedimiento de producción no está cerrado. Ya figuran entre las preguntas para IT del plan de trabajo.

- [ ] **A CONFIRMAR:** servidor donde correrá el sistema (sistema operativo, versión de MySQL).
- [ ] **A CONFIRMAR:** cómo se hacen hoy los respaldos y dónde se guardan (para no duplicar ni pisar la política existente).
- [ ] **A CONFIRMAR:** quién puede autorizar un rollback y quién lo ejecuta.
- [ ] **A CONFIRMAR:** ventana de despliegue (horario con menos uso).
- [ ] **A CONFIRMAR:** tiempo máximo aceptable de interrupción.
- [ ] **A CONFIRMAR:** dónde se instalará el `.jar` y cómo se lo arranca/detiene (servicio, tarea programada, manual).

## 3. Antes de cada despliegue (checklist)

1. Avisar a los usuarios que el sistema va a estar fuera de servicio.
2. **Detener la aplicación** (así no se escribe mientras se respalda).
3. **Anotar la versión de datos actual** y guardarla junto al respaldo:

   ```sql
   SELECT version, description, installed_on FROM flyway_schema_history
   WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1;
   ```
4. **Respaldar la base** con `mysqldump` (ver sección 8 para el comando completo). El respaldo debe incluir `flyway_schema_history`, que va dentro del esquema.
5. **Copiar la carpeta `UPLOADS_DIR`** completa.
6. **Conservar el `.jar` anterior** en una carpeta con su número de versión. No sobrescribirlo.
7. Verificar que el respaldo no esté vacío (tamaño mayor que cero) y guardarlo **fuera del servidor** o en otro disco.
8. Recién entonces desplegar el `.jar` nuevo y arrancar.

> Los respaldos contienen datos personales y de salud de afiliados. Guardarlos en un lugar con acceso restringido y **nunca subirlos a GitHub** (la carpeta `respaldos/` está en `.gitignore`).

## 4. Cuándo hacer rollback

Hacer rollback si, después de desplegar, ocurre cualquiera de estas cosas y no se resuelve en el tiempo acordado con el CMM (sección 2):

- la aplicación no arranca (por ejemplo, Flyway informa un error de migración);
- no se puede iniciar sesión;
- falla una pantalla principal (afiliados, cuotas, cobranza) o aparecen errores 500;
- los datos se ven incorrectos o incompletos.

Si el problema es solo de una pantalla puntual y hay una corrección rápida, evaluar corregir en lugar de volver atrás. La decisión la toma quien autorice el rollback (sección 2).

## 5. Procedimiento de rollback

1. **Detener la aplicación.**
2. **Anotar** la hora y el motivo del rollback, y la última versión de datos (consulta del paso 3 de la sección 3, ejecutada sobre la base actual). Sirve para saber qué se revierte.
3. **Restaurar la base:** borrar y volver a crear el esquema `cmm` y cargar el respaldo (comandos en la sección 8).
4. **Restaurar los comprobantes:** reemplazar `UPLOADS_DIR` por la copia tomada antes del despliegue.
5. **Arrancar el `.jar` anterior** con la misma configuración de antes.
6. **Registrar lo perdido:** lo cargado entre el respaldo y el rollback no está. Pedir a los usuarios que lo revisen y lo vuelvan a cargar. Como ayuda, **antes del paso 3** se puede exportar de la base actual lo cargado después del respaldo (por ejemplo, pagos y afiliados con fecha posterior al respaldo) y guardarlo aparte.

## 6. Verificación después del rollback

- [ ] La aplicación arranca y el log **no** aplica migraciones nuevas ("Schema is up to date. No migration necessary").
- [ ] La versión de datos coincide con la anotada en el paso 3 de la sección 3.
- [ ] Se puede iniciar sesión con un usuario real.
- [ ] Se abre un afiliado conocido y sus datos son los esperados.
- [ ] Se abre un pago con comprobante adjunto y el archivo se descarga.
- [ ] La caja diaria o el listado de pagos muestra lo esperado hasta la hora del respaldo.

## 7. Después del rollback

- Avisar a los usuarios que el sistema vuelve a estar disponible y qué datos deben recargar.
- Analizar la causa del fallo del despliegue **antes** de volver a intentarlo.
- Conservar el respaldo usado hasta que el despliegue siguiente esté estable.

## 8. Ensayo en una PC de desarrollo (Docker + MySQL)

Sirve para practicar el procedimiento y para la prueba de rollback del plan (T34). Usa el contenedor `cmm-mysql` de `docker-compose.yml` (usuario `cmm`/`cmm`, administrador `root`/`root`, **solo desarrollo**). Se ejecuta en PowerShell, dentro de la carpeta del proyecto, con la aplicación **detenida** y el contenedor en marcha (`docker compose up -d`).

> **No usar `>` para guardar el respaldo en PowerShell 5.1:** escribe el archivo en UTF-16 y lo daña. Por eso el volcado se genera dentro del contenedor y se copia con `docker cp`.

**Paso 0. Estado inicial (anotar los resultados)**

```powershell
docker exec cmm-mysql mysql -ucmm -pcmm cmm -e "SELECT COUNT(*) AS afiliados FROM afiliado; SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1;"
```

**Paso 1. Respaldo de la base y de los comprobantes**

```powershell
New-Item -ItemType Directory -Force respaldos | Out-Null
docker exec cmm-mysql mysqldump -uroot -proot --single-transaction --routines --default-character-set=utf8mb4 --result-file=/tmp/cmm-antes.sql cmm
docker cp cmm-mysql:/tmp/cmm-antes.sql .\respaldos\cmm-antes.sql
if (Test-Path .\data\uploads) { Copy-Item .\data\uploads .\respaldos\uploads-antes -Recurse -Force }
(Get-Item .\respaldos\cmm-antes.sql).Length    # debe ser mayor que 0
```

**Paso 2. Simular un cambio que haya que revertir** (crea una tabla de marca que no existía)

```powershell
docker exec cmm-mysql mysql -ucmm -pcmm cmm -e "CREATE TABLE prueba_rollback (id INT); INSERT INTO prueba_rollback VALUES (1);"
```

**Paso 3. Rollback de la base**

```powershell
docker exec cmm-mysql mysql -uroot -proot -e "DROP DATABASE cmm; CREATE DATABASE cmm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
docker cp .\respaldos\cmm-antes.sql cmm-mysql:/tmp/cmm-antes.sql
docker exec cmm-mysql sh -c "mysql -uroot -proot --default-character-set=utf8mb4 cmm < /tmp/cmm-antes.sql"
```

**Paso 4. Rollback de los comprobantes** (solo si existe `respaldos\uploads-antes`)

```powershell
if (Test-Path .\respaldos\uploads-antes) { Remove-Item .\data\uploads -Recurse -Force -ErrorAction SilentlyContinue; Copy-Item .\respaldos\uploads-antes .\data\uploads -Recurse }
```

**Paso 5. Verificar**

```powershell
docker exec cmm-mysql mysql -ucmm -pcmm cmm -e "SELECT COUNT(*) AS afiliados FROM afiliado; SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1; SHOW TABLES LIKE 'prueba_rollback';"
```

Resultado esperado: la misma cantidad de afiliados y la misma versión que en el paso 0, y **ninguna fila** en `SHOW TABLES LIKE 'prueba_rollback'`.

**Paso 6. Arrancar la aplicación** (`$env:SPRING_PROFILES_ACTIVE="mysql"; .\mvnw.cmd spring-boot:run`): en el log debe figurar `Schema ... is up to date. No migration necessary.` e iniciar sesión debe funcionar.

## 9. Registro de ensayos

Completar cada vez que se ensaye o se ejecute un rollback real.

| Fecha | Entorno | Quién | Resultado de los pasos 0 a 6 | Observaciones |
| --- | --- | --- | --- | --- |
| 22/11/2026 | PC de desarrollo, Docker, MySQL 8.4 | Matías | Respaldo de 50.585 bytes. Antes y después del rollback: 18 afiliados y versión de datos 953. La tabla de prueba `prueba_rollback` desapareció. La app arrancó sin aplicar migraciones (`Schema cmm is up to date`). | La copia de comprobantes (`data/uploads`) no se verificó: el ensayo no muestra si la carpeta existía. El inicio de sesión posterior no quedó confirmado en la salida. |
