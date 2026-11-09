// Crea (o actualiza) el administrador que usa la colección de Bruno.
// SOLO contra la base de PRUEBAS (metcoffee_test). Nunca contra Atlas.
// Correo: admin-pruebas@test.local   Contraseña: MetCoffeeAdminTest2026!
const bd = db.getSiblingDB('metcoffee_test');
bd.usuarios.updateOne(
  { correo: 'admin-pruebas@test.local' },
  { $set: {
      nombreUsuario: 'admin-pruebas',
      correo: 'admin-pruebas@test.local',
      password: '$2a$10$8WfmrEr9CXF/xVNdsvwP9.ZVwlIJpsRod7W3g2wTlk9Pz07Tt1yx2',
      telefono: '3000000000',
      rol: 'ADMINISTRADOR'
  } },
  { upsert: true }
);
print(bd.usuarios.findOne({ correo: 'admin-pruebas@test.local' }, { password: 0 }));
