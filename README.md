# Order Service

Order Service is a Spring Boot microservice responsible for managing restaurant orders and order items.

## Configuration

The application uses environment variables for database configuration.

### Environment Variables

Create a local `.env` file in the project root, according to `.env.example`, provided as a template


## Running with Docker Compose

Make sure Docker Desktop is running.

### 1. Start the application and PostgreSQL

```bash
docker compose up -d --build
```

### 2. Check container status

```bash
docker compose ps
```

The following services should be running:

| Service | Description |
|---|---|
| `order-service` | Spring Boot application |
| `order-db` | PostgreSQL database |

The `order-db` container should eventually show a **healthy** status.

### 3. View application logs (if needed)

```bash
docker compose logs -f order-service
```

## API

The Order Service is available at:

```text
http://localhost:8080
```

### Create an Order

**POST** `/order`

Test request:

```json
{
  "userId": 1,
  "tableId": 1,
  "items": [
    {
      "dishId": 1,
      "quantity": 2
    }
  ]
}
```


Expected response:

```json
{
  "id": 1,
  "userId": 1,
  "tableId": 1,
  "status": "NEW",
  "createdAt": "2026-09-18T20:06:29",
  "items": [
    {
      "id": 1,
      "dishId": 1,
      "name": "Temporary dish",
      "quantity": 2,
      "price": 0
    }
  ]
}
```

(at the moment, the order item's name and price are provided by a temporary stub)
### Get All Orders

**GET** `/order`

```text
GET http://localhost:8080/order
```

### Get Order by ID

**GET** `/order/{id}`

```text
GET http://localhost:8080/order/{id}
```

### Delete Order

**DELETE** `/order/{id}`

```text
DELETE http://localhost:8080/order/{id}
```

## Database

The Order Service connects to PostgreSQL using the Docker service name:

```text
order-db:5432
```

PostgreSQL data is stored in the Docker volume:

```text
order-db-data
```

## Testing Database Persistence

The PostgreSQL data should survive container restarts.

### 1. Create an order

Send a `POST /order` request and note the returned order ID.

### 2. Verify the order exists

Send:

```text
GET http://localhost:8080/order
```

Make sure the newly created order is present.

### 3. Stop the containers

```bash
docker compose down
```


### 4. Start the containers again

```bash
docker compose up -d
```

Check the container status:

```bash
docker compose ps
```

### 5. Verify the data

Send:

```text
GET http://localhost:8080/order
```

The order created before the restart should still be present.

This confirms that PostgreSQL data is persisted in the Docker volume.


## Stopping the Application

### Stop containers and keep database data

```bash
docker compose down
```

Start them again with:

```bash
docker compose up -d
```

### Remove containers and the PostgreSQL volume

```bash
docker compose down -v
```

> ⚠️ **Warning:** `docker compose down -v` permanently removes the local PostgreSQL data stored in the Docker volume.
