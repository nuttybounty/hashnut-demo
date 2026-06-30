# HashNut Demo Shop (Java)

A demo merchant application demonstrating how to integrate with HashNut Payment API (V4) using the [hashnut-sdk](https://github.com/nuttybounty/hashnut-sdk). Supports multi-chain payment (ERC20 / TRC20).

## Tech Stack

- **Backend**: Java 11 + Spring Boot 2.7
- **Database**: PostgreSQL
- **Payment**: [HashNut Java SDK](https://github.com/nuttybounty/hashnut-sdk) (V4, via JitPack)
- **Frontend**: [hashnut-demo-web](https://github.com/nuttybounty/hashnut-demo-web) (React + TypeScript)

## Prerequisites

- Java 11+
- Maven 3.6+
- PostgreSQL 14+
- Node.js 18+ (for frontend)
- A HashNut merchant account with API Key
- ngrok (for local development callback)

## Quick Start

### 1. Clone

```bash
git clone https://github.com/nuttybounty/hashnut-demo.git
cd hashnut-demo
```

> SDK dependency is resolved automatically via [JitPack](https://jitpack.io/#nuttybounty/hashnut-sdk) — no manual install needed.

### 2. Create Database

```bash
psql -U postgres -c "CREATE DATABASE demo_shop;"
```

### 3. Configure Seed Data

Edit `migrate.sql` to fill in your API credentials:

```sql
INSERT INTO t_hashnut_api_key (chain_code, splitter, access_key_id, secret_key) VALUES
    ('erc20', '0x...your-eth-splitter...', 'your-access-key-id', 'your-secret-key'),
    ('trc20', 'T...your-tron-splitter',    'your-access-key-id', 'your-secret-key');
```

Then run:

```bash
psql -U postgres -d demo_shop -f migrate.sql
```

### 4. Configure Application

Edit `src/main/resources/application.yml` if needed (database connection, etc.):

```yaml
server:
  port: 1800

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/demo_shop
    username: postgres
    password: postgres

hashnut:
  test-mode: false   # true = testnet, false = production
  base-url: ""       # Leave empty for default
```

### 5. Run Backend

```bash
mvn spring-boot:run
```

The server starts on `http://localhost:1800`.

### 6. Run Frontend

```bash
git clone https://github.com/nuttybounty/hashnut-demo-web.git
cd hashnut-demo-web
npm install
npm run dev
```

The frontend starts on `http://localhost:5173` and proxies `/api` to `localhost:1800`.

Open `http://localhost:5173` in your browser.

## Local Development with ngrok

For HashNut backend to send payment notifications to your local machine, use ngrok:

### 1. Start ngrok

```bash
ngrok http 1800
```

This gives you a public URL like `https://xxxxx.ngrok-free.dev`.

### 2. Configure HashNut API Key

In your HashNut merchant dashboard, set:

| Field | Local Development | Production |
|-------|-------------------|------------|
| notifyURL | `https://xxxxx.ngrok-free.dev/api/notify` | `https://your-domain.com/api/notify` |
| callbackURL | `http://localhost:5173/payment-result` | `https://your-domain.com/payment-result` |

- **notifyURL**: Backend webhook — must be publicly accessible, use ngrok URL
- **callbackURL**: Frontend redirect after payment — use `localhost:5173` for local dev (browser redirect, no public access needed)

### 3. Payment Flow (Local Dev)

```
Browser (localhost:5173)
  -> Click "Pay with Crypto"
  -> POST /api/orders (proxied to localhost:1800)
  -> Redirect to HashNut payment page (defi.hashnut.io/pay)
  -> User pays on-chain
  -> HashNut backend sends notification to ngrok URL -> localhost:1800/api/notify
  -> HashNut frontend redirects to http://localhost:5173/payment-result?state=4&...
  -> Frontend shows payment success
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/products` | List all products |
| GET | `/api/chains` | List supported chains + coins (from DB) |
| POST | `/api/orders` | Create order `{productId, chainCode, coinCode}` |
| GET | `/api/orders/:id` | Query order status |
| POST | `/api/orders/:id/confirm` | Submit payment tx hash `{payTxId}` |
| POST | `/api/notify` | HashNut payment result webhook |

## Database Schema

| Table | Description |
|-------|-------------|
| `t_coin_info` | Supported chain + coin configurations |
| `t_hashnut_api_key` | Splitter address + API credentials per chain |
| `products` | Demo products (price only, no chain/coin binding) |
| `orders` | Orders with user-selected chain + coin |

## Project Structure

```
hashnut-demo/
├── pom.xml
├── migrate.sql                                          # Database schema + seed data
└── src/main/
    ├── resources/application.yml                        # Runtime configuration
    └── java/io/hashnut/demo/
        ├── DemoApplication.java                         # Spring Boot entry point
        ├── config/
        │   ├── HashNutConfig.java                       # SDK client factory (cached per secretKey) + payUrl builder
        │   └── CorsConfig.java                          # CORS support
        ├── model/
        │   ├── Product.java
        │   └── Order.java
        └── controller/
            ├── ProductController.java                   # GET /api/products, GET /api/chains
            ├── OrderController.java                     # POST /api/orders, GET /api/orders/{id}, POST /api/orders/{id}/confirm
            └── NotifyController.java                    # POST /api/notify
```

## License

MIT
