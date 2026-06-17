<?php
/**
 * Database Configuration
 * 
 * Define credentials y parámetros de conexión a MariaDB
 * Usa variables de entorno para seguridad
 */

return [
    'host' => getenv('DB_HOST') ?: 'localhost',
    'user' => getenv('DB_USER') ?: 'root',
    'password' => getenv('DB_PASS') ?: '',
    'database' => getenv('DB_NAME') ?: 'assiten',
    'port' => getenv('DB_PORT') ?: 3306,
    'charset' => 'utf8mb4',
    'collation' => 'utf8mb4_unicode_ci',
    
    // Connection pooling
    'max_connections' => 20,
    'timeout' => 30,
    
    // Security
    'ssl' => [
        'enabled' => (bool)getenv('DB_SSL_ENABLED'),
        'verify_ssl' => (bool)getenv('DB_SSL_VERIFY'),
    ]
];
?>
