# Cuadrante App 1.0.1: AP_POLICY_V1 y distribución RS-05

El código candidato declara `versionName=1.0.1`, `versionCode=2`. Esta rama prepara
la aplicación y su CI; no publica tags ni Releases. La Release histórica v1.0.0
permanece intacta. La identidad histórica ya fue recuperada y provisionada por el
propietario fuera de Codex: no repetir el setup, generar claves ni sustituirla.

## Identidad y CI

- Release: `es.robertodorado.cuadrante`; DEV: `es.robertodorado.cuadrante.debug`,
  etiqueta Cuadrante DEV y sufijo `-DEV`. SDK mínimo 26, compilación/objetivo 36.
- Firma fuera de Gradle. CI normal usa `scripts/unsigned-validation.gradle` para
  construir ambos APK sin acceder a identidad privada, incluida la Debug.
- `.github/workflows/android.yml` ejecuta pruebas Debug/Release, lint, builds,
  identidad compilada, compatibilidad DataStore, detector de secretos y pruebas
  de contrato contra un APK firmado con clave efímera TEST ONLY eliminada al salir.
- `.github/android-distribution.json` sigue siendo configuración, sin versiones,
  certificados ni hashes de APK inventados. Las notas reales están en
  `.github/release-notes.md`.
- `.github/workflows/android-distribute.yml` conserva dispatch manual, prepare
  por defecto y motor fijado a `b15d775833c4167c45d79b5b6bb0254db6b0caea`.
  `ANDROID_DISTRIBUTION_TOOLING_SHA` debe seguir apuntando al SHA central aprobado
  que soporta la identidad legacy. No cambiar variables ni entornos desde esta tarea.

## Catálogo y contrato

`robstore.json` en la rama principal conserva el catálogo de la publicación
**existente 1.0.0**, incluido su asset `cuadrante-v1.0.0.apk`. No es un contrato
anticipado de 1.0.1. Cambiarlo a un APK todavía no publicado produciría metadata
falsa. RobStore descubre la Release existente desde su tag histórico.

RS-05 genera `robstore-update.json`, recibo y catálogo definitivo desde los bytes
del APK firmado durante prepare/publicación; el commit de cierre del nuevo tag
incluirá el catálogo real. El nuevo asset RS-05 será `Android-release.apk` y la
versión se leerá del APK: **2 / 1.0.1**. No volver al procedimiento manual antiguo
ni calcular ahora hashes de una firma futura.

`scripts/verify_release.py` es un verificador independiente de solo lectura para
ese candidato. Contrasta contrato generado, versión del código fuente, paquete,
SDK, ausencia de debuggable, alineación 16 KiB, firma, certificado histórico
confiable y SHA-256 exacto. Ejemplo posterior en el equipo del propietario:

```sh
python3 scripts/verify_release.py /ruta/candidato/Android-release.apk \
  --contract /ruta/candidato/robstore-update.json \
  --certificate-sha256 "$TRUSTED_PUBLIC_CERTIFICATE_SHA256" \
  --build-tools /ruta/android/build-tools/35.0.0
```

La huella debe proceder del certificado histórico ya verificado, nunca inferirse
del propio candidato. El comando no firma ni publica y no necesita secretos.

## Persistencia y migración

Ver [AP_POLICY_V1](AP_POLICY_V1.md). Se mantienen los nombres `app_settings` e
`incidents`, claves `pattern_type`, `shift_sequence`, `reference_date` e
`incidents_v1`. Los AP antiguos se importan como concedidos conservando identidad,
sin inventar fechas ni motivos. La nueva clave `ap_policy_v1` almacena bolsas y
solicitudes con evolución individual. Los cambios AP no reescriben `incidents_v1`.

Las pruebas con payload literal antiguo, transacciones y reapertura real de los
DataStore verifican la conservación lógica. No sustituyen una actualización física
con el certificado histórico en el dispositivo.

## Trabajo posterior del propietario y ChatGPT

1. Revisar la PR Draft, probar la interfaz y voz en Android, aprobar y fusionar a
   main. Obtener CI verde para el SHA exacto que se vaya a distribuir.
2. Mantener instalada 1.0.0 Release y registrar sus ajustes, AP, permisos y vacaciones.
   No desinstalar, borrar DataStore, ejecutar `pm clear` ni hacer downgrade.
3. Con autorización del propietario, ejecutar **prepare**, canal release, tag
   futuro `v1.0.1` y source_sha aprobado, con la identidad histórica ya provisionada.
   No ejecutar el setup otra vez. Prepare no debe publicar.
4. Revisar el candidato firmado: run ID, SHA-256 real, recibo, certificado histórico,
   paquete, 2 / 1.0.1, SDK, alineación, firma y catálogo derivado del APK. Probar AP
   y accesibilidad. Conservar la instalación 1.0.0 necesaria para la prueba RobStore.
5. Solo después de aprobación explícita separada, **publish** usando el mismo
   candidate_run y expected_sha256 revisados, tag `v1.0.1` y confirmación
   `PUBLICAR v1.0.1`, con aprobación del entorno protegido.
6. Refrescar RobStore: debe detectar code 2 > code 1 y ofrecer actualización.
   Descargar e instalar ENCIMA de 1.0.0. Verificar versión 1.0.1 y conservación de
   ajustes, AP (ahora concedidos), permisos y vacaciones, además de saldos e histórico.

No están realizadas en esta tarea la firma histórica, revisión del candidato de
producción, publicación ni actualización física. No se autoriza ejecutarlas aquí.
