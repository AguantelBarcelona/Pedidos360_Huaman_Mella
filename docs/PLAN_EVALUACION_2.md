# Plan verificable · Pedidos360 · Evaluacion Parcial 2

## Estado de esta entrega
- **Etapa 1**: copias del codigo original colocadas en un arbol unificado, plantilla declarativa AWS preparada.
- **NO** se han creado recursos AWS, desplegado EC2, integrado SNS/SQS en Java ni compilado los proyectos.
- No se han agregado productores ni consumidores, ni el servicio administrador.

## Requisitos del profesor

| Requisito | Evidencia prevista | Estado |
|---|---|---|
| Repositorio GitHub unico con frontend y backend | Raiz unificada, commits de ambos integrantes | Carpeta preparada, aun sin subir |
| Login Cognito + API Gateway + RDS | Flujo de Parcial 1 | Codigo original conservado, nube sin comprobar |
| Topico SNS, 3 SQS, 3 DLQ | CloudFormation + consola AWS | Declarado, no desplegado |
| Fanout + filtro por evento | PEDIDO_CREADO a 3; PRODUCTO_ACTUALIZADO a auditoria | Declarado, no probado |
| Configuracion separada de negocio | IaC + application properties futuras | Infra declarada |
| Productores SNS | Evento tras persistencia correcta | Pendiente |
| Consumidores por dominio | @SqsListener y servicios dedicados | Pendiente |
| ACK explicito, reintentos, duplicados, logs | Fallo reproducible + DLQ | Pendiente |
| Servicio REST administrador | Colas, topicos, suscripciones, pausa, DLQ | Pendiente |
| 2 instancias mismo consumidor | Doble proceso, apagar uno y continuar | Pendiente |
| Metricas y alarma CloudWatch | Visible/in-flight/age/DLQ | Alarmas declaradas, no desplegadas |
| Retencion, limpieza | 4 dias origen, 14 dias DLQ, purga manual autorizada | Declarado, explicar y validar |

## Arquitectura objetivo

```text
React + Amplify/Cognito
    | Authorization: Bearer <access_token>
    v
API Gateway + JWT Authorizer
    v
pedidos-service -> productos-service e inventario-service [REST; flujo original]
    | evento DESPUES de persistir (preferible outbox transaccional)
    v
SNS pedidos360-eventos [atributo SNS: eventType]
    |-- SQS pedidos360-notificaciones -> consumidor notificaciones -> DLQ propia
    |-- SQS pedidos360-documentos     -> consumidor documentos     -> DLQ propia
    `-- SQS pedidos360-auditoria      -> consumidor auditoria      -> DLQ propia

mensajeria-admin-service -> administracion SNS/SQS/DLQ y pausa local/remota
CloudWatch -> metrica DLQ + alarmas
```

## Mapa de eventos y enrutamiento

El publicador DEBE enviar `eventType` como **atributo de mensaje de SNS**, tipo `String`, ademas del JSON de cuerpo.
Las suscripciones filtran por el atributo (no por el cuerpo).

| Evento | Notificaciones | Documentos | Auditoria |
|---|:---:|:---:|:---:|
| PEDIDO_CREADO | Si | Si | Si |
| PEDIDO_CANCELADO | Si | No | Si |
| PRODUCTO_ACTUALIZADO | No | No | Si |

## Normas para el siguiente paso de codigo

1. No crear mensajes desde React ni alterar las reglas JWT.
2. No publicar en SNS antes de confirmar una operacion de negocio. Recomendado: outbox en la misma transaccion de guardar pedido, despacho posterior con reintento.
3. Mantener precio oficial y reserva de stock REST sincrona. Evaluar compensacion si falla `pedidoRepository.save` luego de reservar.
4. Consumers manual ACK **solo tras exito**; recuperables lanzan error y dejan vencer visibilidad para redrive; permanentes se manejan con politica explicita (entrega segura a DLQ y ACK posterior).
5. Consumidores idempotentes usando `eventId` persistido; usar atributos SQS de entrega para loguear id, numero de intento y causa; procesar al menos una vez.
6. No exponer operaciones administrativas SNS/SQS con permisos de CLIENTE; exigir autenticacion ADMIN y principios minimos IAM.
7. Pausar un consumidor implica dejar de recibir mensajes, no purgar la cola; la otra instancia debe poder continuar.
8. Nunca poner credenciales AWS en repo ni guardar usuario/clave cloud fuera de variables de entorno.

## Politicas sugeridas

- SQS Standard (entrega al menos una vez, puede duplicar): idempotencia obligatoria.
- Visibilidad: 120 segundos, ajustar si un consumidor tarda mas; usar long polling de 20 segundos.
- Cola origen: retencion 4 dias, para permitir recuperacion de fallas temporales.
- DLQ: 14 dias, para diagnostico y reproceso; se mantiene mayor que la fuente.
- MaxReceiveCount: 3, para forzar y demostrar redrive tras intentos fallidos.
- Purga: solo desde endpoint ADMIN con confirmacion; purga es destructiva y NO es la forma normal de reprocesar.
- CloudWatch: 1 alarma por cada DLQ cuando existen mensajes disponibles. Monitorear tambien NumberOfMessagesReceived, ApproximateNumberOfMessagesNotVisible y ApproximateAgeOfOldestMessage.

## Demo minima a preparar

1. Iniciar sesion en frontend con Cognito, crear pedido y mostrar persistencia.
2. Mostrar SNS y 3 SQS + 3 DLQ + suscripciones en consola AWS.
3. Evento `PEDIDO_CREADO`: los 3 consumidores lo reciben (fanout).
4. Evento `PRODUCTO_ACTUALIZADO`: solo auditoria recibe (filtro).
5. Forzar error recuperable tres veces: logs id/intento/causa y DLQ con mensaje.
6. Reprocesar un mensaje DLQ desde administrador y verificar efecto.
7. Ejecutar 2 instancias del mismo consumidor; cerrar una sin perder mensajes.
8. Mostrar metricas disponibles/en proceso/antiguedad/DLQ, alarma y retencion.
9. Mostrar administrador con crear, borrar, estado, suscripciones, pausa/reanudacion, reprocesar y purgar en entorno de demostracion.

## Entrega GitHub

Un unico repositorio llamado `Pedidos360_Huaman_Mella` (confirmar regla de nombre con docente si corresponde).
Cada integrante debe hacer al menos un commit real, con `git config user.name` y `git config user.email` propios.
Los repositorios de Parcial 1 siguen existiendo intactos; los ZIP no transportan su historial de Git.
Fecha y hora limite indicada en la pauta: miercoles 21 de octubre de 2026 a las 14:29 (Chile).
