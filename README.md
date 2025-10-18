# 🏦 Andina Trading

**Andina Trading** es una aplicación web universitaria desarrollada con arquitectura de **microservicios** que simula un entorno bursátil, permitiendo la gestión de inversionistas, empresas emisoras, contratos, portafolios y transacciones de compra/venta de acciones.

El sistema está diseñado para representar la interacción entre distintos actores del mercado financiero (usuarios, comisionistas y la bolsa de valores) de manera distribuida y escalable.

---

## ⚙️ Arquitectura del Proyecto

El proyecto está compuesto por múltiples microservicios desarrollados en **Spring Boot**, comunicados mediante **Eureka Server** (descubrimiento de servicios) y gestionados a través de **API Gateway**.  
Todos los servicios se orquestan con **Docker Compose**.

### 🧩 Microservicios

| Servicio | Descripción | Puerto |
|-----------|--------------|--------|
| 🧭 **Eureka Service** | Servidor de registro y descubrimiento de microservicios. | `8761` |
| 🚪 **API Gateway** | Punto de entrada central que enruta las solicitudes a los microservicios. | `8080` |
| 💼 **Gestión Service** | Administra usuarios, empresas, contratos y portafolios. | `8081` |
| 📊 **Bolsa Service** | Gestiona las órdenes de compra/venta y simula el intercambio de acciones. | `8082` |
| 🗄️ **MySQL** | Base de datos relacional para los microservicios. | `3306` |

---

## 🛠️ Tecnologías Utilizadas

- **Java 17**
- **Spring Boot 3.5.6**
- **Spring Cloud Netflix Eureka**
- **Spring Cloud Gateway**
- **Spring Data JPA**
- **MySQL 8**
- **Docker & Docker Compose**
- **Maven**

---

## 🚀 Ejecución del Proyecto con Docker

> 💡 Asegúrate de tener instalados **Docker Desktop** y **Docker Compose** antes de continuar.

### Pasos

```bash
git clone https://github.com/yeferbosk/Andina-Trading.git
cd Andina-Trading

Construir las imágenes
docker compose build

Levantar los contenedores (en la raiz del proyecto)
docker compose up -d

Verificar contenedores activos
docker ps
