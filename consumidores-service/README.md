# consumidores-service

Consumidores SQS de Pedidos360, organizados por dominio:

| Paquete | Cola | Eventos | Efecto |
|---|---|---|---|
| `notificaciones` | `pedidos360-notificaciones` | PEDIDO_CREADO, PEDIDO_CANCELADO | Guarda la notificación y deja en el log un "email simulado" |
| `documentos` | `pedidos360-documentos` | PEDIDO_CREADO | Genera el comprobante `COMP-000000NN` |
| `auditoria` | `pedidos360-auditoria` | todos | Guarda el JSON completo del evento |
| `comun` | — | — | Infraestructura compartida (ver abajo) |

Las tablas se crean en el esquema `mensajeria` de la misma RDS.

`comun/` se divide por responsabilidad:

```
comun/
  evento/        EventoMensaje (JSON -> objeto), ProcesadorEvento (interfaz de cada dominio)
  errores/       ErrorRecuperableException, ErrorDefinitivoException
  sqs/           @EscuchaSqs, ConsumidorSqs (bucle, ACK, reintento, DLQ), RegistroConsumidores, SqsConfiguration, ConsumidoresProperties
  idempotencia/  EventoProcesado, EventoProcesadoRepository, TransaccionConsumo
  fallos/        RegistroFallo, RegistroFalloRepository, SimuladorFallos
```

## Cómo funciona (requisitos de la pauta)

**Consumidores declarados con anotación (equivalente a `@SqsListener`).** Cada dominio es una clase con solo lógica de negocio, marcada así:

```java
@EscuchaSqs(dominio = "notificaciones")
public class NotificacionProcesador implements ProcesadorEvento {
    public void procesar(EventoMensaje evento) { ... }
}
```

Al arrancar, `RegistroConsumidores` busca los beans con `@EscuchaSqs` y conecta cada uno a la cola `app.consumidores.colas.<dominio>` de `application.properties`. La clase anotada no toca el SDK de AWS; el ACK, los reintentos y la DLQ los maneja `ConsumidorSqs` (`comun/sqs`).

Se usa una anotación propia en vez de `@SqsListener` de Spring Cloud AWS porque el proyecto usa Spring Boot 4.1, y el ACK explícito queda visible en el código (`DeleteMessage`).


**ACK explícito.** Se usa el AWS SDK 2 con `ReceiveMessage` y long polling de 20 s. El mensaje solo se borra (`DeleteMessage`, el ACK) **después** del commit en PostgreSQL.

**Errores recuperables** (timeout, base de datos caída, cualquier error no clasificado):
- No se hace ACK.
- Se ajusta la visibilidad con backoff: 10 s × intento.
- Cuando `ApproximateReceiveCount` supera `maxReceiveCount = 3`, la **RedrivePolicy de SQS** mueve el mensaje a la DLQ.

**Errores definitivos** (JSON inválido, sin `eventId`, faltan datos, tipo no soportado):
- Se envía una copia a la DLQ con los atributos `motivoFallo`, `dominioOrigen`, `messageIdOriginal` y `tipoFallo`.
- Después se hace ACK del original.
- Si el envío a la DLQ falla, no se hace ACK y el mensaje se trata como recuperable.

**Sin duplicados.**
- La tabla `eventos_procesados` usa la clave primaria `dominio:eventId`.
- El efecto de negocio y la marca de procesado se guardan en **la misma transacción**.
- Si dos instancias reciben el mismo evento, la segunda choca con la clave primaria, hace rollback, lo registra como `DUPLICADO_IGNORADO_ACK` y hace ACK.

**Trazabilidad.** Cada fallo queda en el log (`messageId`, `eventId`, `intento`, `causa`, `accion`, `instancia`) y en la tabla `registro_fallos`.

**Varias instancias.**
- SQS oculta cada mensaje mientras una instancia lo procesa.
- Al detener una instancia, los mensajes que no alcanzó a confirmar reaparecen al vencer el VisibilityTimeout y los toma la otra.
- **Pausar** solo deja de pedir mensajes: no purga nada.

## Compilar y probar

```bash
cd consumidores-service
./mvnw test        # pruebas unitarias con mocks, sin AWS ni BD
./mvnw package     # genera target/consumidores-service-0.0.1-SNAPSHOT.jar
```

## Ejecutar en EC2

Permisos: el perfil de instancia (`LabInstanceProfile` en Learner Lab) debe permitir `sqs:ReceiveMessage`, `sqs:DeleteMessage`, `sqs:ChangeMessageVisibility` y `sqs:SendMessage` (para la DLQ).

```bash
set -a; source .env; set +a      # ver .env.example
java -jar consumidores-service-0.0.1-SNAPSHOT.jar
```

Antes de arrancar los consumidores, revisa en la consola de SQS que las colas no tengan mensajes antiguos de pruebas manuales. Si un mensaje no trae `eventId`, va directo a la DLQ como error definitivo; eso es correcto, pero confunde en la demo.

## API (todo requiere access token de un usuario del grupo ADMIN, salvo `/health`)

| Método | Ruta | Uso |
|---|---|---|
| GET | `/health` | Estado e instancia (público) |
| GET | `/api/consumidores` | Estado de cada consumidor: pausado, contadores, último error |
| POST | `/api/consumidores/{dominio}/pausar` | Deja de leer la cola |
| POST | `/api/consumidores/{dominio}/reanudar` | Vuelve a leer |
| POST | `/api/consumidores/{dominio}/simular-fallo?tipo=RECUPERABLE\|DEFINITIVO\|NINGUNO` | Solo para la demo |
| GET | `/api/consumidores/fallos?dominio=` | Tabla `registro_fallos` |
| GET | `/api/consumidores/procesados?dominio=` | Tabla `eventos_procesados` |
| GET | `/api/notificaciones` | Notificaciones generadas |
| GET | `/api/documentos`, `/api/documentos/pedido/{id}` | Comprobantes |
| GET | `/api/auditoria?eventType=` | Registros de auditoría |

`{dominio}` = `notificaciones`, `documentos` o `auditoria`.

## Guion de demo

**1. Fanout.** Crea un pedido en React. En el log aparecen tres `resultado=PROCESADO_ACK` (uno por dominio) con el mismo `eventId`.

**2. Error recuperable, reintentos y DLQ.**
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8084/api/consumidores/notificaciones/simular-fallo?tipo=RECUPERABLE"
```
- Crea un pedido.
- El log muestra `intento=1/3`, `2/3` y `3/3` con `ERROR_RECUPERABLE`. Los reintentos llegan aproximadamente a los 10 y 20 s.
- Luego el mensaje aparece en `pedidos360-notificaciones-dlq` y salta la alarma de CloudWatch.
- Para terminar, vuelve a `tipo=NINGUNO`.

**3. Error definitivo.** Con `tipo=DEFINITIVO`, el mensaje va directo a la DLQ con el atributo `motivoFallo`, sin reintentos.

**4. Dos instancias.**
```bash
# terminal 1
SERVER_PORT=8084 INSTANCIA_ID=consumidor-A java -jar target/*.jar
# terminal 2
SERVER_PORT=8085 INSTANCIA_ID=consumidor-B java -jar target/*.jar
```
- Crea varios pedidos y observa que los mensajes se reparten entre A y B, porque el log incluye `instancia=`.
- Detén A con Ctrl+C. B sigue procesando y no se pierde nada.
- Si quieres que la segunda instancia solo lea una cola, usa `CONSUMIDORES_ACTIVOS=notificaciones`.

**5. Pausa.**
- Ejecuta `POST /api/consumidores/documentos/pausar` y crea un pedido: `ApproximateNumberOfMessagesVisible` de la cola de documentos sube en la consola.
- Luego ejecuta `reanudar` y los mensajes se consumen.
