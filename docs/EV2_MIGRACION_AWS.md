# Evidencia de migración AWS - Pedidos360 EV2

## Objetivo
Migrar Pedidos360 al nuevo laboratorio AWS Academy, conservando
el código original, los datos y la funcionalidad principal.

## Infraestructura migrada
- EC2: microservicios Java Spring Boot.
- RDS PostgreSQL: base de datos restaurada desde respaldo.
- Cognito: autenticación y grupos ADMIN / CLIENTE.
- API Gateway: endpoints protegidos mediante JWT.
- VPC Link + ALB interno: comunicación privada con EC2.
- SNS / SQS: mensajería, consumidores y colas DLQ.
- React: frontend conectado a la nueva infraestructura.
- systemd: arranque automático habilitado para cuatro servicios.

## Diferencias con el laboratorio anterior
Se conserva la lógica de negocio y el código base.
Los recursos AWS, identificadores, endpoints y configuraciones
de infraestructura corresponden al nuevo laboratorio.

## Pruebas realizadas
- Inicio de sesión Cognito: correcto.
- Catálogo de productos: correcto.
- Inventario: correcto.
- Administración de pedidos: correcto.
- Creación del pedido N.º 36: correcta.
- Publicación SNS del evento PEDIDO_CREADO: confirmada.
- Auditoría SQS: PROCESADO_ACK.
- Documentos SQS: comprobante COMP-00000036.
- Notificaciones SQS: PROCESADO_ACK.

EventId verificado:
1037d11f-f9e1-439d-aa3c-52b01d779ab2

## Estado actual
El flujo principal de EV2 fue validado funcionalmente.
El arranque con systemd está habilitado, pendiente de comprobar
en un próximo reinicio de EC2.

## Próxima etapa
EV3: implementación de Amazon Kinesis, productores,
consumidores, manejo de fallos y monitoreo.
