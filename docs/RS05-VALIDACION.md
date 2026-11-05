# Validación de la auditoría RS-05

Estado inicial remoto a7e27c1c34c8cd8ee7425ba766f1fa7a5b7066b7. Durante el trabajo se
incorporó externamente la PR #2: main f1d918f66940d2699c227f9e67afc4eb130e9c75.
Esta PR complementaria parte de ese main y conserva la infraestructura existente.

| Campaña | PASS | FAIL | SKIP |
| --- | ---: | ---: | ---: |
| Android Debug | 70 | 0 | 0 |
| Android Release | 70 | 0 | 0 |
| Python compatibilidad y contrato firmado | 14 | 0 | 0 |
| Total | 154 | 0 | 0 |

Validación local completa:

```sh
./gradlew -I scripts/unsigned-validation.gradle testDebugUnitTest testReleaseUnitTest lint lintRelease assembleDebug assembleRelease --no-daemon
# PYTHONPATH apunta a tools del SHA auditado b15d775, igual que la CI.
python3 -m unittest discover -s scripts -p 'test_rs05_*.py' -v
python3 scripts/check_rs05_compatibility.py --apks
python3 scripts/check_distribution_secrets.py
git diff --check
```

Lint sin errores (10 avisos existentes por variante), APK Release sin firma y Debug
separado sin clave, versiones 1/1.0.0 y 1/1.0.0-DEV, SDK26/36. Se ejecutó actionlint
sobre ambos workflows sin errores de sintaxis (sin linters opcionales shellcheck/pyflakes).
No cambia app/, Gradle de producto, robstore.json, marcador de notas o pin del motor.
Las claves del contrato firmado son fixtures efímeros destruidos al finalizar.

Motor reutilizable fijado a b15d775833c4167c45d79b5b6bb0254db6b0caea. La variable
ANDROID_DISTRIBUTION_TOOLING_SHA debe apuntar al SHA central completo revisado e
incorporado a main con la corrección del alta Java; no a una rama ni al tooling antiguo.
Las CI main de las bases remotas observadas estaban PASS; los checks de esta PR deben
consultarse en GitHub para su HEAD final.

Pendientes: revisión de estas PR complementarias, importación personal de la clave histórica,
respaldo recuperable y configuración protegida por Roberto; futura versión code>1/notas
reales y prueba física de actualización con datos. No se ha ejecutado el alta real,
prepare ni publish, ni creado una Release/tag o clave Release persistente.
