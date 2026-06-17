#!/bin/bash
#
# Script para crear ZIP de deployment
# Uso: bash create_deployment_zip.sh
#

echo "📦 Creando ZIP de deployment..."

# Excluir archivos innecesarios
exclude_patterns=(
    "--exclude=.git"
    "--exclude=.github"
    "--exclude=.DS_Store"
    "--exclude=*.gradle"
    "--exclude=build/"
    "--exclude=.gradle/"
    "--exclude=.idea/"
    "--exclude=config/.env"  # NO incluir .env real
    "--exclude=*.log"
    "--exclude=node_modules/"
    "--exclude=.env.local"
)

# Crear ZIP
zip -r assiten-backend.zip backend/ docs/ README.md "${exclude_exclude_patterns[@]}" 2>/dev/null

echo "✅ ZIP creado: assiten-backend.zip"
echo "📊 Tamaño: $(du -h assiten-backend.zip | cut -f1)"
echo ""
echo "📝 Contenido del ZIP:"
echo "  ✓ Backend (API + BD migrations)"
echo "  ✓ Documentación (SECURITY_IMPROVEMENTS.md)"
echo "  ✓ Scripts (.htaccess, deploy_assiten.sh, test_authorize.php)"
echo ""
echo "🚀 Próximo paso: Descarga assiten-backend.zip y sube a cPanel"
