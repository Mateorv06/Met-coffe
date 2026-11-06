# Ejecución de pruebas

## Requisitos

- JDK 25 activo.
- Maven 3.9 o posterior.
- Docker Desktop / Docker Engine y Docker Compose para MongoDB local.
- Node.js 22 y npm para Newman.

Comprobar el entorno:

```bash
java -version
mvn -version
```

## 1. Pruebas unitarias (sin MongoDB ni Docker)

```bash
mvn clean test
```

Surefire ejecuta las pruebas unitarias y web (`*Test`) y excluye las clases `*IntegrationTest`.

Las pruebas de servicios verifican reglas de negocio con dependencias simuladas cuando corresponde. Las pruebas web de `CuentaController` y seguridad comprueban respuestas HTTP y acceso protegido.

## 2. Pruebas de integración Java (requieren MongoDB de pruebas)

Iniciar MongoDB local como replica set usando el `docker-compose.yml` del proyecto:

```bash
docker compose up -d mongodb-test
docker compose run --rm mongodb-test-init
```

El segundo comando espera a que MongoDB esté saludable, inicializa el replica set `rs0` y termina. Ejecuta después Maven en esa misma terminal (o configura la variable también en la terminal nueva).

Configurar la conexión en la terminal donde se ejecutará Maven.

**GitHub Codespaces / Linux / macOS (bash):**

```bash
export MONGODB_TEST_URI='mongodb://127.0.0.1:27018/metcoffee_test?replicaSet=rs0&directConnection=true'
```

**Windows PowerShell:**

```powershell
$env:MONGODB_TEST_URI = 'mongodb://127.0.0.1:27018/metcoffee_test?replicaSet=rs0&directConnection=true'
```

Ejecutar las pruebas de integración:

```bash
mvn -Pintegration verify
```

El perfil `integration` activa las clases `*IntegrationTest` mediante Maven Failsafe. El proyecto usa Spring Boot 3.5.16 y Java 25. La base usada es `metcoffee_test`; no se debe apuntar esta variable a producción ni a la base habitual de MongoDB Atlas.

### Qué verifica cada clase de integración

- `MongoIntegrationTest`: guarda y lee un documento real en MongoDB; comprueba que una compra se revierta si falla un descuento de inventario; y verifica que un usuario no pueda consultar el pedido de otro usuario (403), mientras que el propietario sí pueda consultarlo (200). En el escenario de rollback simula el fallo del segundo descuento para comprobar que pedido, pago, inventario, movimientos y carrito quedan sin cambios parciales.
- `ProductoControllerIntegrationTest`: usa `MockMvc` para entrar por HTTP al controlador real de productos. Crea, lista, consulta desde catálogo, actualiza y desactiva un producto; comprueba la persistencia en `ProductoRepository`. También verifica 401 al crear sin token y 400 con un DTO inválido. El administrador y el producto de la prueba se generan con identificadores propios y se limpian al terminar.
- `CompraControllerIntegrationTest`: prepara un usuario, producto, dirección y carrito temporales; confirma la compra por el endpoint HTTP con JWT y verifica el pedido creado, el pago, el vaciado del carrito, el movimiento de inventario y la reducción del stock. También comprueba 401 cuando se intenta confirmar una compra sin token. Los registros temporales se eliminan al finalizar.

Las dos clases nuevas usan `@SpringBootTest`, `@AutoConfigureMockMvc`, `MockMvc`, `ObjectMapper`, servicios reales y repositorios Mongo reales. No reemplazan los servicios o repositorios por mocks.

## 3. Ejecutar toda la batería Java

```bash
mvn clean -Pintegration verify
```

El comando `mvn clean -Pintegration verify` ejecuta en un mismo ciclo Maven las pruebas unitarias (Surefire) y las de integración (Failsafe).

## 4. Pruebas de API con Postman / Newman

Primero, con MongoDB de pruebas activo, inicia la API en una terminal:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

Comprueba que esté disponible:

```bash
curl http://localhost:8080/actuator/health
```

En otra terminal:

```bash
cd postman
npm ci
npm run test:api:report
```

Newman ejecuta `MetCoffee.postman_collection.json` con `MetCoffee.postman_environment.json`. La colección contiene solicitudes para cuenta, seguridad, catálogo, productos administrativos, bolsa/carrito, domicilios, compras, pedidos, inventario y dashboard. Los scripts JavaScript validan códigos HTTP y datos de las respuestas, y guardan valores como `clienteToken`, `usuarioId`, `productoId`, `direccionId` y `pedidoId` para las solicitudes siguientes.

Las pruebas que necesitan privilegios administrativos usan `adminToken`. Para ejecutarlas localmente, configura un token administrativo o las variables `adminCorreo` y `adminPassword` de un administrador existente en la base de pruebas. Sin esas credenciales, las solicitudes administrativas protegidas se omiten; las pruebas negativas que verifican el rechazo por falta de autenticación continúan ejecutándose.

Los reportes se guardan en `postman/reports/`.

## 5. CI de GitHub Actions

El workflow `.github/workflows/ci.yml` usa `ubuntu-latest` y JDK 25. Para evitar el error de `docker create` por pasar parámetros de MongoDB como opciones de Docker, ya no define MongoDB en `services`. En su lugar ejecuta explícitamente:

```bash
docker run -d --name metcoffee-mongodb-test -p 27018:27017 mongo:7.0 mongod --replSet rs0 --bind_ip_all
```

Después inicializa `rs0`, ejecuta Maven y Newman, publica los reportes y elimina el contenedor al finalizar. GitHub Actions en `ubuntu-latest` proporciona Docker; este paso no debe ejecutarse desde un runner self-hosted que no tenga Docker disponible.

## 6. Detener MongoDB local

```bash
docker compose down
```

Esto detiene y elimina los contenedores locales. Para borrar también el volumen de la base de pruebas:

```bash
docker compose down -v
```

El segundo comando elimina los datos de `metcoffee_test` guardados en el volumen Docker; úsalo solo cuando quieras reiniciar la base de pruebas desde cero.


## 7. Entorno Java 25

El Codespace usa la imagen `mcr.microsoft.com/devcontainers/java:25-bookworm`, que proporciona JDK 25. La característica `java` deja intacto el JDK de la imagen e instala Maven. Docker-in-Docker se instala con la característica oficial `ghcr.io/devcontainers/features/docker-in-docker:4.1.3`.

Verificar el entorno después de reconstruir el contenedor:

```bash
java -version
mvn -version
docker --version
docker compose version
```

La aplicación y la imagen Docker también usan Java 25. El proyecto se compila con `maven.compiler.release=25`. Spring Boot se actualizó de 3.4.0 a 3.5.16, generación 3.x compatible oficialmente con Java 25. Mockito se fija en 5.24.0 para evitar problemas de instrumentación de Byte Buddy al ejecutar pruebas en Java 25.

Comando recomendado para ejecutar toda la batería Java:

```bash
mvn clean -Pintegration verify
```

