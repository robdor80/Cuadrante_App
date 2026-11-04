# Preparación de Cuadrante App para RobStore

Esta rama prepara el contrato de descubrimiento de RobStore RS-02/RS-04, **sin publicar todavía un APK ni una Release**.

## Contrato
- `robstore.json` v1 se versiona en la raíz y debe estar presente en el commit apuntado por la etiqueta de la Release.
- Icono: launcher PNG ya existente en `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` (sin duplicarlo).
- Paquete: `es.robertodorado.cuadrante`.
- Adjunto previsto: `cuadrante-v1.0.0.apk`.
- Canal: Release estable publicada, no Draft ni prerelease.

## Bloqueo de publicación: firma
La compilación de `release` no tiene `signingConfig` de distribución configurada en `app/build.gradle.kts`.
No utilizar el certificado Debug efímero de CI para la distribución final.

Antes de crear una etiqueta/Release o activar el topic de distribución:
1. Comprobar si existe una instalación anterior de Cuadrante App y **su certificado de firma y versionCode**, con consentimiento del propietario. No desinstalar ni borrar datos automáticamente.
2. Elegir una clave de firma Release persistente, guardada de forma segura **fuera de Git** y con copia de respaldo. Si ya existe una firma de distribución previa, reutilizarla; nunca sustituirla por una nueva sin evaluar el impacto.
3. Preparar una compilación Release **firmada, zipalign y apksigner verify**; comprobar `applicationId`, nombre, `versionCode` y `versionName`, y que el APK no contenga secretos.
4. Ejecutar tests y una prueba de instalación en dispositivo sin poner en riesgo datos existentes. La instalación nueva y la actualización requieren planes distintos.
5. Publicar solo tras aprobar las comprobaciones: etiqueta del commit con `robstore.json` e icono, Release estable con el adjunto exacto y SHA-256, y topic `robstore-app` si aún no está asignado.
6. Probar desde RobStore DEV en Realme y después Galaxy Tab S9+; conservar PR/ramas RS-03/RS-04 sin fusionar.

Las credenciales, claves de firma y datos locales nunca deben añadirse al repositorio, informes o artefactos públicos.
