# Pedidos360 - Frontend

Frontend web de la plataforma **Pedidos360**, desarrollado con React y TypeScript.

Este proyecto corresponde a la interfaz utilizada para consultar el catálogo de zapatillas y posteriormente gestionar los pedidos de los clientes.

## Tecnologías utilizadas

- React
- TypeScript
- Vite
- AWS Amplify
- Amazon Cognito

## Ejecución local

Para instalar las dependencias:

```bash
npm install
```


## Diagrama de Arquitectura AWS

```mermaid
flowchart TB
    USR(("Usuario<br/>ADMIN / CLIENTE"))
    FE["Frontend React (navegador)<br/>Amplify Authenticator + apiFetch<br/>Rutas protegidas por rol"]
    COG["Amazon Cognito<br/>User Pool<br/>Grupos: ADMIN, CLIENTE"]
    APIGW["API Gateway<br/>Autorizador JWT (Cognito)"]

    subgraph EC2["EC2 · Elastic IP 44.205.90.161 · servicios con systemd"]
        PED["pedidos-service<br/>Spring Boot · Filtro JWT + JPA"]
        PRO["productos-service<br/>Spring Boot · Filtro JWT + JPA"]
        INV["inventario-service<br/>Spring Boot · Filtro JWT + JPA"]
    end

    RDS[("Amazon RDS<br/>PostgreSQL 18<br/>BD pedidos360")]

    USR -- "Usa la aplicación" --> FE
    FE -- "1. Login / renovación de sesión" --> COG
    COG -- "2. ID, access y refresh token" --> FE
    FE -- "3. HTTPS + Authorization: Bearer JWT" --> APIGW
    APIGW -- "4. Petición autorizada" --> PED
    APIGW --> PRO
    APIGW --> INV
    PED -- "JDBC" --> RDS
    PRO -- "JDBC" --> RDS
    INV -- "JDBC" --> RDS

    COG -. "JWKS (valida firma)" .-> APIGW
    COG -. "JWKS (valida firma)" .-> EC2

    classDef actor fill:#FAECE7,stroke:#993C1D,color:#712B13
    classDef aws fill:#E1F5EE,stroke:#0F6E56,color:#085041
    classDef micro fill:#EEEDFE,stroke:#534AB7,color:#3C3489
    classDef front fill:#F1EFE8,stroke:#5F5E5A,color:#444441
    class USR actor
    class COG,APIGW,RDS aws
    class PED,PRO,INV micro
    class FE front
```
