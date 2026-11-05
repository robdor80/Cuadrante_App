# Auditoría para la primera Release — 4 de noviembre de 2026 (UTC)

> Archivo histórico de 1.0.0. Sus versiones, comandos manuales y hashes NO son el procedimiento de 1.0.1. Para la próxima publicación seguir [RS-05 vigente](RS05-DISTRIBUCION.md); no repetir setup, crear claves ni publicar manualmente.

> **Actualización de estado — 4 de noviembre de 2026:** este documento conserva la auditoría *previa* a la publicación. Posteriormente se publicó [v1.0.0](https://github.com/robdor80/Cuadrante_App/releases/tag/v1.0.0), con etiqueta en `8513721961fd980347bf63c7bc3ab6470cf1d329`, APK firmado externamente y respaldado por clave privada custodiada fuera de Git. En RobStore DEV se comprobó descubrimiento, SHA-256 indicado por GitHub, descarga, instalación nueva y arranque en Galaxy Tab S9+. Los apartados que dicen «pendiente», «no se publica» o «no existe Release» describen únicamente el estado *anterior*. Sigue sin haberse probado una actualización sobre una instalación antigua con datos.


Alcance: `robdor80/Cuadrante_App`, rama
`codex/robstore-primer-release-preparacion`, PR Draft #1. Punto de partida:
`33ba8e589434ebff3a03a61a15edc6d7990f1121`. No se publica una Release en esta tarea.

## Contrato e identidad

| Elemento | Resultado |
| --- | --- |
| `robstore.json` | JSON UTF-8 de 307 bytes, esquema entero v1; campos, límites y rutas conformes al contrato RS-02 usado por RS-04. |
| Paquete Release | `es.robertodorado.cuadrante`; coincide con el contrato y no se cambia. |
| Versión | `versionName=1.0.0`, `versionCode=1`; pendiente contrastar con cualquier instalación previa. |
| SDK | mínimo 26, objetivo y compilación 36. |
| Adjunto | `cuadrante-v1.0.0.apk`, nombre exacto válido. |
| Icono | PNG RGBA existente, 192 × 192, 68 467 bytes; inspeccionado visualmente, sin cambios. |
| SHA-256 del icono | `37f782867cf24c82dc2d479f2606a9b67ac40a32c2f529fd8d7b06f5bebddc10` |
| GitHub observado | Repositorio público, topic `robstore-app` presente; listado de Releases vacío. PR #1 abierta, Draft, sin fusionar. |

El manifiesto y el icono se conservarán en el commit etiquetado. El nombre de
asset es una condición del catálogo, no renombra la salida de Gradle.

## Ajustes preparados

- Debug pasa a `.debug`, etiqueta «Cuadrante DEV» y sufijo de versión `-DEV`, para que futuras pruebas de desarrollo no compitan por el paquete de producción.
- Release mantiene paquete, etiqueta y versión. Sigue sin firma de distribución y sin claves en Gradle; se documenta firma externa persistente mediante `apksigner`.
- Se conserva el icono, el contrato y el almacenamiento existente; no hay cambios en repositorios DataStore, nombres de archivo, claves o codecs.
- Wrapper ejecutable y checksum oficial SHA-256 de Gradle 8.13 fijado; sin cambiar Gradle ni dependencias.
- Exclusiones adicionales para material privado habitual y artefactos generados. No se añaden claves, contraseñas ni certificados privados.
- `scripts/verify_release.py` valida un candidato contra paquete, nombre, versión/código, SDK, no depurable, alineación y certificado público esperado; emite el SHA-256 final solo tras pasar las comprobaciones.
- [Guía de publicación](ROBSTORE-PUBLICACION.md): inventario anterior, custodia, compilación, firma interactiva, verificación, checksum, publicación futura y descarga de comprobación.

## Firma y datos

No se ha encontrado `signingConfig` Release, `storeFile`, contraseñas o keystores
versionados, ni configuración previa de firma en el historial de Gradle/CI.
La configuración Release solo incluía opciones de optimización. No se afirma
que no exista una clave fuera de Git: debe confirmarlo el custodio.

Antes del cambio, Debug y Release compartían paquete. Una antigua Debug podría
seguir instalada como `es.robertodorado.cuadrante`: el nuevo sufijo Debug no la
migra ni modifica. Android requiere firma compatible para actualizar ese paquete.
Una clave Release nueva puede ser incompatible aunque coincidan paquete y versión.

La configuración y las incidencias se almacenan en `app_settings.preferences_pb`
e `incidents.preferences_pb`. El backup automático está desactivado; no se ha
encontrado exportación/restauración en la app. No asumir recuperación después de
una desinstalación ni copiar datos entre paquetes sin una migración validada.
Las pruebas unitarias de persistencia no demuestran por sí solas una actualización
real de una instalación anterior.

`adb devices -l` no muestra dispositivos conectados. No se ha instalado,
desinstalado, actualizado ni borrado ninguna aplicación o dato.

## Validación local

Entorno: Temurin JDK **17.0.21+9**, Gradle **8.13**, AGP **8.13.2**, Kotlin
**2.2.21**, SDK Platform **36**, Build Tools **35.0.0**, Python **3.12.14**.
El entorno inicial solo tenía un JRE 21 sin `javac`; la primera ejecución falló
por falta de compilador. Se preparó un JDK completo fuera del repositorio y se
repitió toda la validación con éxito. No se cambian dependencias para sortear fallos.

Comando ejecutado sobre las fuentes finales:

```bash
./gradlew test lint assembleDebug assembleRelease --no-daemon --console=plain
```

Resultado conjunto: **BUILD SUCCESSFUL**, 5 min 51 s, 105 tareas accionables.
Los resultados son locales; no se presentan como checks remotos de GitHub.

| Comprobación | Resultado |
| --- | --- |
| Tests Debug | 70; 0 fallos, 0 errores, 0 omitidos. |
| Tests Release | 70; 0 fallos, 0 errores, 0 omitidos. |
| `lint` | 0 errores, 10 avisos. Sin supresiones ni baseline nuevos. |
| `assembleDebug` | Correcto; `app-debug.apk`, 13 639 285 bytes. |
| `assembleRelease` | Correcto; `app-release-unsigned.apk`, 9 841 147 bytes. |
| `aapt dump badging` | Debug: `.debug` / Cuadrante DEV / 1.0.0-DEV. Release: paquete del contrato / Cuadrante App / 1.0.0. Ambos código 1, SDK 26/36. |
| `zipalign -c -P 16 4` | Correcto para ambos APK. |
| `apksigner verify --verbose --print-certs` | Debug válido (v2, CN=Android Debug). Release rechazado por no tener firma: `DOES NOT VERIFY`, falta `META-INF/MANIFEST.MF`. Este fallo esperado impide distribuirlo. |
| Depuración | Debug depurable; Release no depurable. |
| Permisos/archivos | Sin permiso INTERNET ni archivos con extensiones habituales de claves privadas dentro de los APK. Esto no sustituye una auditoría completa de contenido. |
| Verificador de Release | Sintaxis Python válida; rechaza APK inexistente, APK Debug renombrado y APK Release sin firmar. El recorrido satisfactorio con firma persistente queda pendiente del candidato real. |
| Repositorio | `git diff --check` correcto; exclusiones de claves y artefactos comprobadas; no se versionan APK, keystores, secretos ni datos privados. |

Los 10 avisos de lint son: `LockedOrientationActivity` (1), `DiscouragedApi` (1),
`NewerVersionAvailable` de Kotlin/Compose (2), `UnusedResources` (2),
`IconXmlAndPng` (2), `MonochromeLauncherIcon` (2). La orientación fija puede ser
ignorada en Android 16/pantallas grandes: requiere prueba física en tablet.
Los recursos adaptativos `mipmap-anydpi-v33` sí incluyen monochrome; se conservan
los PNG usados por el contrato y los recursos existentes, sin ocultar avisos.

Gradle avisa de que no puede eliminar símbolos de `libandroidx.graphics.path.so`
y `libdatastore_shared_counter.so`; las empaqueta tal cual. No impide compilar ni
superar la comprobación de alineación del APK. No se ha validado ejecución nativa
en un dispositivo real.

SHA-256 **solo de referencia de esta compilación local sin firmar**:

```text
bd7ee36f3b063ca3fe111788d119382f224e0e4c7a0ae6a509c010524bb9d370  app-release-unsigned.apk
```

No usar ese hash ni ese archivo para publicar `cuadrante-v1.0.0.apk`. Firmar cambia
los bytes; el hash final debe calcularse después de firmar. Los artefactos de esta
validación quedan fuera del control de versiones y no se adjuntan a una Release.

## Bloqueos para la publicación

1. Identificar cualquier instalación previa: paquete, usuario/perfil, `versionCode`, certificado y datos que deben conservarse.
2. Recuperar/seleccionar la clave persistente adecuada y comprobar su respaldo. No se ha generado una clave de distribución temporal ni se ha reutilizado Debug para Release.
3. Si la instalación previa tiene código 1 o superior, acordar un código estrictamente mayor y repetir compilación/verificación antes de publicar.
4. Firmar la APK con la clave aprobada y verificarla. El **SHA-256 de `cuadrante-v1.0.0.apk` firmado está pendiente**, porque aún no existe ese artefacto. El hash del certificado y el de una APK sin firmar no lo sustituyen.
5. Probar instalación nueva y actualización con datos representativos en un dispositivo/perfil de pruebas; validar recuperación y conservación antes de tocar una instalación real.
6. Obtener autorización posterior para etiquetar/publicar; después probar el flujo RS-04 en Realme y Galaxy Tab S9+. No modificar RobStore para eludir un bloqueo.

La PR permanece Draft; no se crea etiqueta ni Release, no se fusiona y no se
modifica RobStore. La preparación no declara todavía una Release apta para
publicación: faltan firma persistente, evidencia de compatibilidad y prueba física.
