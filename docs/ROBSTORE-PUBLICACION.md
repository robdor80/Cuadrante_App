# Primera publicación de Cuadrante App en RobStore

> Archivo histórico de 1.0.0. Sus versiones, comandos manuales y hashes NO son el procedimiento de 1.0.1. Para la próxima publicación seguir [RS-05 vigente](RS05-DISTRIBUCION.md); no repetir setup, crear claves ni publicar manualmente.

> **Estado actual — 4 de noviembre de 2026:** la Release estable [v1.0.0](https://github.com/robdor80/Cuadrante_App/releases/tag/v1.0.0) **ya está publicada**. La etiqueta apunta al commit `8513721961fd980347bf63c7bc3ab6470cf1d329`; el APK firmado `cuadrante-v1.0.0.apk` fue descargado e instalado correctamente desde RobStore DEV en una Galaxy Tab S9+. La clave se generó en un dispositivo del propietario y su respaldo cifrado está custodiado fuera de GitHub. Esta guía recoge el procedimiento usado y las precauciones para próximas versiones. **No ejecutar de nuevo los comandos de creación/publicación contra `v1.0.0`, no sobrescribir su etiqueta o asset y no reutilizar `versionCode=1` para actualizaciones.** Los textos «pendiente» son históricos. Para la siguiente versión deben revisarse tag, nombre de APK, `robstore.json`, `versionName`, `versionCode` y certificado de firma.


Estado: preparación en la PR Draft #1, rama `codex/robstore-primer-release-preparacion`.
**No crear todavía etiquetas ni Releases, fusionar la PR, instalar/desinstalar apps ni modificar RobStore.**
Los comandos de firma y publicación de esta guía son para una ejecución posterior,
cuando se hayan resuelto los bloqueos y exista autorización de publicación.
Resultados observados: [ROBSTORE-AUDITORIA.md](ROBSTORE-AUDITORIA.md).

## Identidad y contrato

- `robstore.json` v1 está en la raíz. Tiene que existir, junto con el icono, en el commit etiquetado.
- Release: paquete `es.robertodorado.cuadrante`, nombre «Cuadrante App», versión `1.0.0`, `versionCode=1` inicialmente; SDK mínimo 26 y objetivo 36.
- Debug: paquete independiente `es.robertodorado.cuadrante.debug`, nombre «Cuadrante DEV», versión `1.0.0-DEV`. Su certificado Debug no es de distribución.
- Icono: PNG existente `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`, 192 × 192; no se duplica ni rediseña.
- APK exacto: `cuadrante-v1.0.0.apk`. No adjuntar una APK Debug ni una APK sin firmar.
- El repositorio debe ser público, no archivado y tener el topic `robstore-app` (ya presente al auditar).
- RobStore descubre una Release **publicada, estable, no Draft ni prerelease**, y lee contrato e icono de su etiqueta. El asset debe ser único, estar completamente subido y tener MIME APK u octet-stream.

No añadir una versión o un SHA-256 inventados al contrato v1: el SHA-256 corresponde
a los bytes del APK **final firmado**, no al APK sin firmar ni al certificado.

## Firma persistente: decisión pendiente

No se ha encontrado configuración previa de firma Release en los archivos
versionados ni en su historial. No hay Releases publicadas observadas. Esto **no**
demuestra que el propietario carezca de un keystore externo o de APK anteriores.

Se mantiene `assembleRelease` sin `signingConfig`: permite validar la compilación
sin claves. La distribución se firma después con `apksigner`, usando un keystore
persistente externo. No se introducen contraseñas en Gradle, propiedades del
proyecto, argumentos de proceso, CI ni documentación. No se genera ninguna clave
en esta preparación. El certificado Debug creado por las herramientas para
compilar Debug solo sirve al desarrollo y no se usa para firmar Release.

Antes de firmar:

1. Inventariar la instalación y APK anteriores, si existen. Recuperar el keystore original, alias y contraseñas desde su custodio; comprobar certificado y vigencia.
2. Para una actualización, reutilizar la misma clave. Un APK permite leer el certificado público, **no recuperar la clave privada**.
3. Si nunca hubo distribución y no hay instalaciones que deban actualizarse, el propietario deberá provisionar una clave persistente en un equipo controlado, fuera de Git, con copia cifrada de respaldo y prueba de recuperación. No usar una clave temporal ni la de Debug.
4. Custodiar archivo, alias y contraseñas en almacenamiento privado y gestor de secretos; registrar por separado la huella SHA-256 **pública** del certificado aprobado. No subir el keystore ni copias de datos a GitHub.

`.gitignore` excluye formatos habituales de claves, propiedades de firma, APK,
bundles, firmas auxiliares y `dist/`. No sustituye una revisión antes del commit;
no protege archivos ya versionados o con extensiones diferentes.

## Instalación anterior y conservación de datos

Sin un dispositivo conectado no puede determinarse si hay instalación anterior,
su firma o su `versionCode`. No probar una instalación a ciegas. En el equipo del
propietario, seleccionar explícitamente el dispositivo y hacer solo lecturas:

```bash
export PATH="$ANDROID_HOME/build-tools/35.0.0:$ANDROID_HOME/platform-tools:$PATH"
export ANDROID_SERIAL='SERIAL_DEL_DISPOSITIVO'
adb devices -l
adb shell pm list packages es.robertodorado.cuadrante
adb shell dumpsys package es.robertodorado.cuadrante
adb shell pm path es.robertodorado.cuadrante
```

El último comando devuelve `package:/.../base.apk` y, si existen, splits. Copiar
el APK base a un directorio **privado externo al repositorio** y leer su identidad:

```bash
# Sustituir la ruta por la devuelta por pm path; no copiar el prefijo package:.
adb pull '/ruta/devuelta/base.apk' '/directorio/privado/cuadrante-anterior.apk'
aapt dump badging '/directorio/privado/cuadrante-anterior.apk'
apksigner verify --verbose --print-certs '/directorio/privado/cuadrante-anterior.apk'
```

Comparar `package`, `versionCode` y SHA-256 del **certificado**, no el hash del
archivo, con el candidato. Revisar también que sea el usuario/perfil correcto.

| Situación | Decisión segura |
| --- | --- |
| No existe el paquete Release | Instalación nueva; aun así usar la clave persistente acordada. |
| Mismo paquete y mismo certificado | Actualización posible, sujeta a versión y prueba de datos. |
| Mismo paquete y certificado distinto, incluida una antigua Debug | Bloquear instalación sobre ese paquete. No desinstalar ni borrar datos para resolverlo. Recuperar la clave original o diseñar/probar una migración explícita. |
| Clave original perdida | No se puede actualizar normalmente conservando la identidad; no basta renombrar la APK o cambiar su versionCode. No improvisar rotación de firma. |
| Versión instalada con `versionCode >= 1` | Antes de publicar, subir el `versionCode` del candidato por encima del instalado, manteniendo `versionName=1.0.0` si procede, y repetir todas las comprobaciones. RobStore podría no ofrecer una versión de igual código. |

Cambiar el `applicationId` Release crea otra app y otro almacenamiento; no migra
los datos anteriores. La separación de Debug protege las instalaciones futuras,
pero no cambia ni recupera una antigua Debug instalada con el paquete Release.

Los datos actuales son Preferences DataStore en `files/datastore/`:
`app_settings.preferences_pb` (claves `pattern_type`, `shift_sequence`,
`reference_date`) e `incidents.preferences_pb` (`incidents_v1`). No se modifican
sus nombres, claves ni codecs en esta PR. Una actualización compatible conserva
el almacenamiento privado; eso debe probarse con datos representativos.

`allowBackup=false` y las reglas de backup/extracción impiden confiar en una copia
automática o en `adb backup`. No se ha encontrado exportación/restauración de datos
en la app. Si la antigua app es depurable y el propietario lo autoriza, `run-as`
podría permitir una copia privada; eso no constituye una migración validada. Para
una app no depurable no asumir que `adb` puede extraer sus datos privados.
No tocar la instalación con datos reales hasta acordar y validar recuperación.

La prueba posterior deberá usar primero un dispositivo/perfil de pruebas: crear
configuración, fecha de referencia, incidencias, permisos y vacaciones con la
versión anterior; cerrar/abrir; actualizar con firma compatible y código superior;
volver a abrir y comparar todos los valores. Solo entonces evaluar el dispositivo
real. No ejecutar `uninstall`, `pm clear`, flags de downgrade ni restauraciones
manuales sobre los datos reales. En esta preparación no se realiza instalación.

## Compilar sin secretos

Requisitos: JDK 17 o compatible (validación local con JDK 17), Python **3.11+**,
Android SDK Platform 36, Build Tools 35.0.0, Platform Tools y acceso a Maven/Gradle.
Configurar `ANDROID_HOME` o `local.properties` (ignorado). Si hay proxy, configurarlo
en el entorno local; no añadir sus credenciales al proyecto.

```bash
git switch codex/robstore-primer-release-preparacion
git status --short
./gradlew test lint assembleDebug assembleRelease --no-daemon
export PATH="$ANDROID_HOME/build-tools/35.0.0:$ANDROID_HOME/platform-tools:$PATH"
aapt dump badging app/build/outputs/apk/release/app-release-unsigned.apk
zipalign -c -P 16 4 app/build/outputs/apk/release/app-release-unsigned.apk
```

La salida de Release es `app-release-unsigned.apk`. Que compile o esté alineada
no significa que sea instalable como distribución. `apksigner verify` debe fallar
antes de firmarla. No usar `signingReport` como evidencia de firma final.

## Firmar y comprobar el archivo final (posteriormente)

Ejecutar sobre un commit revisado, con árbol limpio, después de compilarlo.
Guardar la salida final en un directorio privado **fuera del checkout**. Las rutas
son ejemplos que hay que sustituir; nunca introducir contraseñas en esos comandos.

```bash
set -euo pipefail
export RELEASE_DIR='/directorio/privado/cuadrante-1.0.0'
export CUADRANTE_KEYSTORE='/directorio/privado/claves/cuadrante-release.jks'
export CUADRANTE_KEY_ALIAS='ALIAS_PERSISTENTE'
mkdir -p "$RELEASE_DIR"
# Pide la contraseña interactivamente. Contrastar huella y vigencia con el registro aprobado.
keytool -list -v -keystore "$CUADRANTE_KEYSTORE" -alias "$CUADRANTE_KEY_ALIAS"

# Alinear ANTES de firmar; no modificar el APK después de firmarlo.
zipalign -P 16 -f 4 app/build/outputs/apk/release/app-release-unsigned.apk \
  "$RELEASE_DIR/cuadrante-aligned.apk"
# apksigner solicita las contraseñas; no pasar --ks-pass pass:... ni --key-pass pass:...
apksigner sign --ks "$CUADRANTE_KEYSTORE" --ks-key-alias "$CUADRANTE_KEY_ALIAS" \
  --out "$RELEASE_DIR/cuadrante-v1.0.0.apk" "$RELEASE_DIR/cuadrante-aligned.apk"

# Huella obtenida de un registro confiable independiente del candidato.
export CUADRANTE_CERT_SHA256='HUELLA_SHA256_PUBLICA_APROBADA_DE_64_HEXADECIMALES'
python3 scripts/verify_release.py "$RELEASE_DIR/cuadrante-v1.0.0.apk" \
  --certificate-sha256 "$CUADRANTE_CERT_SHA256" --version-code 1
(cd "$RELEASE_DIR" && sha256sum cuadrante-v1.0.0.apk > cuadrante-v1.0.0.apk.sha256)
(cd "$RELEASE_DIR" && sha256sum --check cuadrante-v1.0.0.apk.sha256)
```

Si se incrementó `versionCode` por una instalación previa, pasar su valor aprobado
a `--version-code`. El verificador exige paquete y versión Release, nombre exacto,
SDK 26/36, APK no depurable, alineación a 16 KiB, firma válida y certificado igual
al esperado; rechaza el certificado Android Debug habitual. Imprime el SHA-256 del
APK final. No comprueba la recuperación de datos ni sustituye la prueba física.
No usar la huella calculada del propio candidato como única fuente de confianza.

Guardar como evidencia pública el commit, versión/código, tamaño, SHA-256 del APK,
huella pública del certificado y resultados de verificación. **El SHA-256 final
está pendiente: no existe aún una APK de distribución firmada.** Los hashes de
compilaciones sin firmar del informe no sirven para publicar la Release.

## Publicar (solo después de resolver bloqueos y recibir autorización)

Requisitos: pruebas físicas de instalación nueva y actualización compatibles,
clave y respaldo custodiados, verificador correcto, árbol limpio, commit revisado,
contrato/icono presentes, notas y SHA-256 final comprobados. No es necesario
modificar RobStore. No ejecutar ahora estos comandos:

```bash
set -euo pipefail
export RELEASE_COMMIT='SHA_COMPLETO_DEL_COMMIT_REVISADO_Y_COMPILADO'
test "$(git rev-parse HEAD)" = "$RELEASE_COMMIT"
test -z "$(git status --porcelain)"
git show "$RELEASE_COMMIT:robstore.json"
git cat-file -e "$RELEASE_COMMIT:app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"
# Detenerse si v1.0.0 ya existe: nunca sobrescribir una etiqueta o asset publicado.
git ls-remote --tags origin 'refs/tags/v1.0.0*'
gh release list --repo robdor80/Cuadrante_App
# Ejecutar solo si se ha confirmado que la etiqueta y Release no existen.
git tag -a v1.0.0 "$RELEASE_COMMIT" -m 'Cuadrante App 1.0.0'
git push origin refs/tags/v1.0.0
# Preparar notas fuera de Git: cambios, SDK mínimo, código, huella pública y SHA-256 final.
gh release create v1.0.0 \
  "$RELEASE_DIR/cuadrante-v1.0.0.apk" "$RELEASE_DIR/cuadrante-v1.0.0.apk.sha256" \
  --repo robdor80/Cuadrante_App --verify-tag --title 'Cuadrante App 1.0.0' \
  --notes-file "$RELEASE_DIR/notas-publicacion.md"
gh release view v1.0.0 --repo robdor80/Cuadrante_App \
  --json tagName,isDraft,isPrerelease,publishedAt,assets,url
```

No añadir `--draft` ni `--prerelease`. El topic ya está presente: no hace falta
modificarlo. Comprobar nombre, tamaño, tipo y estado del asset en GitHub. Descargar
el adjunto publicado a **otro** directorio, comparar su SHA-256 con el aprobado y
repetir `verify_release.py` con el mismo certificado/código. No reconstruir la APK
para compararla: se verifican los mismos bytes que se publicaron.

```bash
export DOWNLOAD_DIR='/otro/directorio/privado/comprobacion-publicada'
mkdir -p "$DOWNLOAD_DIR"
gh release download v1.0.0 --repo robdor80/Cuadrante_App --dir "$DOWNLOAD_DIR" \
  --pattern 'cuadrante-v1.0.0.apk' --pattern 'cuadrante-v1.0.0.apk.sha256'
cmp "$RELEASE_DIR/cuadrante-v1.0.0.apk.sha256" "$DOWNLOAD_DIR/cuadrante-v1.0.0.apk.sha256"
(cd "$DOWNLOAD_DIR" && sha256sum --check cuadrante-v1.0.0.apk.sha256)
python3 scripts/verify_release.py "$DOWNLOAD_DIR/cuadrante-v1.0.0.apk" \
  --certificate-sha256 "$CUADRANTE_CERT_SHA256" --version-code 1
gh api repos/robdor80/Cuadrante_App/releases/tags/v1.0.0 \
  --jq '{tag_name,draft,prerelease,published_at,assets:[.assets[]|{name,size,state,content_type,digest}]}'
```

Usar también aquí el `versionCode` aprobado si se cambió respecto a 1. Comparar
el `digest` de GitHub, si está disponible, con `sha256:` seguido del hash local.

Después, en una sesión autorizada, refrescar RobStore DEV RS-04 y probar descarga,
validación e instalación primero en Realme y después en Galaxy Tab S9+, conservando
los datos previos. Una Release estable puede quedar visible a todos los usuarios
del catálogo; no publicarla solo para sortear un bloqueo de prueba.
