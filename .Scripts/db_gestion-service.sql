-- ===========================================
--  ANDINA TRADING - GESTION SERVICE DATABASE
--  Fecha: 2025-10-17
-- ===========================================

CREATE DATABASE IF NOT EXISTS db_gestion
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE db_gestion;

-- ===========================================
--  TABLA: usuario
-- ===========================================
CREATE TABLE IF NOT EXISTS usuario (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(150) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  rol ENUM('ADMIN', 'COMISIONISTA', 'INVERSIONISTA') NOT NULL,
  ciudad VARCHAR(100),
  telefono VARCHAR(50),
  activo BOOLEAN DEFAULT TRUE,
  fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_email (email),
  INDEX idx_rol (rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
--  TABLA: usuario_comisionista (N:N)
-- ===========================================
CREATE TABLE IF NOT EXISTS usuario_comisionista (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_inversionista BIGINT NOT NULL,
  id_comisionista BIGINT NOT NULL,
  fecha_vinculo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  activo BOOLEAN DEFAULT TRUE,
  UNIQUE KEY ux_inversionista_comisionista (id_inversionista, id_comisionista),
  CONSTRAINT fk_uc_inversionista FOREIGN KEY (id_inversionista) REFERENCES usuario(id) ON DELETE CASCADE,
  CONSTRAINT fk_uc_comisionista FOREIGN KEY (id_comisionista) REFERENCES usuario(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
--  TABLA: empresa (independiente)
-- ===========================================
CREATE TABLE IF NOT EXISTS empresa (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(150) NOT NULL,
  simbolo VARCHAR(20) NOT NULL UNIQUE,
  pais VARCHAR(100) NOT NULL,
  fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  activa BOOLEAN DEFAULT TRUE,
  INDEX idx_pais (pais),
  INDEX idx_simbolo (simbolo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
--  TABLA: contrato
-- ===========================================
CREATE TABLE IF NOT EXISTS contrato (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_inversionista BIGINT NOT NULL,
  id_comisionista BIGINT NOT NULL,
  fecha_inicio DATE NOT NULL,
  fecha_fin DATE,
  estado ENUM('ACTIVO', 'FINALIZADO', 'CANCELADO', 'PENDIENTE') DEFAULT 'PENDIENTE',
  fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY ux_contrato_inversionista_comisionista (id_inversionista, id_comisionista, fecha_inicio),
  CONSTRAINT fk_contrato_inversionista FOREIGN KEY (id_inversionista) REFERENCES usuario(id) ON DELETE CASCADE,
  CONSTRAINT fk_contrato_comisionista FOREIGN KEY (id_comisionista) REFERENCES usuario(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
--  TABLA: portafolio
-- ===========================================
CREATE TABLE IF NOT EXISTS portafolio (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_usuario BIGINT NOT NULL UNIQUE,
  saldo_disponible DECIMAL(15,2) DEFAULT 100000.00,
  valor_total DECIMAL(15,2) DEFAULT 100000.00,
  ganancia_total DECIMAL(15,2) DEFAULT 0.00,
  fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP 
                     ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_portafolio_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
--  DATOS DE EJEMPLO
-- ===========================================
INSERT INTO usuario (nombre, email, password, rol, ciudad, telefono)
VALUES
  ('Admin Andina', 'admin@andina.local', '$2a$10$adminHashDemo1234567890', 'ADMIN', 'Bogotá', '+57 3000000000'),
  ('Juan Pérez', 'juan@andina.com', '$2a$10$juanHashDemo1234567890', 'INVERSIONISTA', 'Bogotá', '+57 3011111111'),
  ('María López', 'maria@brokerlatam.com', '$2a$10$mariaHashDemo1234567890', 'COMISIONISTA', 'Lima', '+51 912345678'),
  ('Carlos Gómez', 'carlos@brokerlatam.com', '$2a$10$carlosHashDemo1234567890', 'COMISIONISTA', 'Santiago', '+56 912345678');

INSERT INTO usuario_comisionista (id_inversionista, id_comisionista)
VALUES (
  (SELECT id FROM usuario WHERE email = 'juan@andina.com'),
  (SELECT id FROM usuario WHERE email = 'maria@brokerlatam.com')
);

INSERT INTO empresa (nombre, simbolo, pais, activa)
VALUES
  ('Ecopetrol S.A.', 'ECOPETROL', 'Colombia', TRUE),
  ('Apple Inc.', 'AAPL', 'Estados Unidos', TRUE),
  ('Cencosud S.A.', 'CENCOSUD', 'Chile', TRUE);

INSERT INTO contrato (id_inversionista, id_comisionista, fecha_inicio, fecha_fin, estado)
VALUES
  ((SELECT id FROM usuario WHERE email = 'juan@andina.com'),
   (SELECT id FROM usuario WHERE email = 'maria@brokerlatam.com'),
   '2025-01-01', '2025-12-31', 'ACTIVO');

INSERT INTO portafolio (id_usuario, saldo_disponible, valor_total, ganancia_total)
VALUES
  ((SELECT id FROM usuario WHERE email = 'juan@andina.com'), 100000.00, 100000.00, 0.00),
  ((SELECT id FROM usuario WHERE email = 'maria@brokerlatam.com'), 95000.00, 97000.00, 2000.00),
  ((SELECT id FROM usuario WHERE email = 'carlos@brokerlatam.com'), 120000.00, 119500.00, -500.00);
