# Instalación y Configuración de la Base de Datos

## Requisitos Previos
- PostgreSQL 18 o última versión

## Pasos de Instalación

### 1. Instalar PostgreSQL

1. Descargar PostgreSQL desde la [página oficial](https://www.postgresql.org/download/)
2. Ejecutar el instalador
3. Durante la instalación, configurar:
   - **Usuario**: `postgres`
   - **Contraseña**: `postgres`
   - **Puerto**: `5432` (por defecto)
4. Instalar **pgAdmin** (incluido en el instalador) para gestionar la base de datos

### 2. Crear Base de Datos y Usuario

1. Abrir **pgAdmin** o **SQL Shell (psql)**
2. Conectarse con el usuario `postgres` y la contraseña configurada
3. Ejecutar las queries del archivo `src/main/resources/sql/crearBaseYUsuario.sql` **en orden**:

```sql
-- Crear la base de datos
CREATE DATABASE dronewars;

-- Crear el usuario admin
CREATE USER admin WITH PASSWORD 'admin';

-- Otorgar privilegios
GRANT ALL PRIVILEGES ON DATABASE dronewars TO admin;
GRANT ALL ON SCHEMA public TO admin;
GRANT CREATE ON SCHEMA public TO admin;
ALTER SCHEMA public OWNER TO admin;
```

### 3. Creación Automática de Tablas

Las tablas de la base de datos se crean **automáticamente** cuando se levanta la aplicación por primera vez.

La aplicación ejecuta el script `src/main/resources/sql/crearTablas.sql` que crea:
- Tablas principales: `equipo`, `tipo_unidad`, `partida`, `jugador`, `unidad`
- Tablas específicas: `portadron_naval`, `portadron_aereo`, `dron_naval`, `dron_aereo`
- Tabla de puntajes: `puntaje`
- Datos iniciales para equipos y tipos de unidad

**Nota**: No es necesario ejecutar manualmente el script `crearTablas.sql`. La aplicación lo gestiona automáticamente.

## Verificación

Para verificar que todo está correctamente configurado:

1. Conectarse a la base de datos `dronewars` con el usuario `admin`
2. Verificar que el schema `public` existe
3. Después de ejecutar la aplicación, verificar que las tablas fueron creadas correctamente

## Configuración de la Aplicación

Asegurarse de que el archivo de configuración de la aplicación (`application.properties` o `application.yml`) tenga las siguientes credenciales:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/dronewars
spring.datasource.username=admin
spring.datasource.password=admin
```
