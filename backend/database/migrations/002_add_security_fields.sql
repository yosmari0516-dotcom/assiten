-- Actualizar tabla conductores con campos de seguridad mejorada

ALTER TABLE `conductores` ADD COLUMN `signing_key` VARCHAR(64) COMMENT 'Clave HMAC derivada del Android Device ID';
ALTER TABLE `conductores` ADD COLUMN `auth_token` VARCHAR(36) COMMENT 'Token de autenticación actual';
ALTER TABLE `conductores` ADD COLUMN `token_expiry` BIGINT COMMENT 'Timestamp de expiración del token';
ALTER TABLE `conductores` ADD COLUMN `last_auth_attempt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Última validación de autenticación';
ALTER TABLE `conductores` ADD COLUMN `failed_auth_count` INT DEFAULT 0 COMMENT 'Contador de intentos fallidos';

-- Crear tabla de fraude detectado
CREATE TABLE IF NOT EXISTS `fraud_attempts` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `device_id` VARCHAR(64) NOT NULL,
  `reason` VARCHAR(255),
  `ip_address` VARCHAR(45),
  `user_agent` TEXT,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  INDEX `idx_device_id` (`device_id`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Registro de intentos de fraude detectados';
