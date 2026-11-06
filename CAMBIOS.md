# MET COFFEE: auditoría técnica y guía de ejecución

Este documento describe el estado real del proyecto Java/Spring Boot. Su objetivo es que otra persona
pueda entender la arquitectura, saber dónde vive cada responsabilidad, ejecutar la aplicación, probarla
y distinguir entre una funcionalidad implementada, una prueba unitaria y una funcionalidad todavía
pendiente.

## 1. Resumen del proyecto

MET COFFEE es una API REST para una tienda de café. La aplicación gestiona:

- cuentas, contraseñas y roles;
- catálogo de productos y presentaciones de 125 g, 250 g y 500 g;
- carritos y bolsas personalizadas;
- domicilios;
- compras, pagos simulados y pedidos;
- inventario por gramos y movimientos;
- dashboard administrativo;
- productos, usuarios y pedidos desde endpoints administrativos.

La configuración vigente usa Java 25, Spring Boot 3.5.16, Spring Web, Spring Data MongoDB, Bean Validation,
Spring Security Crypto y Actuator. La aplicación fue migrada desde Java 17 / Spring Boot 3.4.0; el detalle
de esa migración está en la sección 18. MongoDB es la persistencia real de la aplicación. Las pruebas
unitarias usan Mockito y no necesitan conectarse a MongoDB.

La estructura es equivalente a la organización esperada de `librarydb` (modelos, repositorios, DTOs,
servicios, controllers, configuración y pruebas). El proyecto original `librarydb` no está incluido en
este workspace, por lo que aquí se puede auditar la estructura y el comportamiento de MET COFFEE, pero
no afirmar que cada campo coincide exactamente con el proyecto original.

## 2. Estructura general

```text
met-coffee/
├── pom.xml
├── CAMBIOS.md
└── src/
    ├── main/
    │   ├── java/com/metcoffee/
    │   │   ├── MetCoffeeApplication.java
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   └── resources/application.properties
    └── test/java/com/metcoffee/service/
```

El flujo normal de una petición es:

```text
Cliente HTTP
  -> Controller
  -> DTO y @Valid, cuando está configurado
  -> Service con las reglas de negocio
  -> Repository de Spring Data
  -> MongoDB
  -> DTO/respuesta HTTP
```

Los errores de validación y reglas de negocio pasan por `GlobalExceptionHandler` y se convierten en
respuestas HTTP con un campo `message`.

## 3. Configuración y arranque

### 3.1 `pom.xml`

El archivo define el proyecto Maven `com.metcoffee:met-coffee:1.0.0` y actualmente compila con Java 25.
La configuración inicial usaba Java 17; la actualización está documentada en la sección 18. Las dependencias son:

- `spring-boot-starter-web`: controllers REST, JSON y servidor embebido.
- `spring-boot-starter-data-mongodb`: conexión, mapeo `@Document` y repositorios MongoDB.
- `spring-boot-starter-validation`: anotaciones como `@NotBlank`, `@Email`, `@Size` y `@Valid`.
- `spring-boot-starter-security`: filtro JWT y autorización HTTP para rutas administrativas.
- `spring-security-crypto`: `PasswordEncoder` y BCrypt para contraseñas.
- `jjwt`: creación y validación de tokens JWT.
- `spring-boot-starter-actuator`: endpoints operativos como `/actuator/health`.
- `spring-boot-starter-test`: JUnit, Mockito y utilidades de prueba.

El plugin `spring-boot-maven-plugin` permite ejecutar `mvn spring-boot:run` y construir el proyecto.

### 3.2 `src/main/resources/application.properties`

Variables de configuración:

- `spring.application.name`: nombre de la aplicación, `met-coffee`.
- `spring.data.mongodb.uri`: URI tomada de `MONGODB_URI`; si no existe usa
  `mongodb://localhost:27017/metcoffee?replicaSet=rs0` para soportar transacciones.
- `spring.servlet.multipart.max-file-size`: tamaño máximo individual de imagen, 5 MB.
- `spring.servlet.multipart.max-request-size`: tamaño máximo de petición multipart, 6 MB.
- `app.jwt.secret`: secreto JWT tomado de `JWT_SECRET`; nunca debe compartirse ni confirmarse en Git.
- `app.jwt.expiration`: duración del token, por defecto 30 minutos.
- `app.upload-dir`: carpeta de imágenes tomada de `APP_UPLOAD_DIR`, por defecto `uploads`.

El puerto HTTP de Spring Boot no se sobrescribe aquí, por lo que el valor por defecto es `8080`.

### 3.3 `MetCoffeeApplication.java`

Es el punto de entrada con `@SpringBootApplication`. `main` inicia Spring Boot y
`@EnableScheduling` permite ejecutar la tarea que avanza pedidos enviados a entregados.

### 3.4 `config/SeguridadConfig.java`

Declara el bean `PasswordEncoder`, que usa `BCryptPasswordEncoder` para almacenar contraseñas de forma
no reversible. También configura una `SecurityFilterChain` stateless: registro, login, catálogo y health
son públicos; `/api/admin/**` exige un token JWT con rol `ADMINISTRADOR`. El secreto se toma de una
variable de entorno y los tokens tienen expiración configurable.

## 4. Modelos y variables persistidas

Los modelos marcados con `@Document` se guardan como colecciones MongoDB. `@Id` es el identificador del
documento. Los modelos sin `@Document` se usan como valores embebidos dentro de otros documentos.

### `model/Usuario.java` -> colección `usuarios`

Campos: `id`, `nombreUsuario`, `correo`, `password`, `telefono` y `rol`. `rol` usa el enum `Rol` y por
defecto es `CLIENTE`. La contraseña debe contener el hash BCrypt, nunca la contraseña original.

### `model/Producto.java` -> colección `productos`

Campos: `id`, `nombre`, `origen`, `perfil`, `imagen`, `precioBase`, `stockGramos`, `novedad` y
`activo`. `precioBase` es el valor de referencia y `stockGramos` representa inventario en gramos.
`activo` permite desactivar sin borrar físicamente el producto.

### `model/Carrito.java` -> colección `carritos`

Campos: `id`, `usuarioId` e `items`. `items` contiene líneas `ItemCarrito`. El carrito se identifica por
el usuario, no por una relación MongoDB automática.

### `model/ItemCarrito.java` -> valor embebido

Representa una línea del carrito: producto, cantidad, gramos por unidad, molienda, precio unitario y,
cuando aplica, una `BolsaPersonalizada`. La cantidad y los gramos se usan para calcular total y stock.

### `model/BolsaPersonalizada.java` -> valor embebido

Contiene los gramos totales y una lista de `CompartimentoBolsa`. No es una colección independiente.

### `model/CompartimentoBolsa.java` -> valor embebido

Cada compartimento contiene `productoId` y `gramos`. Permite combinar cafés distintos en una bolsa.

### `model/Direccion.java` -> colección `direcciones`

Campos: `id`, `usuarioId`, `etiqueta`, `destinatario`, `direccion`, `telefono` y `principal`.
La dirección se guarda en el pedido como información de envío para conservar el dato usado en la compra.

### `model/Pedido.java` -> colección `pedidos`

Campos: `id`, `usuarioId`, `direccionEnvio`, `destinatario`, `telefonoEnvio`, `items`, `total`,
`estado` y `fecha`. `items` contiene `DetallePedido`, por lo que el pedido conserva el precio y la
descripción usados durante la compra.

### `model/DetallePedido.java` -> valor embebido

Representa una línea comprada: producto, nombre, cantidad, gramos, precio unitario, subtotal y bolsa
personalizada si corresponde.

### `model/Pago.java` -> colección `pagos`

Campos: `pedidoId`, `total`, `estado` y `referencia`. `EstadoPago` distingue `APROBADO`, `FALLIDO` y
`REEMBOLSADO`.

### `model/MovimientoInventario.java` -> colección `movimientos_inventario`

Es un record con `id`, `productoId`, `pedidoId`, `tipo`, `gramos`, `fecha` y `nota`. `TipoMovimiento`
indica si el movimiento es entrada, salida, ajuste o restauración.

### Enums auxiliares

- `Presentacion`: convierte 125 g, 250 g y 500 g al factor de precio correspondiente.
- `EstadoPedido`: estados del ciclo de vida del pedido.
- `EstadoPago`: estados del pago simulado.
- `Rol`: permisos conceptuales de cliente y administrador.
- `TipoMovimiento`: clasificación de movimientos de inventario.

## 5. DTOs

Los DTOs separan el contrato HTTP de los documentos MongoDB y evitan devolver contraseñas.

- `RegistroRequest`: nombre de usuario, correo, contraseña y teléfono para registrar.
- `LoginRequest`: correo y contraseña para iniciar sesión.
- `UsuarioDTO`: información pública del usuario; no incluye password.
- `ProductoDTO`: datos de catálogo y administración: nombre, origen, perfil, imagen, precio, stock,
  novedad y activo.
- `ItemProductoRequest`: producto, presentación, cantidad y molienda para agregar al carrito.
- `ActualizarItemRequest`: nueva cantidad de una línea.
- `BolsaDTO`: gramos y compartimentos de una bolsa personalizada.
- `CarritoDTO`: id, usuario, items y total calculado.
- `DireccionDTO`: datos de domicilio y destinatario.
- `ConfirmarCompraRequest`: id del domicilio utilizado en la compra.
- `PedidoDTO`: id, total, estado, fecha, detalle y datos de envío.
- `EstadoPedidoRequest`: estado solicitado por administración.
- `AjusteStockRequest`: nuevo stock en gramos y nota del ajuste.
- `CambioRolRequest`: nuevo rol de un usuario.
- `DashboardDTO`: indicadores agregados para administración.

Los DTOs que ya tienen `@Valid` en controller reciben validación automática. Actualmente todavía falta
añadir `@Valid` a algunas entradas de productos, domicilios, inventario, bolsas y operaciones de carrito.

## 6. Repositories

Todos los repositorios extienden `MongoRepository`, por lo que heredan guardar, buscar, eliminar y
listar. Además declaran consultas derivadas por nombre:

- `UsuarioRepository`: buscar por correo y nombre de usuario.
- `ProductoRepository`: productos activos y productos activos marcados como novedad.
- `CarritoRepository`: carrito por `usuarioId`.
- `DireccionRepository`: domicilios por usuario y domicilio por id/usuario.
- `PedidoRepository`: pedidos por usuario ordenados por fecha, por estado y existencia de pedidos de un
  usuario.
- `PagoRepository`: pago asociado a un pedido.
- `MovimientoInventarioRepository`: persistencia y consultas de movimientos.

## 7. Controllers y endpoints

Los controllers reciben HTTP, llaman a un service y devuelven `ResponseEntity`. No deben contener reglas
de negocio complejas.

- `CuentaController` (`/api/cuenta`): registro, login y fusión de carrito.
- `CatalogoController` (`/api/catalogo`): catálogo público y novedades.
- `BolsaPersonalizadaController` (`/api/bolsa`): valida/configura una bolsa.
- `CarritoController` (`/api/carrito/{usuarioId}`): consultar, agregar, actualizar, eliminar y limpiar.
- `DomicilioController` (`/api/domicilios/{usuarioId}`): listar, crear, actualizar y seleccionar principal.
- `CompraController` (`/api/compras`): confirmar la compra de un usuario.
- `PedidoController` (`/api/pedidos/{usuarioId}`): historial, detalle y cancelación del cliente.
- `ProductoController` (`/api/admin/productos`): CRUD lógico de productos y registro de imagen.
- `PedidoAdministrativoController` (`/api/admin/pedidos`): listar, consultar y cambiar estado.
- `InventarioController` (`/api/admin/inventario`): consultar, ajustar y listar movimientos.
- `DashboardController` (`/api/admin/dashboard`): indicadores administrativos.
- `UsuarioController` (`/api/admin/usuarios`): listar, consultar, cambiar rol y eliminar.
- `GlobalExceptionHandler`: transforma `IllegalArgumentException` en 400, `IllegalStateException` en
  409 y errores de `@Valid` en 400.

Importante: las rutas `/api/admin/**` están organizadas como administrativas, pero actualmente no están
protegidas por Spring Security. El `actorId` que aparece en algunas operaciones se recibe desde la
petición, por lo que no reemplaza una autenticación real.

## 8. Services y lógica de negocio

- `CuentaService`: normaliza correo, valida registro, aplica BCrypt, inicia sesión y bloquea después de
  cinco fallos durante 15 minutos. Los valores son `PASSWORD_MIN=8`, `PASSWORD_MAX=72`,
  `MAX_INTENTOS=5` y `BLOQUEO=15 minutos`.
- `CatalogoService`: consulta productos activos y novedades.
- `ProductoService`: crea/actualiza productos, desactiva productos, valida extensiones jpg, jpeg, png
  y webp con máximo de 5 MB, y guarda los bytes en `app.upload-dir/productos`. MongoDB conserva la ruta
  relativa `/uploads/productos/{id}.{extension}`.
- `BolsaPersonalizadaService`: valida gramos, compartimentos y calcula el precio ponderado.
- `CarritoService`: agrega productos y bolsas, valida presentación, molienda y cantidades, fusiona
  carritos y calcula el total.
- `DomicilioService`: administra direcciones y mantiene una dirección principal por usuario.
- `CompraService`: comprueba carrito, domicilio, precio y stock; procesa el pago simulado; crea pedido,
  descuenta inventario, registra pago y vacía el carrito dentro de una transacción MongoDB.
- `PasarelaPago`: interfaz del proveedor de pago.
- `PasarelaPagoSimulada`: implementación de prueba que aprueba o rechaza según la configuración del test.
- `PedidoService`: historial, detalle y cancelación del cliente. La cancelación restaura stock y marca
  el pago como reembolsado cuando corresponde.
- `PedidoAdministrativoService`: transiciones de estado y avance programado de `ENVIADO` a `ENTREGADO`.
  También cancela pedidos, restaura stock y reembolsa pagos.
- `InventarioService`: ajustes, descuentos, restauraciones y movimientos históricos.
- `DashboardService`: calcula ingresos de pedidos entregados y cantidad de clientes.
- `UsuarioService`: lista, consulta, cambia roles y elimina usuarios respetando las reglas de no
  eliminarse a sí mismo ni eliminar usuarios con pedidos.

## 9. MongoDB: función y funcionamiento

MongoDB reemplaza la persistencia temporal de una implementación mockeada. Spring Data transforma cada
objeto `@Document` en un documento BSON y cada repository en operaciones sobre una colección.

Con la configuración actual se usa:

```text
mongodb://localhost:27017/metcoffee
```

La base y las colecciones pueden aparecer al guardar el primer documento; no es necesario crear manualmente
la base vacía. La aplicación utiliza estas colecciones principales:

| Colección                | Se utiliza para                             |
| ------------------------ | ------------------------------------------- |
| `usuarios`               | cuentas, roles y hashes de contraseña       |
| `productos`              | catálogo, precios, stock y estado activo    |
| `carritos`               | líneas del carrito por usuario              |
| `direcciones`            | domicilios de clientes                      |
| `pedidos`                | compras y estado de entrega                 |
| `pagos`                  | pagos aprobados, fallidos y reembolsados    |
| `movimientos_inventario` | entradas, salidas, ajustes y restauraciones |

Los modelos embebidos no crean colecciones propias: `ItemCarrito`, `BolsaPersonalizada`,
`CompartimentoBolsa` y `DetallePedido` se guardan dentro de sus documentos propietarios.

## 10. Cómo instalar y activar MongoDB en Windows

### Instalación como servicio

1. Instalar MongoDB Community Server.
2. Durante la instalación seleccionar `Install MongoD as a Service`.
3. Abrir PowerShell y comprobar el servicio:

```powershell
Get-Service MongoDB
```

Si está detenido:

```powershell
Start-Service MongoDB
```

Comprobar que el puerto responde:

```powershell
Test-NetConnection localhost -Port 27017
```

El resultado correcto debe contener `TcpTestSucceeded : True`.

### Ejecutar MongoDB con Docker

Como alternativa, si Docker está instalado:

```powershell
docker run -d --name metcoffee-mongo -p 27017:27017 mongo:7
```

No se deben ejecutar simultáneamente dos servidores intentando usar el mismo puerto.

### Configurar el servicio local como replica set

Las transacciones MongoDB necesitan un replica set. La instalación actual como servicio standalone no
permite ejecutar la prueba de integración ni las compras transaccionales. Con PowerShell como administrador:

1. Abrir `C:\Program Files\MongoDB\Server\9.0\bin\mongod.cfg`.
2. Añadir al final:

```yaml
replication:
  replSetName: rs0
```

3. Reiniciar el servicio:

```powershell
Restart-Service MongoDB
```

4. Instalar MongoDB Shell (`mongosh`) si todavía no está disponible y ejecutar una sola vez:

```powershell
mongosh --eval "rs.initiate({_id:'rs0',members:[{_id:0,host:'127.0.0.1:27017'}]})"
```

5. Confirmar el estado:

```powershell
mongosh --eval "rs.status().ok"
```

El resultado debe ser `1`. Después se puede ejecutar la integración con el comando indicado en la sección
de pruebas. Esta configuración solo afecta al MongoDB local, no cambia el código que se subirá a GitHub.

## 11. Ejecutar el proyecto paso a paso

Desde PowerShell:

```powershell
cd C:\Users\giral\Desktop\met-coffeeuu\met-coffee
```

Primero ejecutar las pruebas:

```powershell
mvn clean test
```

Después iniciar la aplicación:

```powershell
mvn spring-boot:run
```

Debe mantenerse abierta esa terminal. La aplicación queda normalmente en `http://localhost:8080`.

Comprobar salud:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health -UseBasicParsing
```

La respuesta esperada contiene `{"status":"UP"}`. Probar el catálogo:

```powershell
Invoke-RestMethod http://localhost:8080/api/catalogo
```

Un catálogo vacío es válido si todavía no se han creado productos.

### Registro y login manual

En otra terminal:

```powershell
$registro = @{
    nombreUsuario = "AnaGarcia"
    correo = "ana.garcia@correo.com"
    password = "Clave1234"
    telefono = "3001234567"
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri http://localhost:8080/api/cuenta/registro `
    -Method Post `
    -ContentType "application/json" `
    -Body $registro
```

Después:

```powershell
$login = @{
    correo = "ana.garcia@correo.com"
    password = "Clave1234"
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri http://localhost:8080/api/cuenta/login `
    -Method Post `
    -ContentType "application/json" `
    -Body $login
```

El registro debe devolver un usuario sin la contraseña y el login debe devolver las credenciales públicas
del usuario. Si se repite el correo, se espera un error de conflicto de negocio.

## 12. Pruebas automatizadas

Las pruebas están en `src/test/java/com/metcoffee/service`. Cada clase prueba un service o una regla
aislada. El patrón habitual es:

- `@ExtendWith(MockitoExtension.class)`: activa Mockito con JUnit 5.
- `@Mock`: crea repositories o dependencias falsas.
- `@InjectMocks`: crea el service e inyecta los mocks.
- `when(...).thenReturn(...)`: define respuestas de dependencias.
- `verify(...)`: comprueba que se guardó, eliminó o nunca se llamó algo.
- `assertThrows(...)`: comprueba errores esperados.

Clases cubiertas:

- `BolsaPersonalizadaServiceTest`: gramos, compartimentos y precio.
- `CarritoServiceTest`: cantidades, molienda, precio, stock y fusión.
- `CatalogoServiceTest`: productos activos y novedades.
- `CompraServiceTest`: compra aprobada, pago rechazado, stock y precios.
- `CuentaServiceTest`: registro, BCrypt, login y bloqueo.
- `DashboardServiceTest`: ingresos y clientes.
- `DomicilioServiceTest`: creación, actualización y principal.
- `InventarioServiceTest`: ajustes, descuentos, restauraciones y movimientos.
- `PasarelaPagoSimuladaTest`: aprobación y rechazo del pago simulado.
- `PedidoAdministrativoServiceTest`: transiciones, cancelación y entrega automática.
- `PedidoServiceTest`: historial, detalle y cancelación del cliente.
- `PresentacionTest`: tamaños y factores de precio.
- `ProductoServiceTest`: validación de productos e imágenes.
- `UsuarioServiceTest`: roles, restricciones y eliminación.

### Cómo saber si las pruebas están bien

El comando correcto es:

```powershell
mvn clean test
```

El resultado satisfactorio debe terminar con `BUILD SUCCESS`, sin `Failures` ni `Errors`. El número de
pruebas ejecutadas puede cambiar cuando se agregan casos; el número por sí solo no garantiza cobertura
completa. Una prueba es útil cuando comprueba un resultado observable y también los casos de error, no
solo que el método se pueda invocar.

Además existen pruebas web para login y autorización administrativa, y una prueba de integración MongoDB
que se activa explícitamente con `-Dintegration=true`. La batería normal no requiere MongoDB.

Verificación realizada el 8 de octubre de 2026: `mvn clean test` compiló 71 archivos de producción y
17 archivos de prueba, ejecutó 131 pruebas, obtuvo 0 fallos y 0 errores; las dos pruebas de integración
se omiten por defecto. Surefire reenvía la propiedad Maven `integration` al proceso de JUnit al habilitarlas.

El intento real con `mvn "-Dintegration=true" "-Dtest=MongoIntegrationTest" test` sí activó ambas pruebas,
pero las dos fallaron al escribir/leer porque `localhost:27017` anunció tipo `STANDALONE` y la URI exige
`replicaSet=rs0`. Se confirmó que MongoDB está ejecutándose, pero no que admita transacciones. No se
modificó la configuración del servicio local.

Para ejecutar la integración real contra MongoDB local como replica set:

```powershell
$env:MONGODB_TEST_URI = "mongodb://localhost:27017/metcoffee_test?replicaSet=rs0"
mvn "-Dintegration=true" "-Dtest=MongoIntegrationTest" test
```

La prueba requiere que el servicio MongoDB anuncie el replica set `rs0`. Una instalación standalone
produce un timeout del driver y no valida transacciones.

### Cómo provocar fallos controlados

No se deben modificar los tests existentes para simular un fallo permanente. Para comprobar el mecanismo:

1. Ejecutar un endpoint con un DTO inválido, por ejemplo una contraseña menor de 8 caracteres. Debe
   responder 400.
2. Intentar login con contraseña incorrecta. Debe responder credenciales inválidas y, después del quinto
   intento, bloqueo temporal.
3. Agregar una cantidad mayor al stock. Debe producir error de stock insuficiente.
4. Configurar `PasarelaPagoSimulada` para rechazar en un test. Debe guardarse el pago fallido y no crear
   el pedido ni descontar inventario.
5. Detener MongoDB antes de iniciar Spring Boot. La aplicación no podrá ejecutar correctamente operaciones
   persistentes y la consola mostrará el error de conexión.

Para volver a estado normal, corregir los datos de la petición, reiniciar el bloqueo temporal o restaurar
MongoDB. Para pruebas manuales se recomienda usar una base separada, por ejemplo `metcoffee_test`,
mediante `MONGODB_URI`, y no borrar datos de desarrollo sin confirmar el nombre de la base.

## 13. Auditoría del estado actual (histórico; ver auditoría final en §15)

### Implementado

- Separación por controllers, DTOs, services, repositories y modelos.
- Persistencia MongoDB configurada.
- Registro y login con contraseña BCrypt.
- Bloqueo en memoria ante intentos fallidos.
- Catálogo, productos activos y novedades.
- Carrito, fusión de carritos y bolsas personalizadas.
- Presentaciones y cálculo de precio por gramos.
- Domicilios y dirección principal.
- Pagos simulados y estados de pago.
- Historial, detalle y cancelación de pedidos.
- Inventario por gramos y movimientos.
- Dashboard basado en pedidos entregados.
- Pruebas unitarias de services y reglas principales.
- Autenticación JWT y autorización por rol para `/api/admin/**`.
- Validación HTTP en los DTOs y controllers principales.
- Pruebas web de login y autorización administrativa.
- Soporte de transacciones MongoDB mediante `MongoTransactionManager`.
- Almacenamiento local real de imágenes en la carpeta configurable de uploads.
- Actuator para comprobar salud de la aplicación.

### Observaciones históricas

1. El servicio Windows instalado en 27017 sigue siendo standalone; la instancia de desarrollo rs0 validada
   funciona en 27018. La URI activa del proyecto ya apunta a la instancia validada.
2. La cobertura de integración quedó ejecutada correctamente después de iniciar rs0.
3. **Imágenes en producción:** el almacenamiento local funciona para desarrollo; para un despliegue cloud
   se debe migrar a almacenamiento persistente como Azure Blob Storage, S3 o GridFS.
4. **Equivalencia con `librarydb`:** la estructura es equivalente por capas, pero el código original no
   está disponible para una comparación exacta.

## 14. Diagnóstico de problemas frecuentes

- `Connection refused` en MongoDB: comprobar `Get-Service MongoDB` y el puerto 27017.
- Error de URI: revisar `MONGODB_URI` y que la contraseña de Atlas esté codificada correctamente.
- `/api/catalogo` devuelve `[]`: la aplicación funciona, pero todavía no existen productos activos.
- Puerto 8080 ocupado: cerrar el proceso anterior o ejecutar con `--server.port=8081`.
- `BUILD FAILURE`: leer el primer error completo de Maven; el resumen final no siempre muestra la causa.
- Tests que fallan por datos compartidos: recordar que las pruebas unitarias usan mocks y no deberían
  depender de datos reales de MongoDB.
- La aplicación inicia pero una operación falla: distinguir entre que Spring Boot arranque y que MongoDB
  acepte correctamente cada operación de lectura/escritura.

## 15. Auditoría final de cierre (8 de octubre de 2026)

### Estado operativo comprobado

- MongoDB 9.0 se validó como replica set `rs0` en `127.0.0.1:27018`, con una instancia local aislada
  para desarrollo y pruebas. El nodo fue inicializado y elegido `PRIMARY`; sus datos locales se eliminaron
  al limpiar el proyecto antes de publicarlo.
- [application.properties](src/main/resources/application.properties) usa esa URI por defecto y permite
  sustituirla con `MONGODB_URI`.
- `JWT_SECRET` se mantiene configurable por entorno; la validación manual se ejecutó con una clave de
  desarrollo definida solo en el proceso.
- `/actuator/health` respondió `{"status":"UP"}` con Spring Boot ejecutándose contra MongoDB rs0.
- Las pruebas de integración Mongo activadas ejecutaron 2 pruebas, con 0 fallos y 0 errores: lectura/escritura
  real y rollback completo de compra.
- La suite normal ejecutó 131 pruebas con 0 fallos y 0 errores; las pruebas Mongo se omiten cuando no se pasa
  `-Dintegration=true`.

### Inventario de archivos y responsabilidades

- `MetCoffeeApplication.java`: punto de entrada Spring Boot.
- `config/MongoConfig.java`: crea el `MongoTransactionManager` para transacciones.
- `config/SeguridadConfig.java`: cadena stateless JWT, reglas públicas y rol `ADMIN`.
- `security/JwtService.java` y `security/JwtAuthenticationFilter.java`: emisión, lectura y validación de
  tokens JWT.
- `controller/`: endpoints HTTP para cuenta, catálogo, carrito, compras, domicilios, inventario, productos,
  usuarios, pedidos, bolsas, dashboard y manejo común de errores.
- `dto/`: contratos de entrada y salida; las restricciones Bean Validation producen respuestas `400`.
- `model/`: documentos MongoDB, estados, roles, pedidos, pagos, inventario y objetos de carrito.
- `repository/`: siete repositorios Spring Data para usuarios, productos, carritos, direcciones, pagos,
  pedidos y movimientos de inventario.
- `service/`: reglas de negocio; `CompraService` coordina pago, inventario, pedido y vaciado del carrito
  dentro de una operación transaccional.
- `resources/application.properties`: URI Mongo, tamaño de multipart, rutas de uploads, secreto y duración
  JWT, además del endpoint estático de imágenes.
- `src/test/`: 17 clases con pruebas de servicios, controllers, seguridad e integración Mongo.

### Dependencias y variables de configuración

- Spring Boot Web y Actuator exponen la API REST y salud operativa.
- Spring Data MongoDB persiste documentos y administra repositorios/transacciones.
- Spring Security y `spring-security-crypto` protegen endpoints y almacenan contraseñas con BCrypt.
- JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) implementa tokens firmados.
- Spring Boot Validation aplica `@Valid` y restricciones de DTO.
- `MONGODB_URI`: URI de producción o alternativa; por defecto usa `mongodb://127.0.0.1:27018/metcoffee?replicaSet=rs0`.
- `MONGODB_TEST_URI`: URI exclusiva de las pruebas Mongo.
- `JWT_SECRET`: secreto obligatorio para entornos compartidos; el valor incluido es solo de desarrollo.
- `JWT_EXPIRATION`: duración del token, por defecto `PT30M`.
- `APP_UPLOAD_DIR`: carpeta de imágenes, por defecto `uploads`.

### Resultado y pendientes reales

La fase técnica queda validada para el entorno local preparado. El servicio Windows original en el puerto
27017 sigue siendo standalone porque requiere permisos administrativos para cambiar `Program Files`; las
pruebas se ejecutaron con una instancia temporal rs0 en 27018, que se eliminó al limpiar el proyecto. Para
convertir el servicio instalado en la instancia permanente,
hay que editar su `mongod.cfg` como administrador, añadir `replication.replSetName: rs0`, reiniciarlo y ejecutar
`rs.initiate()` con `127.0.0.1:27017`.

El almacenamiento de imágenes sigue siendo local (`uploads`); un despliegue cloud debe usar almacenamiento
persistente externo.

## 16. Mantenimiento posterior al cierre

1. Configurar MongoDB de pruebas como replica set `rs0` y ejecutar `mvn -Pintegration verify`. La suite incluye pruebas del CRUD HTTP de productos y del flujo HTTP de compra, además de los escenarios Mongo que ya existían.
2. Migrar `uploads` a almacenamiento persistente si se despliega en cloud.
3. Actualizar esta auditoría cada vez que cambie una regla o una variable de configuración.


## 17. Complemento de pruebas solicitado en noviembre de 2026

Se agregó cobertura de integración desde el controlador hasta MongoDB, sin reemplazar las pruebas
unitarias ni cambiar los modelos, servicios o datos funcionales del proyecto:

- `ProductoControllerIntegrationTest`: crea, consulta, actualiza y desactiva un producto con `MockMvc`;
  verifica la persistencia real y los casos de falta de autenticación y DTO inválido.
- `CompraControllerIntegrationTest`: confirma una compra por HTTP con JWT y verifica pedido, pago,
  vaciado del carrito y descuento de inventario en MongoDB; incluye el caso sin autenticación.
- `postman/MetCoffee.postman_collection.json`: añade aserciones JavaScript para los flujos de cuenta,
  productos, catálogo, carrito, domicilios, compra y pedidos, y casos negativos de credenciales,
  autorización, producto inválido y domicilio inexistente.
- `.github/workflows/ci.yml`: en la configuración original de noviembre mantenía JDK 17 (actualizado a Java 25 en la sección 18), ejecutaba las pruebas unitarias, ejecutaba el perfil
  `integration` contra MongoDB rs0 y corre la colección completa de Newman. Las solicitudes que requieren
  administrador se omiten cuando no se proporciona un JWT administrativo; las pruebas negativas de
  autorización sí se ejecutan sin ese token.

Las operaciones automatizadas usan la base `metcoffee_test`; no se deben ejecutar estas pruebas contra
la base de producción o MongoDB Atlas.

### Corrección del arranque de MongoDB en GitHub Actions

Se corrigió `.github/workflows/ci.yml`: `--replSet rs0` y `--bind_ip_all` son argumentos de `mongod`, no opciones aceptadas por `docker create`. El workflow ahora crea el contenedor con `docker run ... mongo:7.0 mongod --replSet rs0 --bind_ip_all`, espera a que MongoDB responda, inicializa el replica set `rs0`, ejecuta Maven y Newman, publica los reportes y elimina el contenedor al terminar. No se cambiaron modelos, servicios, controladores ni datos funcionales.


## 18. Migración del proyecto a Java 25 (noviembre de 2026)

Se migró la configuración técnica del proyecto a Java 25 para trabajar con el JDK disponible en GitHub Codespaces. Se conservaron los modelos, controladores, servicios, repositorios, DTO, datos y reglas funcionales.

Archivos actualizados:

- `pom.xml`: Java 25 (`java.version` y `maven.compiler.release`), Spring Boot 3.5.16, Maven Compiler Plugin 3.16.0, Surefire/Failsafe 3.6.1 y Mockito 5.24.0. Spring Boot 3.5.16 declara compatibilidad hasta Java 25; Mockito 5.18.0 o superior incorpora soporte para Java 25.
- `.devcontainer/devcontainer.json`: imagen oficial `mcr.microsoft.com/devcontainers/java:25-bookworm`, Maven y Docker-in-Docker 4.1.3. Se eliminó el `JAVA_HOME` manual.
- `.github/workflows/ci.yml`: GitHub Actions ahora configura JDK 25, ejecuta `mvn clean -Pintegration verify`, levanta MongoDB como replica set, ejecuta Newman con Node.js 22 y publica reportes.
- `Dockerfile`: las etapas de compilación y ejecución usan Java 25; la etapa de compilación usa Maven 3.9.16.
- `docker-compose.yml`: MongoDB local usa un inicializador idempotente y espera a que el nodo sea PRIMARY.
- `PRUEBAS.md`: requisitos y comandos actualizados a Java 25.

La migración de Java no exige cambiar el código de negocio: el código fuente existente sigue utilizando construcciones compatibles. La actualización de Spring Boot 3.4.0 a 3.5.16 es necesaria porque Spring Boot 3.4 declara compatibilidad hasta Java 24, mientras que 3.5.16 incluye Java 25. Se mantuvo Spring Boot 3.x para evitar el salto de compatibilidad y los cambios mayores de Spring Boot 4.

**Validación pendiente en Codespaces/GitHub Actions:** el entorno de esta edición no dispone de Maven ni Docker, por lo que no se pudo ejecutar `mvn clean -Pintegration verify` ni construir la imagen Docker. Después de actualizar el repositorio, reconstruir el Codespace y ejecutar las pruebas.
