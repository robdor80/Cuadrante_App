# Cuadrante App

Base Android nativa para una aplicación local de calendario de turnos.

## Tecnología

- Kotlin y Jetpack Compose
- Material 3
- Gradle Kotlin DSL
- `java.time.LocalDate` (API mínima 26)
- Sin backend, cuentas, anuncios ni permisos de red

## Estructura

- `ui`: interfaz Compose y tema visual
- `model`: modelos de dominio independientes del idioma
- `calculation`: motor genérico de cálculo de ciclos por fecha
- `data/local`: persistencia local de la configuración mediante DataStore
- `util`: conversiones de fecha independientes de zona horaria

## Compilación

```shell
./gradlew test lint assembleDebug assembleRelease --no-daemon
```

Debug se instala como `es.robertodorado.cuadrante.debug` / «Cuadrante DEV».
Release conserva `es.robertodorado.cuadrante` y se compila sin firma: antes de
distribuirla hay que usar una clave persistente externa y verificar compatibilidad
con instalaciones anteriores. Véase la [guía de publicación en RobStore](docs/ROBSTORE-PUBLICACION.md)
y el [informe de validación](docs/ROBSTORE-AUDITORIA.md).

## Publicación en RobStore

La **Release estable [v1.0.0](https://github.com/robdor80/Cuadrante_App/releases/tag/v1.0.0)** se publicó el 4 de noviembre de 2026 desde el commit `8513721961fd980347bf63c7bc3ab6470cf1d329`. Contiene `cuadrante-v1.0.0.apk` firmado fuera del repositorio con una clave privada persistente. RobStore DEV descubrió automáticamente la Release y descargó, verificó e instaló la aplicación en una Samsung Galaxy Tab S9+. Este ensayo confirma una instalación nueva, **no** una actualización sobre datos existentes.

La clave privada no está en GitHub ni en el proyecto; su custodio conserva un respaldo cifrado fuera del repositorio. Para publicar versiones posteriores: incrementar `versionCode`, utilizar un nuevo tag/Release y nombre de asset coherente con `robstore.json`, y firmar con la misma clave. No sustituir ni mover la etiqueta `v1.0.0`. La guía histórica de preparación y la auditoría se conservan en `docs/`.

## Motor de turnos

`ShiftCalculator(referenceDate, pattern).shiftFor(date)` resuelve fechas pasadas y futuras. La cadencia inicial reutilizable está disponible en `DefaultShiftPatterns.SIX_BY_SIX`.

La cadencia elegida, su secuencia completa y la fecha de referencia se guardan localmente mediante Preferences DataStore.

Distribución futura por Codex Cloud + GitHub Actions, preservando la identidad de
v1.0.0: [infraestructura RS-05 y firma histórica](docs/RS05-DISTRIBUCION.md).
