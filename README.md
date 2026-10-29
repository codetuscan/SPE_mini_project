# SPE Mini Project — Calculator

A simple calculator REST API in plain Java (no framework), with a CI/CD pipeline (Jenkins, Docker, Docker Compose).

## Requirements

- Java 21
- Maven 3.8+

## Build and test

```bash
mvn clean package   # compile, test and build target/calculator.jar
mvn test            # run JUnit tests (reports in target/surefire-reports)
```

## Run

```bash
java -jar target/calculator.jar            # listens on port 8080
PORT=9090 java -jar target/calculator.jar  # custom port
```

## API

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/add?a=&b=` | a + b |
| GET | `/api/v1/subtract?a=&b=` | a - b |
| GET | `/api/v1/multiply?a=&b=` | a * b |
| GET | `/api/v1/divide?a=&b=` | a / b |
| GET | `/actuator/health` | health check |

Examples:

```bash
curl "localhost:8080/api/v1/add?a=2&b=3"
# {"operation":"add","a":2,"b":3,"result":5}

curl -i "localhost:8080/api/v1/divide?a=5&b=0"
# HTTP/1.1 400 Bad Request
# {"error":"Division by zero"}

curl "localhost:8080/actuator/health"
# {"status":"UP"}
```

Missing or non-numeric `a` / `b` return `400` with `{"error":"..."}`. Unknown paths return `404`, and non-GET requests return `405`.

## Docker

Multi-stage build (Maven/JDK build stage, slim JRE runtime, non-root user):

```bash
docker build -t codetuscan/calculator .
docker run -p 8080:8080 codetuscan/calculator
curl "localhost:8080/api/v1/add?a=2&b=3"
```

## Docker Compose

Starts the calculator (with a health check on `/actuator/health`) behind an Nginx reverse proxy:

```bash
docker compose up -d --build
curl "localhost/api/v1/add?a=2&b=3"   # served through Nginx on port 80
docker compose ps
docker compose down
```

Use `HOST_PORT=8082 docker compose up -d` if port 80 is busy, and `TAG=<tag>` to pick an image tag.

## CI/CD (Jenkins)

The [Jenkinsfile](Jenkinsfile) runs on every push to GitHub (webhook, with Poll SCM as a fallback):

1. Checkout
2. Build: `mvn clean package -DskipTests`
3. Test: `mvn test`, JUnit results published
4. Docker Build: image tagged with the build number and `latest`
5. Push: to Docker Hub (`codetuscan/calculator`) using the Jenkins credential `dockerhub-creds`
6. Deploy: `docker compose up -d`, then waits for `/actuator/health`

A build status email is sent on success or failure.

Checking webhook