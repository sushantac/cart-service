# Cart Service

Shopping cart for the e-commerce platform: cart CRUD, quantity updates, price/stock validation against the Product Service, and checkout initiation.

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/cart` | Current user's cart |
| POST | `/api/v1/cart/items` | Add item |
| PUT | `/api/v1/cart/items/{itemId}` | Update quantity |
| DELETE | `/api/v1/cart/items/{itemId}` | Remove item |
| DELETE | `/api/v1/cart` | Clear cart |

## Events

- Published: `cart.checkout.initiated`
- Consumed: `product.catalog.updated`, `product.catalog.deleted`
- Idempotent consumer (eventId dedup) — see SDLC plan section 5.3

## Stack

Java 21, Spring Boot 3.4, Liquibase (schema `cart`), PostgreSQL, Redis, Kafka. Validates stock/pricing via Product Service over REST.

## Development

```bash
./mvnw spring-boot:run
# http://localhost:8082
```

Full local orchestration lives in the `sdlc` repo (`make dev`).