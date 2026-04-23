# Customer Management System

This project contains a Spring Boot backend and a React frontend for a customer management system with:

- Customer create, update, view, and table listing
- Multiple mobile numbers
- Multiple addresses with city and country master data
- Customer-to-customer family member links
- Bulk customer create or upsert from `.xlsx` Excel uploads

## Tech Stack

- Java 8 compatible backend
- Spring Boot with Maven
- MariaDB
- React JS
- Axios
- JUnit
- Apache POI for streaming Excel parsing

All third-party libraries used here are free and stable mainstream packages.

## Project Structure

- `backend` - Spring Boot API
- `frontend` - React + Vite UI
- `database/ddl.sql` - schema
- `database/dml.sql` - seed data for country and city master tables

## Backend Setup

1. Create a MariaDB database named `customer_management`.
2. Run `database/ddl.sql`.
3. Run `database/dml.sql`.
4. Update database credentials in `backend/src/main/resources/application.properties`.
5. Make sure `JAVA_HOME` points to a full JDK.
6. Start the backend:

```powershell
cd backend
mvn spring-boot:run
```

The backend runs on `http://localhost:8080`.

## Frontend Setup

1. Install Node.js 18+.
2. Start the frontend:

```powershell
cd frontend
npm install
npm run dev
```

The frontend runs on `http://localhost:5173`.

## Bulk Upload Format

Upload a `.xlsx` file with a header row containing:

- `Name`
- `Date of Birth`
- `NIC Number`

Supported date formats:

- `yyyy-MM-dd`
- `dd/MM/yyyy`
- `MM/dd/yyyy`
- `d/M/yyyy`
- `M/d/yyyy`
- `dd-MM-yyyy`
- `d-M-yyyy`

Bulk upload modes:

- `UPSERT` - creates new customers and updates existing customers by NIC
- `CREATE_ONLY` - creates only new customers and skips existing NICs

The bulk import service processes the file in batches and now queues uploads as background jobs so the browser request is not held open for very large files.

## Testing

Backend tests:

```powershell
cd backend
mvn test
```

Frontend tests:

```powershell
cd frontend
npm install
npm run test:run
```

## Notes

- The frontend fetches country and city master data for dropdowns, but there is no master-data management screen.
- The bulk upload currently supports `.xlsx` files for streaming safety and large-file handling, and the UI polls a background import job for status.
- The backend batch import updates only the mandatory fields because the upload requirement is based on mandatory columns.
