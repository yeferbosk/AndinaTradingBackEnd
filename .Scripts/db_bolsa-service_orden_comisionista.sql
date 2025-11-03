-- ===========================================
--  ANDINA TRADING - ORDEN COMISIONISTA TABLE
--  Fecha: 2025-11-02
--  Tabla para almacenar órdenes enviadas por comisionistas a traders
-- ===========================================

USE db_bolsa;

-- Crear tabla de órdenes del comisionista
CREATE TABLE IF NOT EXISTS orden_comisionista (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_comisionista INT NOT NULL,
    id_trader INT NOT NULL,
    simbolo VARCHAR(50) NOT NULL,
    nombre_empresa VARCHAR(200),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_limite DECIMAL(15, 4) NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE_APROBACION',
    mensaje TEXT,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    orden_ejecutada_id BIGINT NULL,
    INDEX idx_comisionista (id_comisionista),
    INDEX idx_trader (id_trader),
    INDEX idx_trader_estado (id_trader, estado),
    INDEX idx_estado (estado),
    INDEX idx_fecha_creacion (fecha_creacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Comentarios de columnas
ALTER TABLE orden_comisionista 
MODIFY COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE_APROBACION' 
COMMENT 'PENDIENTE_APROBACION, ACEPTADA, RECHAZADA, EJECUTADA, CANCELADA, ERROR_EJECUCION';

