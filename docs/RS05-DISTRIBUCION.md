# Cuadrante App: infraestructura RS-05 e identidad histórica

Esta tarea prepara infraestructura. El producto sigue en code 1 / 1.0.0; no hay
nueva versión, AP_POLICY_V1, prepare, publish, tag ni Release. No se tocan datos ni
código funcional Android. La Release estable v1.0.0 permanece intacta.

## Archivos permanentes

- `.github/workflows/android.yml`: push y PR, checkout del SHA exacto, tests Debug
  y Release, lint, lintRelease, compilación de ambos APK sin firma, identidad
  compilada, versión frente a Gradle, SDK, etiquetas, alineación, historial sin
  claves y regresión de DataStore. No usa secretos ni entornos de firma.
- `.github/android-distribution.json`: plantilla auditada, repo exacto,
  `Cuadrante App`, paquetes Release `es.robertodorado.cuadrante` y DEV
  `es.robertodorado.cuadrante.debug`, módulo app, SDK 26/36, icono oficial,
  `robstorePolicy=false` y notas versionadas.
- `.github/workflows/android-distribute.yml`: sólo dispatch manual, prepare por
  defecto, motor auditado fijado a
  `b15d775833c4167c45d79b5b6bb0254db6b0caea`; nunca `@main`.
- `.github/release-notes.md`: `<!-- NOT_FOR_PUBLICATION -->` impide preparar o
  publicar accidentalmente. No sustituirlo hasta tener una versión real revisada.
- `scripts/check_rs05_compatibility.py` y tests: conservan identidad Release/DEV,
  SDK, DataStore y claves. `unsigned-validation.gradle` evita crear siquiera una
  clave Debug durante CI normal; tests de firma usan sólo claves efímeras fuera
  del repositorio, eliminadas al terminar. Los artifacts son informes, no claves.

La CI consulta y usa el tooling auditado original sólo para verificar el contrato
contra un APK de test firmado. El motor de distribución descarga su tooling con
`ANDROID_DISTRIBUTION_TOOLING_SHA`, variable distinta que debe fijar Roberto a un
SHA central completo aprobado que contenga la adaptación legacy. No configurar
esa variable al tooling antiguo para distribuir Cuadrante.

## Firma histórica: procedimiento obligatorio del propietario

Ver [procedimiento central completo de importación y verificación](https://github.com/robdor80/RobStore/blob/codex/rs05-legacy-baseline/docs/RS-05-CUADRANTE-LEGACY.md).
Después de fusionar la PR central, consultar ese documento desde el commit
completo aprobado de RobStore/main y ejecutar su `android_owner_setup.py`.
La rama del enlace es sólo para revisar esta propuesta, no una fuente de confianza
para habilitar distribución.

La herramienta nueva acepta `--import-release-keystore` y `--legacy-tag v1.0.0`;
NO utilizar el modo de generación Release. Ejecutarla personalmente desde
Android/Termux, jamás Codex/Cloud/CI. Se requieren herramientas Android nativas
para la arquitectura del teléfono y Java; el documento explica el adaptador de
rutas para Termux. La disponibilidad en el teléfono debe comprobarla Roberto.

El propietario descargará `cuadrante-v1.0.0.apk` de la Release oficial y verificará
SHA-256 `e514c058212eb1bd11f9747488f82ca22b904de40ce46a250ea93ebe462e362a`.
Con `apksigner verify --verbose --print-certs` comprobará la huella del certificado:
`c85a092878e85a35b8ca84132bdf00314da3e90dae80fc137ac52a27d11491e7`.
Después exportará sólo el certificado público del keystore recuperado mediante
`keytool -exportcert` (password por prompt/env, nunca argv) y comparará ambos.
La herramienta repite la comparación antes de importar la clave privada.
Ante diferencias: abortar, sin generar otra identidad Release.

El importador preserva la clave original, normaliza una copia privada a PKCS12 y
permite generar DEV independiente. Exige contraseñas sin eco, respaldo cifrado
AES256/GPG, copia personal en Drive privado y recuperación de la copia descargada.
Sólo después permite cargar secretos en `android-release` y `android-dev`, con
revisión de Roberto y rama main; `android-publish` recibe sólo huellas públicas.
Los pins existentes se comprueban antes de sustituir secretos. No se configura
ningún secreto real en esta tarea.

## Publicación legacy y catálogo

El robstore.json de main describe exclusivamente v1.0.0 y se conserva sin
inventar code/name, SHA o assets futuros. RobStore sigue descubriendo la Release
existente a través de su tag histórico.

La PR central incorpora `tools/approved-legacy-publications.json`: aprobación
exacta de repo, tag, commit histórico, IDs de Release/asset, paquete, 1/1.0.0,
SDK, digest y certificado. El motor comprueba GitHub y bytes del APK real para
obtener el contrato anterior en memoria, sin añadir archivos a v1.0.0. Un
contrato ausente sin aprobación sigue siendo error. El nuevo code debe ser >1.

Una publicación P04 futura generará `robstore-update.json` y `robstore.json`
desde el APK firmado real en un commit de cierre de su nuevo tag. El contrato
incluirá paquete, asset, code/name reales, nombre, descripción e icono. Las pruebas
`scripts/test_rs05_signed_contract.py` contrastan esos campos contra un APK real
firmado con clave temporal y rechazan metadatos/certificados incoherentes.
`scripts/verify_release.py` sigue siendo sólo el verificador histórico de 1.0.0;
no utilizarlo como puerta de publicación de versiones posteriores.

## Datos y prueba física futura

Conservar obligatoriamente:

- Paquete Release y su certificado histórico.
- `app_settings.preferences_pb`: `pattern_type`, `shift_sequence`, `reference_date`.
- `incidents.preferences_pb`: `incidents_v1`.

No uninstall, `pm clear`, downgrade, cambio de firma/paquete, borrado de DataStore
ni migraciones destructivas. AP_POLICY_V1 y la migración de incidencias quedan
para otra tarea. Las reglas CI detectan cambios en estos identificadores; no
prueban por sí mismas la conservación física de todos los datos.

Secuencia posterior, fuera de esta tarea:

1. Revisar y fusionar personalmente ambas PR. Seleccionar tooling central aprobado.
2. Recuperar/importar identidad histórica, verificar respaldo y entornos protegidos.
3. Mantener instalada v1.0.0 con ajustes e incidencias de prueba registrados.
4. Implementar futura versión (p.ej. 2 / 1.1.0), notas definitivas y fusionar a main.
5. Obtener PASS de `android.yml` para ese HEAD de main; dispatch manual prepare
   con ese source_sha y tag futuro todavía inexistente.
6. Firma aislada con identidad histórica; revisar run ID, APK y SHA-256.
7. Instalar como actualización sobre v1.0.0 y confirmar conservación de datos.
8. Sólo con autorización explícita separada: publish, mismo run/hash y
   `PUBLICAR <tag>`, con aprobación de entorno.
9. RobStore refresca catálogo: instalada code 1, publicada superior, «Actualización
   disponible». Descargar/entregar al instalador Android y volver a comprobar datos.

Los bloqueos actuales son intencionados: PR central sin fusionar, tooling nuevo
aún fuera del historial aprobado, secretos/protecciones no configurados, notas
provisionales y code 1 ya utilizado. La comprobación privada de firma y la prueba
de actualización requieren intervención física de Roberto. No están realizadas.
