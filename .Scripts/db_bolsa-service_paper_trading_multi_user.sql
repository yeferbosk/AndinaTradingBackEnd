-- ===========================================
--  ANDINA TRADING - PAPER TRADING MULTI-USUARIO
--  Sistema de cuentas de papel independientes por usuario
--  Fecha: 2025-10-22
-- ===========================================

USE db_bolsa;

-- Tabla de Cuentas de Paper Trading (una por usuario)
CREATE TABLE IF NOT EXISTS cuenta_paper_trading (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    balance_inicial DECIMAL(15, 2) DEFAULT 100000.00,
    balance_actual DECIMAL(15, 2) DEFAULT 100000.00,
    balance_disponible DECIMAL(15, 2) DEFAULT 100000.00,
    balance_invertido DECIMAL(15, 2) DEFAULT 0.00,
    ganancia_perdida_total DECIMAL(15, 2) DEFAULT 0.00,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    activa BOOLEAN DEFAULT TRUE,
    UNIQUE KEY uk_usuario_cuenta (usuario_id),
    INDEX idx_usuario_activa (usuario_id, activa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de Posiciones (acciones que tiene cada usuario)
CREATE TABLE IF NOT EXISTS posicion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    simbolo VARCHAR(50) NOT NULL,
    nombre_empresa VARCHAR(200),
    cantidad INT NOT NULL DEFAULT 0,
    precio_promedio DECIMAL(15, 4) NOT NULL,
    valor_mercado_actual DECIMAL(15, 2),
    ganancia_perdida DECIMAL(15, 2),
    porcentaje_ganancia DECIMAL(10, 2),
    fecha_primera_compra TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cuenta_id) REFERENCES cuenta_paper_trading(id) ON DELETE CASCADE,
    UNIQUE KEY uk_cuenta_simbolo (cuenta_id, simbolo),
    INDEX idx_cuenta_simbolo (cuenta_id, simbolo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de Historial de Transacciones
CREATE TABLE IF NOT EXISTS transaccion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    orden_id BIGINT,
    tipo VARCHAR(20) NOT NULL, -- COMPRA, VENTA, DEPOSITO, RETIRO
    simbolo VARCHAR(50),
    cantidad INT,
    precio_unitario DECIMAL(15, 4),
    monto_total DECIMAL(15, 2) NOT NULL,
    comision DECIMAL(15, 2) DEFAULT 0.00,
    balance_anterior DECIMAL(15, 2),
    balance_posterior DECIMAL(15, 2),
    fecha_transaccion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    descripcion TEXT,
    FOREIGN KEY (cuenta_id) REFERENCES cuenta_paper_trading(id) ON DELETE CASCADE,
    FOREIGN KEY (orden_id) REFERENCES orden(id) ON DELETE SET NULL,
    INDEX idx_cuenta_fecha (cuenta_id, fecha_transaccion),
    INDEX idx_cuenta_tipo (cuenta_id, tipo),
    INDEX idx_simbolo (simbolo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Actualizar tabla de órdenes para incluir cuenta_id
-- Verificar si la columna no existe antes de agregarla
SET @dbname = DATABASE();
SET @tablename = 'orden';
SET @columnname = 'cuenta_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  'ALTER TABLE orden ADD COLUMN cuenta_id BIGINT AFTER usuario_id'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Agregar índice si no existe
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (index_name = 'idx_cuenta_orden')
  ) > 0,
  'SELECT 1',
  'ALTER TABLE orden ADD INDEX idx_cuenta_orden (cuenta_id)'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Agregar foreign key si no existe
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (constraint_name = 'fk_orden_cuenta')
  ) > 0,
  'SELECT 1',
  'ALTER TABLE orden ADD CONSTRAINT fk_orden_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuenta_paper_trading(id) ON DELETE SET NULL'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Datos iniciales de prueba (opcional)
-- Crear cuenta de paper trading para usuario de prueba (id_usuario = 1)
INSERT INTO cuenta_paper_trading (usuario_id, balance_inicial, balance_actual, balance_disponible) 
VALUES (1, 100000.00, 100000.00, 100000.00)
ON DUPLICATE KEY UPDATE fecha_actualizacion = CURRENT_TIMESTAMP;

COMMIT;

