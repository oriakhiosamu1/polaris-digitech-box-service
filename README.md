# Polaris DigiTech - Box Delivery Service

A REST API service for managing delivery boxes that can carry and deliver small items to remote locations.

## Technologies Used

- Java 21
- Spring Boot 4.1.1
- Spring Data JPA
- MySQL
- Lombok
- SpringDoc OpenAPI (Swagger)
- Maven

## Features

- Create a new box
- Load a box with items
- Check loaded items for a given box
- Check available boxes for loading
- Check battery level of a box

### Business Rules Enforced

- A box cannot be loaded with items that exceed its weight limit
- A box cannot be in `LOADING` state if battery level is below 25%

## Prerequisites

- Java 21+
- Maven 3.8+
- MySQL 8.x running on `localhost:3306`

## Database Setup

1. Make sure MySQL is running.
2. Update the database credentials in `src/main/resources/application.properties`:

   ```properties
   spring.datasource.username=root
   spring.datasource.password=
   ```

## How to Build

```bash
./mvnw clean install
```

## How to Run
By default, the application uses MySQL as the database. To run the application with MySQL, just click the run button in your IDE or use the following command:

```bash
./mvnw spring-boot:run (for MySQL database)
```
```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=h2" (for in-memory H2 database)
```

## How to Test

```bash
./mvnw test
```

## API Documentation (Swagger)
Once the app is running, visit http://localhost:8080/swagger-ui.html 

## API Endpoints

| Method | Endpoint                 | Description                     |
|--------|--------------------------|---------------------------------|
| POST   | /api/boxes               | Create a new box                |
| POST   | /api/boxes/{id}/load     | Load a box with items           |
| GET   | /api/boxes/{txref}/items | Get loaded items of a box       |
| GET   | /api/boxes/available     | Get boxes available for loading |
| GET   | /api/boxes/{txref}/batter | Get battery level of a box      |

## Preloaded Data
On first startup, the application loads 5 sample boxes