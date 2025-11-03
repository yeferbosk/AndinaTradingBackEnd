-- ===========================================
--  ANDINA TRADING - USUARIO SERVICE DATABASE
--  Fecha: 2025-10-21
-- ===========================================

-- Crear base de datos
CREATE DATABASE IF NOT EXISTS microsUsuario
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE microsUsuario;

-- ===========================================
--  TABLA: Usuario
-- ===========================================
CREATE TABLE IF NOT EXISTS Usuario (
  id_usuario INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  apellido VARCHAR(50),
  email VARCHAR(100) UNIQUE NOT NULL,
  telefono VARCHAR(20),
  password VARCHAR(100) NOT NULL,
  estado BOOLEAN,
  rol VARCHAR(50) NOT NULL CHECK (
    rol IN ('Trader', 'Comisionista', 'Administrador', 'AreaLegal', 'JuntaDirectiva')
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
--  TABLA: Comisionista
-- ===========================================
CREATE TABLE IF NOT EXISTS Comisionista (
  id_comisionista INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  apellido VARCHAR(100) NOT NULL,
  telefono VARCHAR(20),
  email VARCHAR(100) UNIQUE NOT NULL,
  password VARCHAR(100) NOT NULL,
  estado BOOLEAN
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
--  TABLA RELACIONAL: Usuario_Comisionista (N:N)
-- ===========================================
CREATE TABLE IF NOT EXISTS Usuario_Comisionista (
  id_usuario INT NOT NULL,
  id_comisionista INT NOT NULL,
  PRIMARY KEY (id_usuario, id_comisionista),
  FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
    ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (id_comisionista) REFERENCES Comisionista(id_comisionista)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
--  DATOS INICIALES: Usuario
-- ===========================================
INSERT INTO Usuario (nombre, apellido, email, telefono, password, estado, rol) VALUES
('Laura', 'Martínez', 'laura@unbosque.edu.co', '+57 301 456 7890', 'pass123', TRUE, 'Trader'),
('Carlos', 'Gómez', 'carlos@unbosque.edu.co', '+57 310 222 3344', 'pass456', TRUE, 'Comisionista'),
('Ana', 'Ríos', 'ana@unbosque.edu.co', '+57 311 333 1122', 'pass789', FALSE, 'Administrador'),
('Diego', 'Moreno', 'diego@unbosque.edu.co', '+57 320 444 5566', 'adminpass', TRUE, 'AreaLegal'),
('Valentina', 'Lopez', 'valentina@unbosque.edu.co', '+57 312 555 7788', 'valepass', TRUE, 'JuntaDirectiva');

-- ===========================================
--  DATOS INICIALES: Comisionista
-- ===========================================
INSERT INTO Comisionista (nombre, apellido, telefono, email, password, estado) VALUES
('Mateo', 'Rodríguez', '+57 300 123 4567', 'mateo@unbosque.edu.co', 'm123', TRUE),
('Sofia', 'Vargas', '+57 301 234 5678', 'sofia@unbosque.edu.co', 's456', TRUE),
('Juan', 'Pérez', '+57 302 345 6789', 'juan@unbosque.edu.co', 'j789', FALSE),
('Camila', 'López', '+57 303 456 7890', 'camila@unbosque.edu.co', 'c101', TRUE),
('Andrés', 'García', '+57 304 567 8901', 'andres@unbosque.edu.co', 'a202', TRUE);


-- ===========================================
--  DATOS INICIALES: Usuario_Comisionista
-- ===========================================
INSERT INTO Usuario_Comisionista (id_usuario, id_comisionista) VALUES
(1, 1),
(2, 2),
(1, 3),
(3, 4),
(5, 5);