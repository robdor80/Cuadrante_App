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

## Motor de turnos

`ShiftCalculator(referenceDate, pattern).shiftFor(date)` resuelve fechas pasadas y futuras. La cadencia inicial reutilizable está disponible en `DefaultShiftPatterns.SIX_BY_SIX`.

La cadencia elegida, su secuencia completa y la fecha de referencia se guardan localmente mediante Preferences DataStore.
