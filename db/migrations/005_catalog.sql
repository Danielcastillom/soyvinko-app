INSERT INTO app.roles(code,description) VALUES
 ('customer','Panel de cliente'),('professional','Panel de profesional'),
 ('support','Atención de casos de soporte'),('admin','Administración y moderación'),('superadmin','Gestión de toda la plataforma y roles');
INSERT INTO app.permissions(code,description) VALUES
 ('support.read','Consultar expedientes de soporte con auditoría'),('support.manage','Gestionar casos de soporte'),
 ('content.moderate','Moderar solicitudes, archivos y reseñas sin alterar puntuaciones'),
 ('kyc.review','Verificar identidad y revocar verificación'),('credits.refund','Devolver crédito de un contrato con caso asociado'),
 ('users.manage','Suspender y reactivar usuarios'),('roles.manage','Asignar roles administrativos'),('plans.manage','Gestionar planes comerciales');
INSERT INTO app.role_permissions(role_code,permission_code)
 SELECT 'superadmin',code FROM app.permissions;
INSERT INTO app.role_permissions(role_code,permission_code)
 SELECT 'admin',code FROM app.permissions WHERE code<>'roles.manage';
INSERT INTO app.role_permissions(role_code,permission_code) VALUES('support','support.read'),('support','support.manage');
INSERT INTO app.categories(slug,name) VALUES
 ('plomeria','Plomería'),('electricidad','Electricidad'),('informatica','Reparación y soporte de computadores'),
 ('cerrajeria','Cerrajería'),('cerrajeria-automotriz','Cerrajería automotriz'),('refrigeracion','Refrigeración y aire acondicionado'),
 ('limpieza','Limpieza'),('carpinteria','Carpintería'),('pintura','Pintura'),('electrodomesticos','Reparación de electrodomésticos');
UPDATE app.categories SET parent_id=(SELECT id FROM app.categories WHERE slug='cerrajeria') WHERE slug='cerrajeria-automotriz';
-- No se inventan precios ni planes comerciales activos. Configurarlos antes del lanzamiento.
