# Social API Deployment Context (Azure VM)

## Why local bootRun differs from full Docker

### Local (bootRun)
- Infrastructure (Postgres, RabbitMQ, pgAdmin) runs in Docker.
- Spring Boot runs on the host with `./gradlew.bat bootRun`.
- Connectivity uses localhost-based defaults in application.yml, for example:
  - `SOCIAL_DB_URL` default points to `localhost:${SOCIAL_DB_PORT}`.
  - `SOCIAL_RABBIT_HOST` default is `localhost`.

### Azure VM (full Docker)
- Everything runs inside Docker.
- The API container connects to other containers by service name over the Docker network.
- The API container includes JDK 17 and enforces memory limits via JVM flags.
- Docker Compose manages startup order and service health checks.

## Required .env variables

These values are used by Docker Compose and Spring Boot configuration.

### Core application and port settings
- `SOCIAL_APP_PORT=8083`
- `SOCIAL_CORS_ALLOWED_ORIGINS=http://<frontend-host>`

### Postgres settings
- `SOCIAL_DB_NAME=social_features`
- `SOCIAL_DB_USER=social`
- `SOCIAL_DB_PASSWORD=changeme`
- `SOCIAL_DB_PORT=5435`
- `SOCIAL_DB_HOST=postgres`

Notes:
- `SOCIAL_DB_HOST` is used by Docker Compose to build the JDBC URL inside the API container.
- The API service sets `SOCIAL_DB_URL` using `SOCIAL_DB_HOST`, so the container uses `postgres` (the service name) instead of `localhost`.

### RabbitMQ settings
- `SOCIAL_RABBIT_USER=guest`
- `SOCIAL_RABBIT_PASSWORD=guest`
- `SOCIAL_RABBIT_PORT=5674`
- `SOCIAL_RABBIT_MGMT_PORT=15674`
- `SOCIAL_STOMP_RELAY_PORT=61613`
- `SOCIAL_RABBIT_WEB_STOMP_PORT=15675`

Notes:
- The API service also sets `SOCIAL_RABBIT_HOST=rabbitmq` and `SOCIAL_STOMP_RELAY_HOST=rabbitmq` in Docker Compose, so the container uses service-name routing.

### JWT validation
- `SOCIAL_JWT_PUBLIC_KEY=<base64-x509-rsa-public-key>`
- `SOCIAL_JWT_ISSUER=mhsa-auth`
- `SOCIAL_JWT_AUDIENCE=mhsa-api`
- `SOCIAL_JWT_SIGNING_KID=`

### Optional pgAdmin
- `SOCIAL_PGADMIN_EMAIL=admin@social.local`
- `SOCIAL_PGADMIN_PASSWORD=admin`
- `SOCIAL_PGADMIN_PORT=5052`

## Azure VM deployment steps

### 1) Install prerequisites
```bash
sudo apt-get update
sudo apt-get install -y git ca-certificates curl

# Docker
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo $VERSION_CODENAME) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo usermod -aG docker $USER
newgrp docker
```

### 2) Clone the repository
```bash
git clone <REPO_URL>
cd thesis_social
```

### 3) Configure environment
```bash
cp .env.example .env
nano .env
```

Minimum edits for Docker networking:
```
SOCIAL_DB_HOST=postgres
SOCIAL_RABBIT_HOST=rabbitmq
SOCIAL_STOMP_RELAY_HOST=rabbitmq
```

### 4) Build and start all services
```bash
docker compose up -d --build
```

### 5) Verify
```bash
docker compose ps
```

The API should be available on:
```
http://<vm-public-ip>:${SOCIAL_APP_PORT}
```
