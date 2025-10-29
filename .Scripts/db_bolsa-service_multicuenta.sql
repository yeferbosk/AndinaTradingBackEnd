-- ===========================================
--  ANDINA TRADING - BOLSA SERVICE - SISTEMA MULTICUENTA
--  Fecha: 2025-10-22
--  Descripción: Tablas para gestión de portafolios y posiciones por usuario
-- ===========================================

USE db_bolsa;

-- ===========================================
--  TABLA: portafolio_usuario
--  Descripción: Almacena el portafolio de paper trading de cada usuario
-- ===========================================
CREATE TABLE IF NOT EXISTS portafolio_usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE,
    saldo_disponible DECIMAL(15,2) NOT NULL DEFAULT 100000.00,
    saldo_invertido DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    saldo_total DECIMAL(15,2) NOT NULL DEFAULT 100000.00,
    ganancia_perdida_total DECIMAL(15,2) DEFAULT 0.00,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_usuario_id (usuario_id),
    INDEX idx_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
--  TABLA: posicion_usuario
--  Descripción: Almacena las posiciones abiertas de cada usuario
-- ===========================================
CREATE TABLE IF NOT EXISTS posicion_usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    simbolo VARCHAR(50) NOT NULL,
    nombre_empresa VARCHAR(255),
    cantidad INT NOT NULL DEFAULT 0,
    precio_promedio DECIMAL(15,4) NOT NULL DEFAULT 0.0000,
    valor_actual DECIMAL(15,2) DEFAULT 0.00,
    ganancia_perdida DECIMAL(15,2) DEFAULT 0.00,
    ganancia_perdida_porcentaje DECIMAL(10,4) DEFAULT 0.0000,
    fecha_apertura TIMESTAMP NULL,
    fecha_actualizacion TIMESTAMP NULL,
    INDEX idx_usuario_id (usuario_id),
    INDEX idx_simbolo (simbolo),
    INDEX idx_usuario_simbolo (usuario_id, simbolo),
    UNIQUE KEY uk_usuario_simbolo (usuario_id, simbolo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
--  COMENTARIOS DE LAS TABLAS
-- ===========================================

-- portafolio_usuario
-- - saldo_disponible: Dinero en efectivo disponible para comprar
-- - saldo_invertido: Dinero actualmente en posiciones abiertas
-- - saldo_total: Total del portafolio (disponible + invertido)
-- - ganancia_perdida_total: P&L acumulado de todas las operaciones

-- posicion_usuario
-- - simbolo: Código de la acción (EC, CIB, AVH)
-- - cantidad: Número de acciones en posesión
-- - precio_promedio: Precio promedio de compra
-- - valor_actual: Valor actual de la posición (cantidad * precio_mercado)
-- - ganancia_perdida: Diferencia entre valor_actual y costo_total
-- - ganancia_perdida_porcentaje: P&L en porcentaje

-- ===========================================
--  DATOS DE PRUEBA (OPCIONAL)
-- ===========================================

-- Crear portafolio para usuario de prueba (ID 1)
INSERT INTO portafolio_usuario (usuario_id, saldo_disponible, saldo_invertido, saldo_total)
VALUES (1, 100000.00, 0.00, 100000.00)
ON DUPLICATE KEY UPDATE 
    saldo_disponible = VALUES(saldo_disponible),
    saldo_total = VALUES(saldo_total);

-- ===========================================
--  CONSULTAS ÚTILES PARA VERIFICACIÓN
-- ===========================================

-- Ver portafolios creados
-- SELECT * FROM portafolio_usuario;

-- Ver posiciones abiertas
-- SELECT * FROM posicion_usuario;

-- Ver órdenes por usuario
-- SELECT * FROM orden WHERE usuario_id = 1;

-- Resumen de un portafolio
-- SELECT 
--     p.usuario_id,
--     p.saldo_disponible,
--     p.saldo_invertido,
--     p.saldo_total,
--     p.ganancia_perdida_total,
--     COUNT(pos.id) as total_posiciones
-- FROM portafolio_usuario p
-- LEFT JOIN posicion_usuario pos ON p.usuario_id = pos.usuario_id
-- WHERE p.usuario_id = 1
-- GROUP BY p.id;



