# Evaluación 3 - Pedidos360

## 1. Objetivo

Implementar una arquitectura de procesamiento de eventos
mediante Amazon Kinesis Data Streams, complementando la
arquitectura SNS y SQS desarrollada en la evaluación anterior.

## 2. Infraestructura

| Stream | Shards | Retención |
|---|---:|---:|
| pedidos360-pedidos-stream | 3 | 24 horas |
| pedidos360-inventario-stream | 3 | 24 horas |
| pedidos360-eventos-fallidos | 1 | 24 horas |

Los recursos se aprovisionan mediante AWS CloudFormation.

Plantillas:

- infraestructura/kinesis/streams-cloudformation.yml
- infraestructura/kinesis/alarmas-cloudformation.yml

Región: us-east-1.

## 3. Productores

### pedidos-service

Publica eventos PEDIDO_CREADO utilizando el identificador
del pedido como partition key.

### inventario-service

Publica eventos relacionados con la creación de inventario,
actualización, reserva y liberación de stock.

Utiliza el identificador del producto como partition key.

Ambos servicios implementan:

- Eventos estructurados mediante DTO.
- Patrón transactional outbox.
- Publicación mediante AWS SDK.
- Validación de la respuesta de Kinesis.
- Registro de errores e intentos de publicación.

## 4. Consumidores KCL

El nuevo microservicio consumidores-kinesis-service
utiliza Java 17 y Amazon Kinesis Client Library 3.5.4.

| Aplicación KCL | Responsabilidad |
|---|---|
| p360-pedidos-analitica | Procesamiento analítico de pedidos |
| p360-pedidos-trazabilidad | Registro de trazabilidad |
| p360-inventario-proyeccion | Proyección del inventario |

Las aplicaciones mantienen checkpoints independientes
en DynamoDB.

Se implementaron dos instancias de consumidores con
balanceo automático de shards mediante leases.

## 5. Tolerancia a fallos

- Reasignación automática de shards entre workers.
- Recuperación desde checkpoints almacenados en DynamoDB.
- Procesamiento idempotente mediante PostgreSQL.
- Tres intentos de procesamiento por evento.
- Backoff de 500 y 1000 milisegundos.
- Publicación de eventos inválidos en un stream de fallidos.
- Continuidad del procesamiento después de un evento inválido.

## 6. Monitoreo

Se utiliza Amazon CloudWatch y la métrica MillisBehindLatest
para supervisar el retraso de las aplicaciones KCL.

Se configuraron tres alarmas independientes:

- pedidos360-kcl-analitica-lag
- pedidos360-kcl-trazabilidad-lag
- pedidos360-kcl-inventario-lag

Umbral: 60000 milisegundos.

Durante las pruebas, las tres alarmas alcanzaron el estado OK
y las aplicaciones registraron un retraso de 0 ms.

## 7. Administración REST

El microservicio mensajeria-admin-service incorpora
las siguientes operaciones:

| Método | Endpoint |
|---|---|
| GET | /api/kinesis/streams |
| GET | /api/kinesis/streams/{nombre} |
| POST | /api/kinesis/streams |
| PUT | /api/kinesis/streams/{nombre}/shards |
| PUT | /api/kinesis/streams/{nombre}/retention |
| DELETE | /api/kinesis/streams/{nombre} |
| GET | /api/kinesis/aplicaciones |
| GET | /api/kinesis/aplicaciones/{nombre}/checkpoints |
| GET | /api/kinesis/aplicaciones/{nombre}/lag |

Los endpoints requieren autenticación mediante Amazon Cognito
y el rol ADMIN.

Los streams principales están protegidos contra modificaciones
destructivas desde la API.

La eliminación requiere confirmación explícita.

## 8. Resultados de las pruebas

Pruebas realizadas en AWS el 16 de noviembre de 2026.

| Prueba | Resultado |
|---|---|
| Publicación de eventos de pedidos | Exitosa |
| Publicación de eventos de inventario | Exitosa |
| Consumo mediante KCL | Exitoso |
| Balanceo entre dos workers | Exitoso |
| Recuperación ante detención de worker | Exitosa |
| Persistencia de checkpoints | Exitosa |
| Detección de eventos duplicados | Exitosa |
| Reintentos ante eventos inválidos | Exitosa |
| Publicación en stream de fallidos | Exitosa |
| Continuidad del procesamiento | Exitosa |
| Alarmas CloudWatch | OK |
| Acceso administrativo sin token | HTTP 401 |
| Listado y consulta de streams | HTTP 200 |
| Creación de stream temporal | HTTP 202 |
| Escalamiento de 1 a 2 shards | Exitoso |
| Eliminación sin confirmación | HTTP 428 |
| Eliminación con confirmación | Exitosa |
| Consulta de checkpoints | HTTP 200 |
| Consulta de lag | HTTP 200 |
| Aumento de retención | Bloqueado por SCP del laboratorio |

## 9. Restricciones del entorno AWS Academy

La operación IncreaseStreamRetentionPeriod se encuentra
bloqueada por una Service Control Policy del laboratorio.

La API implementa la operación correspondiente, pero AWS
rechazó su ejecución con una denegación explícita.

El stream permaneció activo con una retención de 24 horas.

Esta restricción externa impidió validar exitosamente
el aumento de retención en el entorno del laboratorio.

## 10. Compatibilidad con la evaluación anterior

La arquitectura SNS y SQS de la EV2 se mantiene operativa.

La incorporación de Kinesis amplía las capacidades de
procesamiento de eventos sin reemplazar los componentes
implementados previamente.

## 11. Conclusión

Pedidos360 incorpora procesamiento distribuido de eventos,
consumidores independientes, persistencia de checkpoints,
idempotencia, tolerancia a fallos, monitoreo centralizado
y administración REST protegida mediante Cognito.

## 12. Configuración centralizada

| Variable de entorno | Valor predeterminado |
|---|---|
| AWS_REGION | us-east-1 |
| KINESIS_CONSUMERS_ENABLED | false |
| KINESIS_PEDIDOS_STREAM | pedidos360-pedidos-stream |
| KINESIS_INVENTARIO_STREAM | pedidos360-inventario-stream |
| KINESIS_FALLIDOS_STREAM | pedidos360-eventos-fallidos |
| KINESIS_INITIAL_POSITION | TRIM_HORIZON |
| KINESIS_POLL_MS | 1000 |
| KINESIS_MAX_ATTEMPTS | 3 |
| KINESIS_RETRY_DELAY_MS | 500 |
| KINESIS_TIMEOUT_MS | 10000 |
| KINESIS_MAX_RECORDS | 25 |
| KINESIS_METRICS_LEVEL | SUMMARY |
| KINESIS_WORKER_ID | worker |
| KINESIS_LEASE_ASSIGNMENT_STRATEGY | LEASE_COUNT_BASED |

La conexión con PostgreSQL utiliza las variables
DB_HOST, DB_NAME, DB_USER y DB_PASS.

Las credenciales reales se mantienen fuera del repositorio.

## 13. Aprovisionamiento de infraestructura

Desde un entorno con AWS CLI, permisos autorizados y
acceso a las plantillas del repositorio:

```bash
aws cloudformation validate-template \
  --template-body file://infraestructura/kinesis/streams-cloudformation.yml

aws cloudformation deploy \
  --template-file infraestructura/kinesis/streams-cloudformation.yml \
  --stack-name pedidos360-kinesis-ev3 \
  --region us-east-1

aws cloudformation deploy \
  --template-file infraestructura/kinesis/alarmas-cloudformation.yml \
  --stack-name pedidos360-cloudwatch-ev3 \
  --region us-east-1
```

No eliminar los streams existentes sin evaluar la pérdida
de eventos y la compatibilidad de los checkpoints KCL.

## 14. Compilación y pruebas

Desde la raíz del repositorio:

```bash
(cd pedidos-service && bash ./mvnw -B clean package)
(cd inventario-service && bash ./mvnw -B clean package)
(cd consumidores-kinesis-service && bash ./mvnw -B clean package)
(cd mensajeria-admin-service && bash ./mvnw -B clean package)
```

## 15. Operación de consumidores en EC2

Los workers se ejecutan como servicios systemd independientes.

```bash
sudo systemctl start pedidos360-kcl@1.service
sudo systemctl start pedidos360-kcl@2.service

sudo systemctl status pedidos360-kcl@1.service
sudo systemctl status pedidos360-kcl@2.service
```

Consulta de registros:

```bash
sudo journalctl -u pedidos360-kcl@1.service -n 100
sudo journalctl -u pedidos360-kcl@2.service -n 100
```

Cada worker ejecuta las tres aplicaciones KCL y dispone
de un identificador independiente.

En el laboratorio, el uso simultáneo de ambos workers
debe considerar la memoria disponible de la instancia EC2.

## 16. Seguridad

- Los microservicios utilizan el rol IAM autorizado del laboratorio.
- La API administrativa requiere autenticación Cognito y rol ADMIN.
- El puerto administrativo 8086 se mantiene sin exposición pública.
- El acceso de prueba se realiza mediante un túnel SSH.
- Los tres streams principales están protegidos contra modificaciones.
- La eliminación de streams requiere confirmación explícita.
