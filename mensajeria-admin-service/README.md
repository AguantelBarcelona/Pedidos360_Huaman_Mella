# mensajeria-admin-service

API REST para administrar la mensajería de Pedidos360 (SNS, SQS, DLQ y CloudWatch). No usa base de datos.

- **Seguridad:** todo `/api/**` exige un access token de Cognito de un usuario del grupo **ADMIN**. Solo `/health` es público.
- **Alcance limitado:** solo opera sobre recursos cuyo nombre empieza con `pedidos360-` (`MENSAJERIA_PREFIJO`). Cualquier otro ARN o cola se rechaza con 403.
- **Operaciones destructivas** (eliminar, purgar, reprocesar): exigen una confirmación escrita con el nombre exacto del recurso.
- **Trazabilidad:** cada operación queda en el log `ADMIN_AUDIT` con usuario, operación y recurso.

El reproceso y la purga de DLQ se basan en `AdministracionMensajeriaController`, de la primera versión del equipo (`mensajeria-service`).

## Estructura

```
mensajeria_admin_service/
  comun/          AdminProperties, AwsConfiguration, NombresRecursos (validación), AuditoriaAdmin, errores
  topicos/        Crear, listar y eliminar tópicos SNS
  colas/          Crear (con su DLQ y RedrivePolicy), listar, consultar estado y eliminar colas
  suscripciones/  Suscribir una cola a un tópico con filtro por eventType
  dlq/            Ver mensajes, reprocesar (StartMessageMoveTask), tareas y purgar
  consumidores/   Pausar y reanudar consumidores-service (reenvía el token ADMIN)
  monitoreo/      Métricas SQS de CloudWatch y estado de las alarmas
  security/       Cognito (igual que el resto de los servicios)
```

## Compilar

```bash
./mvnw clean test
./mvnw package     # target/mensajeria-admin-service-0.0.1-SNAPSHOT.jar
```

## Endpoints

`TOKEN` = access token de un usuario ADMIN. `A="-H Authorization:Bearer\ $TOKEN"`.

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/admin/topicos` | Lista los tópicos `pedidos360-*` |
| POST | `/api/admin/topicos` `{"nombre":"demo"}` | Crea `pedidos360-demo` |
| DELETE | `/api/admin/topicos?arn=...&confirmar=pedidos360-demo` | Elimina el tópico |
| GET | `/api/admin/colas` | Estado de todas las colas: disponibles, en proceso, retención, visibilidad, redrive |
| GET | `/api/admin/colas/{nombre}` | Estado de una cola |
| POST | `/api/admin/colas` `{"nombre":"demo"}` | Crea `pedidos360-demo` + `pedidos360-demo-dlq` (4 días / 120 s / 20 s / DLQ 14 días / maxReceiveCount 3) |
| DELETE | `/api/admin/colas/{nombre}?confirmar={nombre}&incluirDlq=true` | Elimina la cola (y su DLQ) |
| GET | `/api/admin/suscripciones?topicArn=...` | Suscripciones del tópico, con su filtro |
| POST | `/api/admin/suscripciones` `{"topicArn":"...","cola":"demo","eventTypes":["PEDIDO_CREADO"]}` | Agrega el permiso a la política de la cola y suscribe con RawMessageDelivery y filtro |
| DELETE | `/api/admin/suscripciones?arn=...` | Elimina la suscripción |
| GET | `/api/admin/dlq` | Estado de las 3 DLQ |
| GET | `/api/admin/dlq/{dominio}/mensajes?max=5` | Ver mensajes sin sacarlos (incluye `motivoFallo`) |
| POST | `/api/admin/dlq/{dominio}/reprocesar` `{"confirmacion":"REPROCESAR pedidos360-{dominio}-dlq"}` | Devuelve los mensajes a la cola origen (1 msg/s) |
| GET | `/api/admin/dlq/{dominio}/tareas` | Avance del reproceso |
| POST | `/api/admin/dlq/{dominio}/purgar` `{"confirmacion":"VACIAR pedidos360-{dominio}-dlq"}` | Borra todo el contenido de la DLQ |
| GET | `/api/admin/consumidores` | Estado de cada instancia de consumidores-service |
| POST | `/api/admin/consumidores/{dominio}/pausar[?instancia=URL]` | Pausa en todas las instancias, o en una |
| POST | `/api/admin/consumidores/{dominio}/reanudar[?instancia=URL]` | Reanuda |
| GET | `/api/admin/metricas/{cola}?minutos=60` | Visibles, en proceso, antigüedad, enviados, recibidos y borrados (CloudWatch) |
| GET | `/api/admin/alarmas` | Estado de las alarmas `pedidos360-*` (OK / ALARM / INSUFFICIENT_DATA) |

`{dominio}` = `notificaciones`, `documentos` o `auditoria`.

## Notas

- **Reprocesar no duplica efectos.** `consumidores-service` es idempotente por `eventId`.
- **Purgar es destructivo y no es la forma normal de reprocesar.** SQS permite una purga cada 60 s; si se intenta otra antes, responde 409.
- **Pausar** solo deja de leer la cola: los mensajes se acumulan y no se pierden.
- **Permisos IAM** (LabRole en Learner Lab): `sns:*Topic*`, `sns:Subscribe`, `sns:Unsubscribe`, `sns:List*`, `sqs:*Queue*`, `sqs:SetQueueAttributes`, `sqs:*MessageMoveTask*`, `sqs:PurgeQueue`, `sqs:ReceiveMessage`, `cloudwatch:GetMetricData`, `cloudwatch:DescribeAlarms`.
