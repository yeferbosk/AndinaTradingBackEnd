-- ===========================================
--  ANDINA TRADING - BOLSA SERVICE DATABASE
--  Fecha: 2025-10-16
-- ===========================================

-- Crear base de datos
CREATE DATABASE IF NOT EXISTS db_bolsa
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE db_bolsa;

-- ===========================================
--  TABLA: orden
-- ===========================================
CREATE TABLE IF NOT EXISTS orden (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_usuario BIGINT NOT NULL,                           -- inversionista que emite la orden
  id_comisionista BIGINT NULL,                          -- comisionista asociado (puede ser NULL si el usuario opera directo)
  id_empresa BIGINT NOT NULL,                           -- empresa (referencia al empresa-service)
  simbolo VARCHAR(20) NOT NULL,                         -- ticker de la acción (ej: AAPL, ECOPETROL)
  tipo ENUM('COMPRA', 'VENTA') NOT NULL,                -- tipo de orden
  cantidad INT NOT NULL CHECK (cantidad > 0),
  precio_limite DECIMAL(15,4) NULL,                     -- precio máximo o mínimo aceptado
  precio_ejecucion DECIMAL(15,4) NULL,                  -- precio final ejecutado (cuando se completa la orden)
  estado ENUM('PENDIENTE', 'ENVIADA', 'EJECUTADA', 'RECHAZADA', 'CANCELADA') 
         DEFAULT 'PENDIENTE' NOT NULL,
  id_externo_ibkr VARCHAR(50) NULL,                     -- ID asignado por Interactive Brokers
  fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_usuario (id_usuario),
  INDEX idx_comisionista (id_comisionista),
  INDEX idx_empresa (id_empresa),
  INDEX idx_estado (estado),
  INDEX idx_simbolo (simbolo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
--  DATOS DE EJEMPLO
-- ===========================================
INSERT INTO orden (id_usuario, id_comisionista, id_empresa, simbolo, tipo, cantidad, precio_limite, estado)
VALUES
  (2, 3, 1, 'ECOPETROL', 'COMPRA', 10, 2500.50, 'PENDIENTE');