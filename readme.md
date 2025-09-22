# E-commerce Analytics Platform

An analytics platform that provides an interactive dashboard, machine learning powered insights (customer segmentation, recommendations, forecasting), and observability, all containerized for easy deployment.

---

## Features

* **Interactive Dashboard** – KPIs and visualizations for orders, revenue, and customers.
* **Machine Learning Services**

  * Customer segmentation
  * Product recommendations
  * Demand forecasting (triggered by users)
* **Microservices Architecture** – Spring Boot backend, React frontend, and a Python ML service.
* **Event-Driven Processing** – Kafka integration for ML workflows.
* **Observability & Monitoring** – Jaeger tracing, metrics collection, and logs.
* **CI/CD** – Automated testing, container builds, and deployments.
* **Deployment** – Docker Compose (local).

---

## Tech Stack

* **Backend**: Spring Boot (Java), REST APIs
* **Frontend**: React + Tailwind
* **ML Service**: Python (forecasting, clustering, recommendation)
* **Data**: PostgreSQL + Flyway migrations
* **Cache**: Redis
* **Messaging**: Apache Kafka
* **Containerization**: Docker, Docker Compose
* **Monitoring**: Jaeger, Prometheus/Grafana
* **CI/CD**: GitHub Actions pipeline

---

## Installation

### Prerequisites

* Docker & Docker Compose

### Run with Docker Compose

```bash
git clone https://github.com/MARCUS-IMPERATOR/ecommerce-analytics.git
cd ecommerce-analytics
docker-compose up --build
```

This will start:

* Backend API (`http://localhost:8080`)
* Frontend dashboard (`http://localhost:3000`)
* ML Service (`http://localhost:5000`)
* Jaeger UI (`http://localhost:16686`)
* Grafana (`http://localhost:3001`) (admin/admin)
---

## Testing

Run backend tests:

```bash
mvn test
```

CI/CD pipeline runs tests automatically pushes to `main`.

---