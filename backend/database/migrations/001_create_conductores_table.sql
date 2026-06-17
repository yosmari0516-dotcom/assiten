-- Tabla: conductores
-- Almacena información de conductores y su estado de autorización
-- 
-- Columnas:
-- - id: PK autoincremental
-- - telefono_id: UUID único del dispositivo (asignado por la app)
-- - numero_telefono: Número de teléfono real del conductor (opcional)
-- - estado: 'activo', 'bloqueado', 'suspendido'
-- - created_at: Fecha de registro
-- - updated_at: Última actualización
-- - nota: Campo de observaciones para administradores

CREATE TABLE IF NOT EXISTS `conductores` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `telefono_id` VARCHAR(36) NOT NULL UNIQUE COMMENT 'UUID del dispositivo Android',
  `numero_telefono` VARCHAR(20) COMMENT 'Número de teléfono del conductor',
  `estado` ENUM('activo', 'bloqueado', 'suspendido') NOT NULL DEFAULT 'activo' COMMENT 'Estado de autorización',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `nota` TEXT COMMENT 'Observaciones del administrador',
  
  INDEX `idx_telefono_id` (`telefono_id`),
  INDEX `idx_estado` (`estado`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tabla de conductores con estado de autorización';

-- Tabla de auditoría: action_logs
-- Registra todas las acciones intentadas por conductores para debugging

CREATE TABLE IF NOT EXISTS `action_logs` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `telefono_id` VARCHAR(36) NOT NULL,
  `accion` VARCHAR(50) NOT NULL,
  `elemento_id` VARCHAR(255),
  `authorized` BOOLEAN NOT NULL DEFAULT FALSE,
  `estado` VARCHAR(50),
  `ip_address` VARCHAR(45),
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  INDEX `idx_telefono_id` (`telefono_id`),
  INDEX `idx_accion` (`accion`),
  INDEX `idx_created_at` (`created_at`),
  FOREIGN KEY (`telefono_id`) REFERENCES `conductores`(`telefono_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Auditoría de acciones automatizadas';
