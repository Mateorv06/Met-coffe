# Pruebas de API

Estas pruebas deben ejecutarse únicamente contra una instancia local de Met Coffee configurada con la base MongoDB de pruebas. No uses esta colección contra MongoDB Atlas.

## Preparación

Desde la raíz del proyecto:

```bash
docker compose up -d mongodb-test mongodb-test-init
export MONGODB_TEST_URI='mongodb://127.0.0.1:27018/metcoffee_test?replicaSet=rs0&directConnection=true'
```

Inicia la aplicación con el perfil de pruebas:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

Importa en Postman:

- `MetCoffee.postman_collection.json`
- `MetCoffee.postman_environment.json`

La colección crea un usuario cliente y guarda su token e identificadores en variables. También incluye casos negativos de login, autorización y compra.

El login del cliente guarda automáticamente el JWT en `clienteToken` y el identificador en `usuarioId`. Las rutas de carrito, domicilios, compras y pedidos usan ese token sin copiarlo manualmente. La carpeta `Administración - productos` prepara el producto antes de probar catálogo y compras; al final, `Administración` ejecuta las pruebas administrativas restantes y desactiva el producto de prueba.

La solicitud `Iniciar sesión administrador (requiere variables locales)` se omite cuando `adminCorreo` y `adminPassword` están vacíos. Para probar el CRUD administrativo, define esas variables con las credenciales de un administrador que ya exista en la base de pruebas. La colección guarda el JWT en `adminToken` y lo añade automáticamente a las solicitudes administrativas. No existe un registro público de administradores. Si no se configura un administrador, esas solicitudes se omiten y el resto de pruebas puede ejecutarse normalmente.

## Newman

```bash
cd postman
npm ci
npm run test:api
npm run test:api:report
```

Los reportes se generan en `postman/reports/`. El flujo de compra necesita un producto creado y disponible; las solicitudes administrativas están incluidas para preparar esos datos cuando exista un `adminToken` válido. Ejecuta la colección completa desde Newman o, en Postman, las carpetas en este orden: Cuenta, Seguridad, Administración - productos, Catálogo, Bolsa y carrito, Domicilios y compras, Pedidos y Administración. La solicitud de subida de imagen queda deshabilitada porque requiere seleccionar un archivo local.

## Limpieza

```bash
docker compose down -v
```

El comando elimina únicamente el volumen `metcoffee-mongodb-test-data` del entorno local de pruebas.
