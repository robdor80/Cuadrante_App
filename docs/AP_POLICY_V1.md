# AP_POLICY_V1 · Cuadrante 1.0.1

Cada solicitud tiene ID y fecha de creación; cada fecha conserva ID, fecha, estado,
bolsa de origen, resolución, observaciones/motivo opcionales e instantáneas de su
evolución. Las fechas pueden estar separadas entre meses y años. Editar pendientes
conserva la solicitud; quitar una fecha la cancela, manteniendo su identidad e
histórico. Moverla mantiene el ID de esa fecha. Las fechas resueltas permanecen
intactas cuando se editan otras pendientes de la misma solicitud.

`ApPolicy` es un reducer puro y determinista. `ShiftCalculator` decide elegibilidad:
MORNING, AFTERNOON o NIGHT, incluso en fines de semana/festivos. OFF y POST_NIGHT
se rechazan. NIGHT cubre la noche que empieza en esa fecha y nunca modifica el
turno del día siguiente. AP consume el turno completo. Las reservas activas no
pueden solaparse con AP, permiso ni vacaciones; las bajas/resoluciones históricas
liberadas no bloquean futuras solicitudes.

Cada bolsa anual tiene cuota mínima 8 y vencimiento exclusivo al 1 de enero
siguiente, salvo prórroga explícita. Hasta 2027-03-01 significa último uso el
2027-02-28. La elección de bolsa siempre es expresa, también cuando hay dos
bolsas elegibles. SOLICITADO reserva 1, CONCEDIDO mantiene 1, DENEGADO y CANCELADO
liberan 1. Cambiar bolsa solicitado/concedido libera/reserva en una sola operación.
Los totales de disponible, prórroga y caducidad derivan del saldo restante; no se
suman entre sí como unidades adicionales. Liberados cuenta fechas en estado final
liberado, no el número de cambios de bolsa o eventos.

## Operaciones y confirmación

`ApRepository.preview` construye y valida el cambio completo, sin escritura.
La propuesta incluye estado previo y posterior, datos persistidos originales,
turnos guardados y hora explícita. El resumen muestra fechas, turnos, estados,
bolsas, observaciones, motivos y saldo resultante. `confirm` exige confirmación,
rechaza propuestas obsoletas, vuelve a ejecutar el dominio y escribe la clave
nueva dentro de `DataStore.edit`. No hay escrituras parciales en ediciones,
resoluciones ni cambios de bolsa. Los nuevos permisos/vacaciones también verifican
solapamientos con reservas AP dentro de la transacción de incidencias.

La interfaz conserva botones grandes verde 👍 y rojo 👎 con descripciones para
lectores de pantalla. La voz usa el reconocedor Android mediante RecognizerIntent,
prefiere reconocimiento sin conexión y no integra servicios generativos ni API
propias. Acepta únicamente «confirmar»/«confirmo» o «cancelar»/«cancelo» inequívocos;
las hipótesis contradictorias no confirman. La disponibilidad y ejecución sin
conexión dependen del reconocedor instalado. Si no está disponible, los botones
siguen funcionando. El parser de voz no contiene lógica de negocio.

El calendario muestra el turno original con superposición: círculo AP, triángulo
vacaciones y cuadrado permiso; azul solicitado, verde concedido y rojo denegado.
Denegados siguen visibles; cancelados solo en histórico. Permisos/vacaciones
heredados se dibujan como efectivos/concedidos sin cambiar sus datos. Los textos
accesibles incluyen tipo y estado para no depender únicamente del color.

## Formato y migración compatible

- DataStore `app_settings`: no cambia ninguna clave ni codec de ajustes.
- DataStore `incidents`, clave `incidents_v1`: conserva registros `D|...` y `R|...`.
  Las operaciones AP nunca modifican este payload. Las modificaciones de otras
  incidencias preservan líneas desconocidas y extensiones ajenas.
- Nueva clave `ap_policy_v1` dentro de `incidents`: Base64 de un formato binario
  versionado explícitamente (v1), con bolsas, solicitudes, fechas e histórico.
  Separadores, saltos de línea y Unicode de observaciones no alteran los campos.
  Un formato desconocido/corrupto genera error y bloquea su reescritura; nunca se
  sustituye silenciosamente por un estado vacío.
- Importación perezosa y determinista: los `DateIncident` AP de 1.0.0 se proyectan
  como solicitudes concedidas con el ID original; cada fecha obtiene un ID
  determinista. Solicitud/resolución/motivo permanecen nulos e histórico vacío.
  El primer cambio real registra el estado importado observado y el cambio con
  hora actual, sin atribuir esa hora al suceso original.
- Como 1.0.0 carecía de bolsas, los AP legados se asignan al año natural de cada
  fecha y su bolsa inicial de 8, sin prórroga automática. La cuota puede ajustarse
  expresamente. Si el legado excede la cuota, se conserva el déficit visible y se
  impiden nuevas reservas que lo aumenten. No se elimina ni limita el legado.
- Tras un cambio, la solicitud AP de la clave nueva prevalece por ID sobre el
  registro histórico: cancelar un AP importado nunca lo resucita al reiniciar.

La PR incluye pruebas del codec antiguo literal, conservación de permisos y
vacaciones, reapertura de DataStore en disco, validación multifecha, saldo,
concurrencia, rechazo de resumen obsoleto, marcadores y confirmación/voz. La prueba
de instalación 1.0.0 → 1.0.1 mediante RobStore se realizará después con firma real.
