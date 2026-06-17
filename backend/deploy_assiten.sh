#!/bin/bash

##############################################################################
# DEPLOY SCRIPT: Assiten AccessibilityService Backend
# 
# Este script prepara e instala todo el backend en cPanel
# Uso: bash deploy_assiten.sh
##############################################################################

set -e  # Detiene si hay error

echo "🚀 Iniciando deployment de Assiten Backend..."

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Variables
BACKEND_DIR="$(pwd)/assiten-backend"
API_DIR="$BACKEND_DIR/api/v1"
CONFIG_DIR="$BACKEND_DIR/config"
DB_DIR="$BACKEND_DIR/database/migrations"
LOG_DIR="/var/log/assiten"

echo -e "${YELLOW}[1/6]${NC} Extrayendo archivos..."
mkdir -p "$BACKEND_DIR"
echo -e "${GREEN}✓${NC} Directorio backend creado"

echo -e "${YELLOW}[2/6]${NC} Creando estructura de directorios..."
mkdir -p "$API_DIR"
mkdir -p "$CONFIG_DIR"
mkdir -p "$DB_DIR"
mkdir -p "$LOG_DIR"
echo -e "${GREEN}✓${NC} Directorios creados"

echo -e "${YELLOW}[3/6]${NC} Configurando permisos..."
chmod 755 "$BACKEND_DIR"
chmod 755 "$API_DIR"
chmod 755 "$CONFIG_DIR"
chmod 755 "$DB_DIR"
chmod 777 "$LOG_DIR" 2>/dev/null || echo -e "${YELLOW}ℹ${NC} Permisos del log pueden requerir root"
echo -e "${GREEN}✓${NC} Permisos configurados"

echo -e "${YELLOW}[4/6]${NC} Creando archivo .htaccess para seguridad..."
cat > "$BACKEND_DIR/.htaccess" << 'EOF'
# Deshabilita listado de directorios
Options -Indexes

# Requiere HTTPS
<IfModule mod_rewrite.c>
    RewriteEngine On
    RewriteCond %{HTTPS} off
    RewriteRule ^(.*)$ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]
end

# Protege archivos sensibles
<FilesMatch "\.(env|sql|log)$">
    Order allow,deny
    Deny from all
</FilesMatch>
EOF
echo -e "${GREEN}✓${NC} .htaccess creado"

echo -e "${YELLOW}[5/6]${NC} Creando archivo de configuración..."
if [ ! -f "$CONFIG_DIR/.env" ]; then
    cp "$CONFIG_DIR/.env.example" "$CONFIG_DIR/.env" 2>/dev/null || cat > "$CONFIG_DIR/.env" << 'EOF'
# Database Configuration
DB_HOST=localhost
DB_USER=assiten_user
DB_PASS=secure_password_here
DB_NAME=assiten
DB_PORT=3306

# API Configuration
API_KEY=your-secure-api-key-here
API_BASE_URL=https://your-domain.com

# Logging
LOG_LEVEL=info
LOG_PATH=/var/log/assiten

# Security
DEVICE_SIGNING_ENABLED=true
TOKEN_EXPIRY_HOURS=24
MAX_FAILED_AUTH_ATTEMPTS=5
EOF
    echo -e "${GREEN}✓${NC} .env creado (ACTUALIZA CON TUS DATOS)"
else
    echo -e "${YELLOW}ℹ${NC} .env ya existe"
fi

echo -e "${YELLOW}[6/6]${NC} Generando instrucciones de post-instalación..."
cat > "$BACKEND_DIR/INSTALL_INSTRUCTIONS.md" << 'EOF'
# Instrucciones de Instalación en cPanel

## 1. Sube archivos a cPanel

### Opción A: FTP/SFTP
```bash
sftp user@your-domain.com
cd public_html
mkdir -p assiten
cd assiten
put -r *
```

### Opción B: SSH (Terminal cPanel)
```bash
cd ~/public_html
unzip assiten-backend.zip
cd assiten
```

## 2. Configura variables de entorno

```bash
# Abre cPanel > File Manager > public_html/assiten/
# Edita config/.env con tus datos:

DB_HOST=localhost
DB_USER=tu_usuario_bd
DB_PASS=tu_contraseña_bd
DB_NAME=tu_base_datos
API_KEY=genera-una-clave-aleatoria
API_BASE_URL=https://tu-dominio.com
```

## 3. Crea la base de datos y usuario

```bash
# En cPanel > MySQL Databases:
# 1. Crea nueva BD: assiten
# 2. Crea nuevo usuario: assiten_user
# 3. Asigna usuario a BD con TODOS los privilegios
```

## 4. Ejecuta migraciones

```bash
# En cPanel > Terminal (SSH):
cd ~/public_html/assiten
mysql -u assiten_user -p assiten < database/migrations/001_create_conductores_table.sql
mysql -u assiten_user -p assiten < database/migrations/002_add_security_fields.sql
```

## 5. Configura logs

```bash
# En cPanel > Terminal:
mkdir -p /home/hlngnuyrki/logs/assiten
chmod 777 /home/hlngnuyrki/logs/assiten
```

## 6. Verifica instalación

```bash
curl -X POST https://tu-dominio.com/assiten/api/v1/authorize \
  -H "X-API-Key: tu-api-key" \
  -H "Content-Type: application/json" \
  -d '{"telefono_id":"test-123:sig","accion":"test"}'

# Debe retornar error 404 (device not found) o similar, NO 500
```

## 7. Configura SSL (HTTPS)

- En cPanel > SSL/TLS Status
- Auto-genera con Let's Encrypt
- Verifica que authorize.php funcione con HTTPS

## Archivos Importantes

```
assiten/
├── api/v1/
│   ├── authorize.php          ⭐ Endpoint principal
│   └── authorize_secure.php   ⭐ Versión con seguridad mejorada
├── config/
│   ├── .env                   ⚙️ Configuración (PRIVADO)
│   ├── .env.example           📋 Template
│   └── database.php           🗄️ Conexión BD
├── database/migrations/
│   ├── 001_create_*.sql       📊 Tabla conductores
│   └── 002_add_security_*.sql 🔐 Campos de seguridad
├── logs/                      📝 Logs (si aplica)
└── README.md                  📖 Documentación
```

## Troubleshooting

### Error 500 en authorize.php
```bash
# Ver logs:
tail -f /home/hlngnuyrki/logs/assiten/authorization.log

# Verificar permisos:
ls -la ~/public_html/assiten/api/v1/
# Debe ser: -rw-r--r-- (644)
```

### Error de BD
```bash
# Verificar conexión:
mysql -h localhost -u assiten_user -p -e "USE assiten; SHOW TABLES;"

# Si no funciona, reejecutar migraciones:
mysql -u assiten_user -p assiten < database/migrations/001_create_conductores_table.sql
```

### HTTPS/SSL issues
```bash
# Forzar HTTPS en .htaccess (ya incluido)
# Si aún tiene problemas:
# - Ve a cPanel > SSL/TLS Status
# - Auto-generate Let's Encrypt certificate
```

## ✅ Verificación Final

```bash
# 1. API responde
curl -I https://tu-dominio.com/assiten/api/v1/authorize
# Debe retornar: HTTP/1.1 405 Method Not Allowed (POST required)

# 2. BD está accesible
mysql -u assiten_user -p assiten -e "SELECT COUNT(*) FROM conductores;"

# 3. Logs se crean
ls -la /home/hlngnuyrki/logs/assiten/
```

## 🔒 Seguridad Post-Instalación

- [ ] Cambia API_KEY en config/.env
- [ ] Cambia contraseña BD en config/.env
- [ ] Elimina .env.example después de copiar
- [ ] Verifica que .env NO sea accesible públicamente
- [ ] Activa HTTPS en cPanel
- [ ] Configura rate limiting en authorize.php

EOF
echo -e "${GREEN}✓${NC} Instrucciones creadas"

echo ""
echo -e "${GREEN}✅ Deployment preparado exitosamente!${NC}"
echo ""
echo "📁 Ubicación: $BACKEND_DIR"
echo "📖 Lee: $BACKEND_DIR/INSTALL_INSTRUCTIONS.md"
echo ""
echo -e "${YELLOW}Próximos pasos:${NC}"
echo "1. Crea un ZIP de la carpeta '$BACKEND_DIR'"
echo "2. Descárgalo a tu PC"
echo "3. Sube a cPanel via FTP o File Manager"
echo "4. Sigue las instrucciones en INSTALL_INSTRUCTIONS.md"
echo ""
