# Java Load Balancer

A small Spring Boot HTTP load balancer with two sample backend services. It forwards incoming `GET` requests to healthy backends using round-robin or weighted round-robin selection, and retries another backend when a request fails.

## Project layout

| Path | Purpose | Default port |
| --- | --- | ---: |
| `java-load-balancer/` | Load balancer application | 9090 |
| `instances/Light_Task_Server/` | Sample light-task backend | 8080 |
| `instances/Heavy_Task_Server/` | Sample heavy-task backend | 8081 |
| `instances/run-windows.bat` | Build and launch the backends on Windows | — |
| `instances/run-linux.sh` | Build and launch the backends on Linux | — |
| `docker-compose.yml` | Docker Compose setup for the load balancer and both backends | — |

## Requirements

- Java 21 or later to build and run the Spring Boot applications.
- The included Maven Wrapper is used for local builds; a separate Maven installation is not required.
- Docker Engine or Docker Desktop and the Docker Compose plugin are only needed for the container option.

The first Maven build may need an internet connection to download dependencies. Docker builds also download their base images.

## Run the full project locally

Start the two backend services first, then start the load balancer. Each service can be run in a separate terminal from the repository root.

### 1. Build and start the backends

On Windows, run:

```bat
instances\run-windows.bat
```

This builds both backend projects and opens separate windows for the Light Task Server on port `8080`, a second Light Task Server on port `8082`, and the Heavy Task Server on port `8081`. Keep those windows open while using the load balancer.

On Linux or macOS, run:

```bash
cd instances
chmod +x run-linux.sh
./run-linux.sh
```

This builds and starts the Light Task Server on port `8080` and Heavy Task Server on port `8081` in the background. Logs are written to `instances/light-instance.log` and `instances/heavy-instance.log`. The script prints their process IDs; stop them with `kill <PID>`.

### 2. Start the load balancer

In a new terminal from the repository root:

**Windows:**

```bat
cd java-load-balancer
mvnw.cmd spring-boot:run
```

**Linux or macOS:**

```bash
cd java-load-balancer
./mvnw spring-boot:run
```

The load balancer listens on `http://localhost:9090`. To build a runnable JAR instead, use `mvnw.cmd package` on Windows or `./mvnw package` on Linux/macOS, then run `java -jar target/java-load-balancer-0.0.1-SNAPSHOT.jar` from `java-load-balancer/`.

## Make requests

The load balancer forwards the requested path to a backend. For example:

```bash
curl http://localhost:9090/hi
curl http://localhost:9090/health
```

`/hi` returns the name of the backend that handled the request. `/health` is forwarded to a backend; it is not a health endpoint for the load balancer itself. When running the backends as local processes, you can check them directly at `http://localhost:8080/health` and `http://localhost:8081/health`.

The Light Task Server's `/hello` endpoint deliberately waits 100 seconds before responding. The load balancer's backend read timeout is 2 seconds, so `/hello` will time out and trigger a retry rather than return a normal response. The Heavy Task Server's `/hello` endpoint responds immediately.

## Run everything with Docker Compose

From the repository root, build and start the load balancer and both backends together:

```bash
docker compose up --build
```

The load balancer is available at `http://localhost:9090`. The backend containers communicate with it over the Compose network, so their ports are not published on the host. To start in the background, add `-d`. View logs with `docker compose logs -f` and stop the stack with `docker compose down`.

The backend URLs can be overridden with `LIGHT_SERVER_URL` and `HEAVY_SERVER_URL` if you run the load balancer outside Compose.

## Configuration

Load balancer defaults are in [`java-load-balancer/src/main/resources/application.properties`](java-load-balancer/src/main/resources/application.properties):

| Setting | Default | Description |
| --- | --- | --- |
| `server.port` | `9090` | Port for the load balancer HTTP server. |
| `load-balancer.algorithm` | `WEIGHTED_ROUND_ROBIN` | Backend selection strategy. Accepts `ROUND_ROBIN` or `WEIGHTED_ROUND_ROBIN`. |
| `load-balancer.max-attempts` | `2` | Maximum backend attempts for one request; limited to the number of currently available backends. |

Override a setting at startup with a Spring Boot command-line option. For example:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=9091 --load-balancer.algorithm=ROUND_ROBIN"
```

The backend URL defaults are Light Task Server (`http://localhost:8080`, weight `3`) and Heavy Task Server (`http://localhost:8081`, weight `1`). Override them with `LIGHT_SERVER_URL` and `HEAVY_SERVER_URL`. With weighted round robin and both backends available, requests are distributed in a 3:1 pattern over time. The Windows script also starts a second light backend on port `8082`, but the load balancer does not include it in its configured backend list by default.

## Behavior and limitations

- The controller forwards requests as HTTP `GET` requests and returns the backend response as plain text. Request bodies, original HTTP methods, and request headers are not forwarded.
- Backends are initially assumed healthy and health is refreshed every 30 minutes. A failed request also affects its circuit breaker.
- Each backend circuit breaker opens after three recorded request failures and permits requests again after 30 seconds.
- Backend connections time out after 1 second and reads time out after 2 seconds.
- The sample services and the load balancer are configured for local development. Update the backend URLs and deployment configuration before using them in another environment.

## Source map

- `java-load-balancer/src/main/java/com/LB/controller/` — incoming request controller.
- `java-load-balancer/src/main/java/com/LB/service/` — request forwarding and backend health checks.
- `java-load-balancer/src/main/java/com/LB/loadbalancer/strategy/` — round-robin strategies.
- `java-load-balancer/src/main/java/com/LB/circuitbreaker/` — per-backend failure circuit breaker.
- `instances/Light_Task_Server/src/` and `instances/Heavy_Task_Server/src/` — sample backend implementations.
