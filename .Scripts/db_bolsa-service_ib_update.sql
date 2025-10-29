-- ===========================================
--  ANDINA TRADING - BOLSA SERVICE IB UPDATE
--  Fecha: 2025-01-27
--  Actualización para integración con Interactive Brokers
-- ===========================================

USE db_bolsa;

-- Agregar columnas para integración con Interactive Brokers
ALTER TABLE orden 
ADD COLUMN simbolo VARCHAR(20) COMMENT 'Símbolo del instrumento (ej: AAPL, MSFT)',
ADD COLUMN accion VARCHAR(10) COMMENT 'COMPRA o VENTA',
ADD COLUMN estado VARCHAR(20) DEFAULT 'PENDIENTE' COMMENT 'PENDIENTE, ENVIADA_IB, EJECUTADA, CANCELADA, ERROR_IB',
ADD COLUMN ib_order_id INT COMMENT 'ID de la orden en Interactive Brokers',
ADD COLUMN fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Crear índices para mejorar el rendimiento
CREATE INDEX idx_orden_estado ON orden(estado);
CREATE INDEX idx_orden_ib_order_id ON orden(ib_order_id);
CREATE INDEX idx_orden_simbolo ON orden(simbolo);

-- Actualizar órdenes existentes con valores por defecto
UPDATE orden 
SET estado = 'PENDIENTE',
    fecha_actualizacion = CURRENT_TIMESTAMP
WHERE estado IS NULL;

-- Crear tabla para logs de operaciones IB
CREATE TABLE IF NOT EXISTS ib_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    orden_id BIGINT NOT NULL,
    operation_type VARCHAR(50) NOT NULL COMMENT 'CONNECT, DISCONNECT, PLACE_ORDER, CANCEL_ORDER, MARKET_DATA',
    status VARCHAR(20) NOT NULL COMMENT 'SUCCESS, ERROR, PENDING',
    ib_order_id INT,
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (orden_id) REFERENCES orden(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Crear tabla para datos de mercado en tiempo real
CREATE TABLE IF NOT EXISTS market_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    simbolo VARCHAR(20) NOT NULL,
    bid_price DECIMAL(10,2),
    ask_price DECIMAL(10,2),
    last_price DECIMAL(10,2),
    volume BIGINT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_market_data_simbolo (simbolo),
    INDEX idx_market_data_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insertar datos de ejemplo para testing
INSERT INTO orden (tipo, cantidad, precio, fecha_creacion, empresa_id, usuario_id, simbolo, accion, estado) VALUES
('compra', 100, 150.50, NOW(), 1, 1, 'AAPL', 'COMPRA', 'PENDIENTE'),
('venta', 50, 300.25, NOW(), 2, 2, 'MSFT', 'VENTA', 'PENDIENTE'),
('compra', 200, 25.75, NOW(), 3, 1, 'GOOGL', 'COMPRA', 'PENDIENTE');

COMMIT;
