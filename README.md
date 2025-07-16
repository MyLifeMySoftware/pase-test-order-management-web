# 🔐 Sistema de Gestion de ordenes mediante JWT - Pase Project

Microservicio empresarial para gestión y administración, protegido mediante JWT. Parte del ecosistema de microservicios Pase, desarrollado en Spring Boot 3.3.13 y Java 21.

### Módulos

- **pase-test-database-lib**: Librería compartida con entidades JPA, repositorios y DTOs
- **pase-test-auth-web**: Microservicio de autenticación con API REST y lógica de negocio

## ✨ Características Principales

| Característica | Implementado | Descripción |
|---|:---:|---|
| **Integración con Auth	** | ✅ | Autenticación vía token emitido por Auth Web |
| **Role-Based Access** | ✅ | Sistema de roles granulares con permisos |
| **Validación Avanzada** | ✅ | Spring Validation con patrones personalizados |
| **Manejo de Excepciones** | ✅ | Global exception handler con respuestas estructuradas |
| **Monitoreo** | ✅ | Actuator, métricas personalizadas, health checks |
| **Documentación API** | ✅ | OpenAPI 3.0 con Swagger UI |

## 🛠️ Stack Tecnológico

### Backend Core
- **Java 21** - Última versión LTS
- **Spring Boot 3.3.13** - Framework principal
- **Spring Security 6** - Seguridad y autenticación
- **Spring Data JPA** - Persistencia de datos
- **Hibernate** - ORM con soporte Envers

### Base de Datos
- **PostgreSQL** - Base de datos principal
- **HikariCP** - Pool de conexiones optimizado
- **Spring Data Envers** - Auditoría de cambios

### Herramientas de Desarrollo
- **Lombok** - Reducción de boilerplate
- **MapStruct** - Mapeo de objetos
- **Micrometer** - Métricas y observabilidad
- **Checkstyle** - Calidad de código

## 🚀 Instalación y Configuración

### Prerrequisitos
```bash
Java 21+
Maven 3.8+
PostgreSQL 12+
```

### 1. Clonar el repositorio
```bash
git clone https://github.com/MyLifeMySoftware/pase-test-order-management-web
cd pase-project
```

### 2. Configurar Base de Datos
```sql
-- Crear base de datos
CREATE DATABASE pase_db;
CREATE USER owner WITH ENCRYPTED PASSWORD 'Owner123';
GRANT ALL PRIVILEGES ON DATABASE pase_db TO owner;
```

### 3. Variables de Entorno
```bash
# Crear archivo .env
cat > .env << EOF
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/pase_db
DB_USERNAME=owner
DB_PASSWORD=Owner123

# JWT Configuration
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=604800000
JWT_ISSUER=pase-auth-service

# Application Configuration
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8081
EOF
```

### 4. Compilar e Instalar
```bash
# Instalar librería de base de datos
cd pase-test-database-lib
mvn clean install

# Compilar microservicio de autenticación
cd ../pase-test-auth-web
mvn clean install
```

## 🔐 Configuración de Seguridad

### JWT Configuration
```yaml
jwt:
  secret: ${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
  access-token:
    expiration: ${JWT_ACCESS_TOKEN_EXPIRATION:900000}  # 15 minutos
  refresh-token:
    expiration: ${JWT_REFRESH_TOKEN_EXPIRATION:604800000}  # 7 días
  issuer: ${JWT_ISSUER:pase-auth-service}
```

### Usuarios por Defecto
```yaml
# Usuarios creados automáticamente en el primer arranque
Admin:
  username: admin
  password: Admin123!
  role: ADMIN
  
Test User:
  username: testuser  
  password: Test123!
  role: USER
  
Moderator:
  username: moderator
  password: Mod123!
  role: MODERATOR
```

## 📚 API Documentation

### Base URL
```
http://localhost:8081/swagger-ui/index.html#/
```

### Endpoints Principales

#### 🔓 Endpoints Públicos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `//api/v1/order-management` | Administracion y gestion de ordenes |

## 🧪 Testing

### Ejecutar Tests
```bash
# Tests unitarios
mvn test

# Tests de integración
mvn test -Dspring.profiles.active=test

# Tests con coverage
mvn clean test jacoco:report
```

### Test de Endpoints
```bash
# Verificar salud del servicio
curl http://localhost:8081/api/v1/test/health

# Verificar documentación
curl http://localhost:8081/v3/api-docs

# Acceder a Swagger UI
open http://localhost:8081/swagger-ui/index.html
```

## 📁 Estructura del Proyecto

```
pase-project/
├── pase-test-database-lib/              # Librería compartida
│   ├── src/main/java/
│   │   └── pase/test/com/database/
│   │       ├── entity/                  # Entidades JPA
│   │       │   └── user/
│   │       │       ├── User.java
│   │       │       ├── Role.java
│   │       │       ├── UserPermission.java
│   │       │       └── RefreshToken.java
│   │       ├── repository/              # Repositorios JPA
│   │       │   └── user/
│   │       ├── dto/                     # Data Transfer Objects
│   │       │   └── user/
│   │       ├── exception/               # Excepciones personalizadas
│   │       └── config/                  # Configuraciones
│   └── pom.xml
│
├── pase-test-order-management-web/                  # Microservicio de administracion
│   ├── src/main/java/
│   │   └── pase/test/com/order/management/
│   │       ├── controller/              # Controllers REST
│   │       │   └── OrderManagementController.java
│   │       ├── service/                 # Lógica de negocio
│   │       │   └── UserManagementService.java
│   │       ├── security/                # Configuración de seguridad
│   │       │   └── jwt/
│   │       │       ├── JwtService.java
│   │       │       ├── JwtAuthenticationFilter.java
│   │       │       └── JwtAuthenticationEntryPoint.java
│   │       ├── config/                  # Configuraciones
│   │       ├── utils/                   # Utilidades
│   │       └── boot/                    # Inicialización
│   │           └── OrderManagementDataInitializationService.java
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── application.yml
│   └── pom.xml
│
└── README.md
```

## 🐛 Troubleshooting

### Problemas Comunes

#### Error de Compilación
```bash
# Error: "Association targets java.security.Permission"
# Solución: Limpiar y recompilar
mvn clean install -DskipTests
```

#### Error de JWT
```bash
# Error: "JWT token expired"
# Verificar configuración
jwt.access-token.expiration=900000  # 15 minutos
jwt.refresh-token.expiration=604800000  # 7 días
```

#### Error de Base de Datos
```bash
# Error: "User not found"
# Verificar inicialización de datos
spring.jpa.hibernate.ddl-auto=update
```

#### Error de Conexión
```bash
# Error: "Connection refused"
# Verificar PostgreSQL
systemctl status postgresql
systemctl start postgresql
```

### Logs de Debugging
```bash
# Habilitar debug logging
logging.level.pase.test.com=DEBUG
logging.level.org.springframework.security=DEBUG

# Ver logs en tiempo real
tail -f logs/auth-service.log
```

## 🤝 Contribución

### Proceso de Contribución
1. **Fork** el repositorio
2. **Crear** branch de feature: `git checkout -b feature/amazing-feature`
3. **Commit** cambios: `git commit -m 'Add amazing feature'`
4. **Push** al branch: `git push origin feature/amazing-feature`
5. **Abrir** Pull Request

### Estándares de Código
- Seguir las convenciones de Google Java Style
- Usar Checkstyle para validación
- Mantener coverage de tests > 80%
- Documentar APIs con OpenAPI

### Equipo de Desarrollo
- **Lead Developer**: Erick Antonio Reyes Montalvo
- **Email**: montalvoerickantonio@gmail.com
- **GitHub**: [@ErickReyesMontalvo](https://github.com/ErickReyesMontalvo)

---

<div align="center">
  <p>Hecho con ❤️</p>
  <p>
    <a href="#-sistema-de-autenticación-jwt---pase-project">⬆ Volver al inicio</a>
  </p>
</div>
