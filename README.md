# 🛡️ reconciliation-core: The Self-Healing Reconciler

The `reconciliation-core` is to resolve transactions stuck in an **UNCERTAIN** state due to network timeouts or bank delays.

## 🚀 Role in the Ecosystem
1. **Detection:** Scans the shared database for transactions with `UNCERTAIN` status.
2. **Safety:** Implements a 30-second "cool-down" buffer to avoid race conditions with live traffic.
3. **Inquiry:** Calls the Bank's Status Inquiry API to find the ground truth.
4. **Resolution:** Updates the transaction to a final state (`AUTHORIZED` or `FAILED`).

## 🛠️ Technical Stack
- **Java 21** (Virtual Threads ready)
- **Spring Boot 3.4.1**
- **Spring Data JPA** (Shared persistence)
- **MockitoBean** (Modernized unit testing)

## 📦 How to Build & Run

### Local Build
```bash
mvn clean install
mvn test
```

### Run with Docker

```bash
docker build -t reconciliation-core .
docker run -p 8082:8082 reconciliation-core
```

## 🧪 Testing the Logic
The logic is fully verified via EventCoreApplicationTests, which covers:

1. Success Scenarios: Moving records from Uncertain to Authorized.

2. Safety Buffer: Ensuring "fresh" records are ignored.

3. Persistence: Ensuring records remain in Uncertain if the bank is still processing.


## 🔗 Configuration
Important properties in application.properties:
1. reconciliation.interval: Frequency of the cleanup cycle (default 30s).
2. spring.datasource.url: Must point to the same DB as resilience-core.
