-- Migración 032: Actualización descriptiva de planes para Marketplace Multicategoría
UPDATE app.plans
SET benefits_summary = '• Mayor visibilidad en búsquedas.\n• Botón de compra exprés directa de productos para clientes.\n• Acceso a estadísticas de ventas.\n• Carga de inventario (hasta 30 productos).'
WHERE code = 'STORE-STANDARD-30';

UPDATE app.plans
SET benefits_summary = '• Mayor visibilidad en búsquedas.\n• Botón de compra exprés directa de productos para clientes.\n• Acceso a estadísticas de ventas.\n• Carga de inventario (hasta 70 productos).'
WHERE code = 'STORE-PLUS-50';
