# Order Service

Order Service is a Spring Boot microservice responsible for managing restaurant orders and order items.

## Configuration

The application uses environment variables for database configuration.

### Environment Variables

Create a local `.env` file in the project root, according to `.env.example`, provided as a template

The host port used by the **Order Service** also configured in the .env file, under the name `SERVER_HOST_PORT`.
This port is used on the host machine and can be changed if it conflicts with ports used by other local services.


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
## Running with Kubernetes

### 1. Configure `order-secret.yaml`

Configure `k8s/order-secret.yaml` using `k8s/order-secret.yaml.example`.


### 2. Build the Docker image

Build the `order-service` image:

```shell
docker build -t order-service:latest .
```

Docker Desktop Kubernetes can use the locally built image, so no image loading step is required.

### 3. Run `dry-run`

Validate the Kubernetes manifests on the client side:

```shell
kubectl apply -f k8s/. --dry-run=client
```

Validate the manifests against the Kubernetes API server:

```shell
kubectl apply -f k8s/. --dry-run=server
```

### 4. Apply the Kubernetes manifests

Deploy the database, service, ConfigMap, Secret, and other Kubernetes resources:

```shell
kubectl apply -f k8s/.
```

### 5. Check the pods

Check the status of the deployed pods:

```shell
kubectl get pods
```

The expected result is two `order-service` replicas and one `order-db` pod in the `Running` state.

### 6. Check the deployment

```shell
kubectl get deployment order-service
```

The `READY` value should be `2/2`.

### 7. Port-forward the service

To access the `order-service` from the host machine:

```shell
kubectl port-forward svc/order-service 8085:8080
```

The service is then available at:

```text
http://localhost:8085
```

For example, the health endpoint can be checked at:

```text
http://localhost:8085/actuator/health
```

## Communication with Payment Service

The `order-service` communicates with the `payment-service` through a Kubernetes `ClusterIP` Service.

The payment service URL is configured using a Kubernetes ConfigMap:

```yaml
PAYMENT_SERVICE_URL: "http://payment-service:8080"
```

The `order-service` uses this value to send payment creation requests to the `payment-service`.

When a new order is created, the following flow is performed:

1. `order-service` saves the order to the database.
2. `order-service` calls `PaymentClient`.
3. `PaymentClient` sends a `POST /payment` request to:

```text
http://payment-service:8080/payment
```

4. Kubernetes DNS resolves `payment-service` to the corresponding `ClusterIP` Service.
5. The Kubernetes Service forwards the request to one of the available `payment-service` pods.
6. `payment-service` creates the payment and returns the payment data.
7. `order-service` receives the response and completes the order creation request.

Both services run with two replicas in Kubernetes. The `payment-service` ClusterIP Service distributes incoming requests between the available payment-service pods.

To verify the service endpoints:

```shell
kubectl get endpoints payment-service
```

To verify the configured payment service URL inside an `order-service` pod:

```shell
kubectl exec deployment/order-service -- printenv PAYMENT_SERVICE_URL
```

Expected output:

```text
http://payment-service:8080
```

The interaction can also be verified through the application logs. When an order is created, `order-service` logs the outgoing payment request, while `payment-service` logs the received payment creation request.
### Verifying Order → Payment Communication

After starting the port-forward for `order-service`, create a new order:

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

The order creation triggers a request from `order-service` to `payment-service`.

To verify that the payment was created, port-forward the `payment-service`:

```shell
kubectl port-forward svc/payment-service 8084:8080
```

Then request all payments:

```shell
curl http://localhost:8084/payment
```

The response should contain a payment associated with the newly created order:

```json
[
  {
    "id": 1,
    "orderId": 1,
    "status": "PENDING",
    "createdAt": "2026-09-25T..."
  }
]
```

This confirms that creating an order in `order-service` successfully triggers payment creation in `payment-service`.


## API

The Order Service is available at:

```text
http://localhost:<chosen port>
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
GET http://localhost:8085/order
```

### Get Order by ID

**GET** `/order/{id}`

```text
GET http://localhost:8085/order/{id}
```

### Delete Order

**DELETE** `/order/{id}`

```text
DELETE http://localhost:8085/order/{id}
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
GET http://localhost:8085/order
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
GET http://localhost:8085/order
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
