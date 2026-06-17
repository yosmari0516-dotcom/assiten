# Assiten Backend - Deployment Package

Este paquete contiene todo lo necesario para desplegar el backend de Assiten en cPanel.

## 📦 Contenido

```
assiten-backend/
├── api/v1/
│   ├── authorize.php              # Endpoint de autorización básico
│   └── authorize_secure.php       # Endpoint con seguridad mejorada ⭐
│
├── config/
│   ├── .env                       # Configuración (PRIVADO)
│   ├── .env.example               # Template de configuración
│   └── database.php               # Configuración de BD
│
├── database/
│   └── migrations/
│       ├── 001_create_conductores_table.sql    # Tabla principal
│       └── 002_add_security_fields.sql         # Campos de seguridad
│
├── docs/
│   └── SECURITY_IMPROVEMENTS.md   # Documentación de mejoras
│
├── logs/                          # Directorio de logs (se crea automáticamente)
├── .htaccess                      # Configuración Apache (seguridad)
├── README.md                      # Este archivo
├── INSTALL_INSTRUCTIONS.md        # Guía de instalación paso a paso
└── deploy_assiten.sh              # Script de instalación automática
```

## ⚡ Instalación Rápida (3 pasos)

### 1. Descarga y extrae el ZIP
```bash
unzip assiten-backend.zip
cd assiten-backend
```

### 2. Ejecuta el script de instalación
```bash
bash deploy_assiten.sh
```

### 3. Configura variables de entorno
```bash
vi config/.env
# Edita: DB_USER, DB_PASS, DB_NAME, API_KEY
```

## 📋 Checklist de Instalación

- [ ] Archivos subidos a cPanel
- [ ] Variables de .env configuradas
- [ ] Base de datos creada
- [ ] Usuario BD con permisos
- [ ] Migraciones ejecutadas
- [ ] SSL/HTTPS activo
- [ ] Logs configurados
- [ ] Endpoint /api/v1/authorize responde

## 🔧 Configuración Requerida

**config/.env:**
```env
# BD MariaDB en cPanel
DB_HOST=localhost
DB_USER=tu_usuario_bd
DB_PASS=tu_password_segura
DB_NAME=nombre_base_datos

# API Security
API_KEY=genera-uuid-aleatorio-aqui
API_BASE_URL=https://tu-dominio.com

# Logging
LOG_PATH=/home/tu_usuario/logs/assiten
```

## 🚀 Despliegue en cPanel

### Opción A: FTP/SFTP
```bash
# Desde tu PC
sftp user@your-domain.com
cd public_html
mkdir assiten
cd assiten
put -r *
```

### Opción B: File Manager cPanel
1. Abre cPanel > File Manager
2. Navega a /public_html
3. Sube assiten-backend.zip
4. Click derecho > Extract
5. Renombra carpeta a 'assiten'

### Opción C: SSH (Terminal cPanel)
```bash
cd ~/public_html
unzip assiten-backend.zip
mv assiten-backend assiten
cd assiten
```

## ✅ Verificación Post-Instalación

### Test 1: API responde
```bash
curl -X POST https://tu-dominio.com/assiten/api/v1/authorize \
  -H "X-API-Key: tu-api-key" \
  -H "Content-Type: application/json" \
  -d '{"telefono_id":"test:sig","accion":"test"}'
```

**Respuesta esperada:** 404 (device not found) o 403 (signature invalid)
**No debe haber:** 500 Internal Server Error

### Test 2: BD funciona
```bash
mysql -u assiten_user -p assiten -e "SELECT * FROM conductores LIMIT 1;"
```

### Test 3: Logs se crean
```bash
ls -la logs/
# Debe existir authorization.log
```

## 🔐 Seguridad

✅ Incluido:
- Firma digital HMAC-SHA256 del deviceId
- Token rotativo de autenticación (24h)
- Encriptación de credenciales
- Rate limiting (60 req/min por dispositivo)
- Detección de fraude
- Logging de intentos fallidos

⚠️ Configurar después:
- [ ] Cambiar API_KEY por una segura
- [ ] Cambiar contraseña de BD
- [ ] Habilitar HTTPS/SSL
- [ ] Configurar firewall

## 📞 Soporte

**Error común: 500 Internal Server Error**
```bash
# Ver logs:
tail -f logs/authorization.log

# Verificar permisos:
chmod 644 api/v1/authorize.php
chmod 755 api/v1/
```

**Error: Cannot connect to database**
```bash
# Verificar credenciales en .env:
mysql -h localhost -u DB_USER -p DB_PASS -e "USE DB_NAME; SHOW TABLES;"
```

**Error: HTTPS/SSL issues**
- Ve a cPanel > SSL/TLS Status
- Auto-generate Let's Encrypt certificate
- Fuerza HTTPS en .htaccess (ya incluido)

## 📚 Documentación Completa

Lee `docs/SECURITY_IMPROVEMENTS.md` para:
- Arquitectura de seguridad
- Flujo de autenticación
- Detección de fraude
- Troubleshooting avanzado

## 🎯 Siguiente Paso: Android Client

Una vez el backend esté corriendo:

1. Actualiza `BuildConfig.kt` en Android:
```kotlin
const val API_BASE_URL = "https://tu-dominio.com/assiten"
const val API_KEY = "tu-api-key-aqui"
```

2. Compila la app:
```bash
./gradlew build
```

3. Instala en dispositivo:
```bash
./gradlew installDebug
```

---

**Creado por:** Copilot  
**Última actualización:** 2026-06-17  
**Versión:** 1.0.0
