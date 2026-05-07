# USAGE – How to Run the POS Project Locally

## Overview
This repository contains a **full‑stack POS (Point‑of‑Sale) system**:
- **Backend** – Spring Boot 17 (Java) REST API (`backend/`)
- **Frontend** – Node.js 20 + Express + Nunjucks (`frontend/`)
- **Specifications** – Detailed design, requirements and task breakdown in `specifications.md` and `design.md`.

The goal of this guide is to help you **run the project locally**, understand its structure, and know where to find the specifications.

---

## Prerequisites
| Tool | Minimum Version |
|------|-----------------|
| **Java** | 17 (OpenJDK) |
| **Maven** | 3.9+ |
| **Node.js** | 20.x |
| **npm** | 10.x |
| **Git** | any |

Make sure `java`, `mvn`, `node` and `npm` are available in your `PATH`.

---

## 1. Clone the Repository (if you haven't already)
```bash
# From your workspace root
git clone https://github.com/your-org/pos-project.git
cd POS-PROJECT/pos-repo
```

---

## 2. Backend – Build & Run
```bash
# Inside the repository root
cd backend
# Install dependencies (Maven will download them automatically)
./mvnw clean install   # Windows: .\mvnw.cmd clean install
# Run the application
./mvnw spring-boot:run   # Windows: .\mvnw.cmd spring-boot:run
```
The backend will start on **http://localhost:8080** and expose Swagger UI at **http://localhost:8080/swagger-ui.html**.

> **Tip**: If you see a port conflict, change the `server.port` property in `src/main/resources/application.yaml`.

---

## 3. Frontend – Install & Start
```bash
cd ../frontend
npm install
npm start
```
The frontend runs on **http://localhost:3000** and expects the backend at `http://localhost:8080`. Adjust the `.env` file if you change the backend URL or port:
```env
BACKEND_URL=http://localhost:8080
PORT=3000
```

---

## 4. Run Tests
### Backend Tests
```bash
cd ../backend
./mvnw test   # Windows: .\mvnw.cmd test
```
### Frontend Tests
```bash
cd ../frontend
npm test
```
All tests should pass (`✅`).

---

## 5. Project Structure
```
pos-repo/
├─ backend/          # Spring Boot API
│   ├─ src/main/java/com/pos/...   # Domain, Application, Infrastructure
│   └─ pom.xml
├─ frontend/         # Node/Express UI
│   ├─ src/          # domain, application, infrastructure, views
│   ├─ .env
│   └─ package.json
├─ specifications.md # High‑level functional specs
├─ design.md        # Architectural decisions & UI wireframes
└─ tasks.md        # Incremental implementation tasks (used by Kiro)
```

---

## 6. Specifications & Design Docs
- **`specifications.md`** – Lists every user story, acceptance criteria, and functional requirement.
- **`design.md`** – Describes the system architecture (hexagonal backend, layered frontend), data models, and component diagrams.
- **`tasks.md`** – Breaks the work into small, testable steps. Useful when extending the project.

Read them to understand *what* the system does and *how* it is organized before making changes.

---

## 7. Common Commands Summary
| Action | Command |
|--------|---------|
| Build backend | `./mvnw clean install` |
| Run backend | `./mvnw spring-boot:run` |
| Test backend | `./mvnw test` |
| Install frontend deps | `npm install` |
| Run frontend | `npm start` |
| Test frontend | `npm test` |
| Lint (if added) | `npm run lint` |

---

## 8. Troubleshooting
- **Port already in use** – Change `server.port` in `backend/src/main/resources/application.yaml` or `PORT` in the frontend `.env`.
- **Missing Java version** – Install OpenJDK 17 and set `JAVA_HOME`.
- **Dependency errors** – Run `./mvnw clean install -U` to force update Maven dependencies.
- **Frontend cannot reach backend** – Verify `BACKEND_URL` in `.env` matches the actual backend address.

---

## 9. Contributing
1. Fork the repository.
2. Create a feature branch.
3. Follow the existing **Spec‑Driven** workflow: update `requirements.md`, `design.md`, `tasks.md` before adding code.
4. Submit a Pull Request with updated specs and passing tests.

---

*Enjoy building and extending the POS system!*
