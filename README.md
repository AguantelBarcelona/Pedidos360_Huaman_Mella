# Pedidos360 Huaman Mella — Evaluacion Parcial 2

Este repositorio de trabajo unifica las cuatro aplicaciones del Parcial 1.
En la Etapa 2 se modifico exclusivamente pedidos-service para agregar un publicador SNS con outbox transaccional. El resto de servicios y el frontend conserva el codigo original.

## Contenido

- `frontend/`: React + TypeScript + Amplify (Cognito).
- `pedidos-service/`: Spring Boot, pedidos y coordinacion REST.
- `productos-service/`: Spring Boot, catalogo.
- `inventario-service/`: Spring Boot, existencias/reservas.
- `infraestructura/mensajeria-cloudformation.yml`: SNS + 3 SQS + 3 DLQ + filtros + 3 alarmas.
- `docs/PLAN_EVALUACION_2.md`: seguimiento de pauta, arquitectura y pruebas.

## Estado y limites

**Infraestructura AWS creada y fanout/filtros comprobados por el estudiante en Learner Lab.** El publicador Java ahora esta preparado pero no se ha compilado ni probado en EC2. Faltan consumidores, administrador y pruebas de procesamiento/dlq. Instrucciones: `docs/ETAPA_2_PUBLICADOR_SNS.md`.

## Probar la base anterior (requiere la configuracion real del Parcial 1)

```bash
cd frontend
cp .env.example .env
# Completar .env con IDs publicos de Cognito y URL de API Gateway del laboratorio
npm ci
npm run build
npm run dev
```

Los tres servicios Spring Boot mantienen configuracion propia y variables de entorno para PostgreSQL/Cognito. Por ejemplo:

```bash
cd pedidos-service
./mvnw test
```

No subir `.env` a Git; el `.gitignore` en raiz lo excluye.

## AWS: crear recursos cuando la cuenta Learner Lab este lista

La plantilla permite desplegar la topologia declarativa con AWS CLI, **si Learner Lab habilita CloudFormation y los permisos necesarios**:

```bash
aws sts get-caller-identity
aws cloudformation validate-template --template-body file://infraestructura/mensajeria-cloudformation.yml
aws cloudformation deploy --stack-name pedidos360-ep2-mensajeria \
  --template-file infraestructura/mensajeria-cloudformation.yml \
  --parameter-overrides PrefijoProyecto=pedidos360
aws cloudformation describe-stacks --stack-name pedidos360-ep2-mensajeria \
  --query 'Stacks[0].Outputs'
```

No ejecutar `deploy` hasta confirmar region AWS y permisos. La plantilla no incluye IAM de EC2 para publicar/consumir: se definira con la configuracion real de EC2. No crear claves AWS dentro del repositorio.

La prueba de mensajes requiere publicar el atributo SNS **`eventType` (String)** para que funcionen los filtros; un cuerpo JSON con `eventType` no basta para esta plantilla.

Siguiente etapa: probar el productor/outbox en `pedidos-service` y crear consumidores `@SqsListener` con ACK explicito, idempotencia y manejo de DLQ. La confirmacion del flujo completo requiere realizar pruebas reales en AWS.
