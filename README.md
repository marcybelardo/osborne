# Osborne
## A fun and simple budget manager

### Running
```bash
# Build and start
docker compose up --build

# run in background
docker compose up --build -d

# tear down (keeps DB)
docker compose down

#tear down and wipe database
docker compose down -v
```

### Running locally
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
