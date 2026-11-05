# Cuadrante App 1.0.1

- Gestión de Asuntos Propios con solicitudes de varias fechas, incluso separadas entre meses y años. Cada fecha conserva su estado, bolsa, resolución, observaciones e histórico dentro de la misma solicitud.
- Bolsas anuales configurables desde 8 AP, saldos y prórrogas expresas con fecha límite exclusiva. La elección de bolsa es manual; cambiarla libera y reserva en una operación atómica.
- SOLICITADO reserva una unidad, CONCEDIDO la mantiene y DENEGADO/CANCELADO la liberan. Se pueden cancelar AP concedidos sin plazo mínimo.
- Edición de fechas pendientes con validación completa de turnos, duplicados, solapamientos y saldo. Si alguna fecha falla, no se guarda ningún cambio.
- Resumen obligatorio antes de guardar, con fechas, turnos, bolsas y saldo resultante; botones grandes de confirmar/cancelar y confirmación complementaria por voz mediante Android, sin API generativas. La disponibilidad de voz depende del reconocedor instalado.
- El calendario mantiene siempre el turno original: círculo AP, triángulo vacaciones y cuadrado permiso; azul solicitado, verde concedido y rojo denegado. Los AP denegados siguen visibles y los cancelados permanecen en histórico.
- Actualización compatible con los ajustes e incidencias de 1.0.0. Los AP anteriores se consideran concedidos, sin inventar fechas históricas; permisos y vacaciones se conservan. Se mantiene el paquete Release y se incrementa versionCode a 2.
