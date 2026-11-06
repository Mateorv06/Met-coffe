# Ejecución de pruebas

## Requisitos

- JDK 17 activo.
- Maven 3.9 o posterior.
- Docker y Docker Compose para integración MongoDB.
- Node.js y npm para Newman.

Comprobar el JDK efectivo:

```bash
java -version
mvn -version
printf '%s\n' "$JAVA_HOME"
```

## Pruebas unitarias

```bash
mvn clean test
```

Surefire ejecuta las pruebas `*Test` y excluye `*IntegrationTest`.

Las rutas de carrito, domicilios, compras y pedidos requieren un JWT cuyo `sub` coincida con el `usuarioId` de la URL. Un usuario autenticado sobre el recurso de otro usuario recibe HTTP 403.

## Pruebas de integración

Levantar MongoDB de pruebas:

```bash
docker compose up -d mongodb-test mongodb-test-init
export MONGODB_TEST_URI='mongodb://127.0.0.1:27018/metcoffee_test?replicaSet=rs0&directConnection=true'
mvn -Pintegration verify
```

La integración usa repositorios Mongo reales y la base `metcoffee_test`. Nunca hereda la URI de Atlas. `MongoIntegrationTest` conserva las pruebas de lectura/escritura real, rollback de compra y autorización entre usuarios. Además, `ProductoControllerIntegrationTest` y `CompraControllerIntegrationTest` prueban peticiones HTTP con `MockMvc`, servicios reales y repositorios Mongo reales: CRUD de productos y confirmación de compra con persistencia de pedido, pago, carrito e inventario.

## Todas las pruebas y empaquetado

```bash
mvn clean test
mvn -Pintegration verify
mvn package -DskipTests
```

## API

Iniciar la aplicación de prueba:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

Después consultar la salud:

```bash
curl http://localhost:8080/actuator/health
```

Ejecutar Newman:

```bash
cd postman
npm ci
npm run test:api:report
```

Los reportes quedan en `postman/reports/`.

La colección completa se ejecuta con Newman. El registro y login del cliente guardan automáticamente el JWT en `clienteToken` y el ID en `usuarioId`. Los casos negativos de autenticación se ejecutan sin token. Las solicitudes administrativas utilizan `adminToken`; si no hay un token configurado, Newman omite esas solicitudes para que CI pueda ejecutarse sin credenciales administrativas reales. Para probar el CRUD administrativo desde Postman, proporciona `adminCorreo` y `adminPassword` de un administrador de la base de pruebas, o define `adminToken` localmente.

## Limpieza

```bash
docker compose down -v
```

La limpieza elimina solo el almacenamiento Docker de pruebas.
