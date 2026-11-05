# Validación del candidato 1.0.1

Validación local realizada el 5 de noviembre de 2026. No es una publicación ni una
prueba de actualización física con firma histórica.

```sh
./gradlew -I scripts/unsigned-validation.gradle \
  testDebugUnitTest testReleaseUnitTest lint lintRelease \
  assembleDebug assembleRelease --no-daemon
python3 scripts/check_rs05_compatibility.py --apks
python3 scripts/test_rs05_compatibility.py
python3 scripts/test_verify_release.py
PYTHONPATH=.rs05-audit-tooling/tools python3 scripts/test_rs05_signed_contract.py
python3 scripts/check_distribution_secrets.py
git diff --check
```

El init script es el mismo de CI: evita crear/usar incluso la clave Debug. Los APK
normales de validación son sin firma. Solo la prueba de contrato usa una clave
TEST ONLY efímera en un directorio temporal, eliminada al terminar. El tooling de
esa prueba está fijado a `b15d775833c4167c45d79b5b6bb0254db6b0caea`.

| Comprobación | Resultado |
| --- | --- |
| Unitarias Debug | 142 PASS, 0 FAIL/error/omitidas |
| Unitarias Release | 142 PASS, 0 FAIL/error/omitidas |
| Regresiones RS-05 de identidad/DataStore | 6 PASS |
| Verificador independiente del candidato | 8 PASS |
| Contrato/catálogo contra APK firmado TEST ONLY | 3 PASS |
| Total de casos ejecutados en la batería final | **301 PASS, 0 FAIL** (159 casos distintos; unitarias en dos variantes) |
| lint y lintRelease | PASS; 0 errores/fatales, 10 avisos anteriores por variante |
| assembleDebug y assembleRelease | PASS, ambos sin firma |
| Paquetes compilados, etiquetas, versión, SDK, alineación y ausencia de firma | PASS |
| Identidad/DataStore, detector de secretos, diff check | PASS |

Los avisos lint corresponden a orientación fija, nuevas versiones disponibles de
Kotlin y recursos/iconos preexistentes; no se han suprimido verificaciones. No se
han versionado APK, bundles, keystores, claves ni contraseñas.

El código declara `versionCode=2`, `versionName=1.0.1`, Release
`es.robertodorado.cuadrante`, DEV `.debug`/`-DEV`, SDK 26/36. Los payloads antiguos
se prueban con cadenas literales del codec 1.0.0 y reapertura de DataStore real en
disco. `app_settings`, `incidents`, sus claves históricas e `incidents_v1` se
mantienen compatibles.

Pendiente tras revisión/fusión: prueba manual de interfaz/accesibilidad/voz en
Android, prepare con firma histórica, revisión del candidato real, autorización
separada de publish `v1.0.1` y actualización mediante RobStore encima de 1.0.0.
La voz requiere un reconocedor Android disponible; los botones siempre permiten
confirmar/cancelar. Ver el procedimiento exacto en [RS-05](RS05-DISTRIBUCION.md).
