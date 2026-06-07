# Court Case Management System (CCMS)

A beginner-friendly full-stack Court Case Management System built with:

- Backend: Java Servlet + JDBC
- Frontend: React (functional components)
- Database: MySQL
- Server: Apache Tomcat

## Features

- Login and register with role-based access: `Admin`, `Lawyer`, `Staff`
- Session-based authentication
- Case CRUD, search, and status filters
- Hearing management with next-hearing tracking
- Document management using file paths
- Dashboard cards and analytics
- Demo-data fallback in the frontend when the backend API is unavailable

## Project Structure

```text
code/
├── ccms-backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ccms/
│       │   ├── config/
│       │   ├── dao/
│       │   ├── filter/
│       │   ├── model/
│       │   ├── servlet/
│       │   └── util/
│       └── webapp/WEB-INF/web.xml
├── ccms-frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── components/
│       ├── pages/
│       ├── App.jsx
│       ├── api.js
│       ├── demoData.js
│       ├── main.jsx
│       └── style.css
└── database/
    └── ccms_schema.sql
```

## Database Setup

1. Create a backend environment file from [`ccms-backend/.env.example`](ccms-backend/.env.example).
2. Update `CCMS_DB_URL`, `CCMS_DB_USER`, `CCMS_DB_PASSWORD`, and `CCMS_TOMCAT_HOME` if needed.
3. Optional but recommended for a full reset: run [`database/ccms_schema.sql`](database/ccms_schema.sql).

The backend can now bootstrap a fresh `ccms_db` schema and seed starter data on first connection if your MySQL user is allowed to create databases and tables.

### Sample Users

- Admin: `admin` / `admin`
- Lawyer: `lawyer@ccms.com` / `lawyer123`
- Staff: `staff@ccms.com` / `staff123`
- Judge: `judge@ccms.com` / `judge123`
- Citizen: `citizen@ccms.com` / `citizen123`

## Backend Setup

1. Open the backend project: [`ccms-backend/`](ccms-backend/)
2. Create `ccms-backend/.env` from the example file if you have not already.
3. Build the WAR:

```powershell
cd "c:\Users\ASUS\Desktop\minor 2\code\ccms-backend"
mvn clean package
```

4. Deploy `target/ccms-backend.war` to Apache Tomcat, or start it from the repo root with:

```powershell
cd "c:\Users\ASUS\Desktop\minor 2\code"
npm run backend
```

The root launcher builds the WAR, copies it into Tomcat `webapps`, and starts Tomcat by resolving `CCMS_TOMCAT_HOME` or common local Tomcat locations automatically.

5. The backend base URL becomes:

```text
http://localhost:8080/ccms-backend
```

## Frontend Setup

1. Open the frontend project: [`ccms-frontend/`](ccms-frontend/)
2. Create `ccms-frontend/.env` from [`ccms-frontend/.env.example`](ccms-frontend/.env.example) if you want AI features or a custom backend URL.
3. Install dependencies:

```powershell
cd "c:\Users\ASUS\Desktop\minor 2\code\ccms-frontend"
npm install
```

4. Start the React app:

```powershell
npm run dev
```

5. Open:

```text
http://localhost:5173
```

The frontend uses `VITE_API_BASE_URL` when provided and otherwise defaults to
`http://localhost:8080/ccms-backend` during local Vite development.

## Root Commands

From the repo root you can now use:

```powershell
npm run backend
npm run frontend
npm run start
```

`npm run backend` builds the WAR, deploys the latest artifact to Tomcat, and starts the backend. `run-project.bat` now uses the same root scripts instead of hardcoded Tomcat paths.

The React frontend uses `/login` as the entry route and protects `/dashboard`,
`/cases`, `/hearings`, `/documents`, `/notifications`, and `/profile` with
session-based authentication.

## REST API Endpoints

### Auth

- `POST /login`
- `POST /logout`
- `GET /session`
- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/logout`
- `GET /api/auth/session`

### Cases

- `GET /api/cases`
- `POST /api/cases`
- `PUT /api/cases/{id}`
- `DELETE /api/cases/{id}`
- `GET /api/cases/search?query=`
- `GET /api/cases/filter?status=`

### Hearings

- `POST /api/hearings`
- `GET /api/hearings/{caseId}`

### Documents

- `POST /api/documents`
- `GET /api/documents/{caseId}`

### Dashboard

- `GET /api/dashboard`

### Analytics

- `GET /api/analytics/status`
- `GET /api/analytics/delay`
- `GET /api/analytics/judge-load`

## RBAC Rules Used

- `Admin`: full access including delete
- `Lawyer`: create and update cases, hearings, and documents
- `Staff`: view cases/dashboard/analytics and manage hearings/documents

## Notes

- The backend uses `HttpSession` for session storage.
- Passwords are hashed with SHA-256 for a simple beginner-friendly setup.
- The frontend switches to demo data automatically if API requests fail.
- Comments were added in places where the logic benefits from explanation without over-commenting simple lines.
