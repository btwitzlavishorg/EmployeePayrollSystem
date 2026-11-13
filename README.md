# Employee Payroll Management System

Java 11 payroll web app with an embedded JDK HTTP server, vanilla HTML/CSS/JS frontend, and MySQL (with automatic H2 fallback).

## Quick start

```bash
./scripts/start.sh
```

Then open **http://localhost:8080/**

Stop the server:

```bash
./scripts/stop.sh
```

Optional custom port:

```bash
PORT=9090 ./scripts/start.sh
PORT=9090 ./scripts/stop.sh
```

Manual run (from project root):

```bash
mvn clean package
java -jar target/payroll-system.jar
```

## Demo logins

| Username   | Password  | Role       |
|------------|-----------|------------|
| `admin`    | `admin123`| ADMIN      |
| `hrmanager`| `admin123`| HR_MANAGER |

## Database setup

### Option A — zero config (recommended for local demo)

If MySQL is not available on `localhost:3306`, the app automatically uses an embedded H2 database under:

`~/.payroll-system/data/`

Schema and sample data are loaded from the classpath on first run.

### Option B — MySQL

1. Install MySQL 8+ and ensure it is running.
2. Create/override credentials in `config/db.properties` (or env vars):

```properties
db.url=jdbc:mysql://localhost:3306/payroll_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true
db.user=root
db.password=YOUR_PASSWORD
```

Environment overrides: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.

Optional H2 data directory override: `PAYROLL_DATA_DIR` or `-Dpayroll.data.dir=...`.

## Features

- Session login/logout with HTTP-only cookie; APIs require authentication
- Role checks for HR writes and payroll mutations
- Employees, departments, attendance, leaves, salary structure
- Payroll preview/process/batch, payslip PDF, reports CSV/PDF
- Attendance absences, half-days, and unpaid leave affect LOP

## Tests

```bash
mvn test
```

## Project layout

- `src/main/java` — Java backend (services, DAO, HTTP API)
- `frontend/` — SPA served by the embedded server
- `database/` — SQL scripts (also packaged under `src/main/resources/database/`)
- `scripts/` — start/stop helpers
