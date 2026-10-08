# Dafne Daniela

App Android creada con **APK Studio**.

- Identificador: `com.dafnedaniela.app`
- Contenido: archivos en `app/src/main/assets/www/`

## Cómo se compila
Cada vez que se sube un cambio a la rama `main`, GitHub ejecuta `.github/workflows/build.yml`, compila el APK y lo publica en **Releases**.

Descarga directa de la última versión (repositorio público):
`https://github.com/USUARIO/REPOSITORIO/releases/latest/download/dafne-daniela.apk`

## Importante: la llave de firma
La primera compilación crea `keystore/release.jks`. **No la borres**: sin ella no podrás instalar actualizaciones encima de la app ya instalada.
Si vas a publicar en Google Play, crea tu propia llave privada y guárdala como secreto (KS_PASS).

## Compilar en tu PC (opcional)
Abre esta carpeta con Android Studio y usa *Build → Build APK(s)*.
