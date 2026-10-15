# Etapa 2: Publicador SNS en pedidos-service

## Evidencias previas

- CloudFormation: `pedidos360-ep2-mensajeria` = `CREATE_COMPLETE` (captura del estudiante).
- Fanout confirmado: `PEDIDO_CREADO` llego a las tres colas.
- Filtro confirmado: `PRODUCTO_ACTUALIZADO` incremento auditoria de 1 a 2; las otras colas permanecieron en 1.

## Cambios de esta etapa

Se incorporo `EventoOutbox` (entidad PostgreSQL), repositorio JPA, generacion del evento dentro de la transaccion del pedido, `ProcesadorOutbox` y `PublicadorSns` con AWS SDK for Java 2.x. Se agrego la dependencia `software.amazon.awssdk:sns` (BOM 2.40.5) y se adapto `PedidoServiceTest`.

`PedidoService.crear` ahora guarda el pedido y su evento **en la misma transaccion**. Un job revisa pendientes cada 10 segundos y publica en SNS pasando el atributo `eventType=PEDIDO_CREADO` (String) que necesitan los filtros.

**Todavia no son consumidores**: este paso solo convierte `pedidos-service` en productor de SNS. El registro de evento no significa que el mensaje se haya publicado: `PENDIENTE` y `PUBLICADO` se consultan en la tabla `pedidos.eventos_outbox`.

### 1. Preparar entorno

En el mismo entorno donde corre `pedidos-service` (por ejemplo, EC2), proporciona las variables que ya usabas para RDS y Cognito y, ademas:

```bash
export AWS_REGION=us-east-1
export SNS_TOPIC_ARN='arn:aws:sns:us-east-1:TU_CUENTA:pedidos360-eventos'
export MESSAGING_ENABLED=true
```

**No pegues claves AWS en el repositorio.** Usa un rol IAM de EC2 con permiso `sns:Publish` solo para ese topic. Comprueba permisos segun AWS Academy/Learner Lab. Si no hay rol/permisos, deja `MESSAGING_ENABLED=false` mientras configuramos acceso.

### 2. Compilar y probar

```bash
cd pedidos-service
./mvnw test
./mvnw spring-boot:run
```

En Windows con Git Bash utiliza `./mvnw` (si falla por permisos, `bash mvnw`). Java 17+ y Maven Wrapper descargaran las dependencias la primera vez.

### 3. Generar evento real

Crea un pedido con React autenticado usando el flujo del Parcial 1. Verifica que la tabla `pedidos.eventos_outbox` tenga una fila de tipo `PEDIDO_CREADO` en estado `PUBLICADO` (puede tardar unos 10 segundos) y que las tres colas incrementen sus mensajes disponibles, si no hay consumidores activos.

### 4. Diagnostico

- Si no aparece el evento: revisa `MESSAGING_ENABLED=true`, el endpoint de compra, que la base RDS sea la correcta y que el pedido se haya creado.
- Si queda `PENDIENTE`: revisa credenciales, conectividad de red y permisos `sns:Publish`. Los logs indican `eventId`, numero de intento y causa.
- Si pasa a `ERROR`: se agotaron 10 intentos; por ahora la fila se conserva para revision manual. Reproceso/administracion se implementa en otra etapa.
- Si en SNS se publica pero se pierde la confirmacion transaccional, podria publicarse dos veces. Los futuros consumidores implementaran **idempotencia por `eventId`**.

### Advertencias tecnicas

- No hemos compilado ni ejecutado en AWS este paquete todavia: hay que probarlo en tu Windows/EC2.
- El patron outbox hace atomicos el pedido y el evento en PostgreSQL, **no** la llamada previa a inventario por REST. Si hay un fallo al guardar en RDS despues de reservar stock, podria quedar una reserva huerfana; para resolverlo se necesita una compensacion o saga posterior.
- La version inicial usa un bloqueo pesimista mientras publica en red: adecuado para una demo pequena pero se deberia sustituir por reclamacion de lotes/leases en un entorno con mas carga.
- Si no quieres tener el productor activo aun, deja `MESSAGING_ENABLED=false`; conserva el comportamiento del Parcial 1.
- No elimines tus capturas ni publiques tokens JWT, contrasenas o secretos de AWS.

### Archivos principales

- `pedidos-service/pom.xml`
- `pedidos-service/src/main/resources/application.properties`
- `pedidos-service/src/main/java/cl/pedidos360/pedidos_service/service/PedidoService.java`
- `pedidos-service/src/main/java/cl/pedidos360/pedidos_service/messaging/{EventoOutbox,EventoOutboxRepository,EventoOutboxService,SnsConfiguration,PublicadorSns,ProcesadorOutbox}.java`
- `pedidos-service/src/test/java/cl/pedidos360/pedidos_service/service/PedidoServiceTest.java`
