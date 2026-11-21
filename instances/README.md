# Load Balancer Instances

This repository contains two Spring Boot services that can run locally as Java processes or as Docker containers.

| Service | Project folder | Default port |
| --- | --- | ---: |
| Light Task Server | `Light_Task_Server` | 8080 |
| Heavy Task Server | `Heavy_Task_Server` | 8081 |

## Requirements

For the Windows and Linux scripts:

- Java 17 or later
- Maven is not required; each project includes the Maven Wrapper

For Docker:

- Docker Desktop on Windows or Docker Engine on Linux
- Docker Compose plugin (`docker compose`)

## Run locally on Windows

From the repository root, double-click `run-windows.bat` or run this in Command Prompt:

```bat
run-windows.bat
```

The script packages both services and starts each in its own console window. Leave those windows open while using the services. Press `Ctrl+C` or close a window to stop its service.

## Run locally on Linux

From the repository root, run:

```bash
chmod +x run-linux.sh
./run-linux.sh
```

The script packages both services, starts them in the background, and writes logs to `light-instance.log` and `heavy-instance.log`. It prints the process IDs; stop the services with `kill <PID>`.

## Run the full stack with Docker Compose

The root `docker-compose.yml` builds and starts both backend services and the load balancer. Run commands from the repository root:

```bash
docker compose up --build
```

The load balancer is available at `http://localhost:9090`. The backend containers are reachable by the load balancer over the Compose network and are not published directly on the host.

Common Docker commands:

```bash
# Start in the background
docker compose up --build -d

# Follow container logs
docker compose logs -f

# Stop and remove the containers
docker compose down
```

## Check the services locally

| Service | Health check | Example endpoint |
| --- | --- | --- |
| Light Task Server (local process) | `http://localhost:8080/health` | `http://localhost:8080/hello` |
| Heavy Task Server (local process) | `http://localhost:8081/health` | `http://localhost:8081/hello` |

## Run more instances without copying project folders

You can run multiple Java processes from the same built JAR. Give every process a unique port and instance name. For example, start a second Light Task Server on port 8082:

**Windows Command Prompt:**

```bat
start "Light_Task_Server_2" java -jar "Light_Task_Server\target\instance-1-0.0.1-SNAPSHOT.jar" --server.port=8082 --instance.name=Light_Task_Server_2
```

**Linux:**

```bash
java -jar Light_Task_Server/target/instance-1-0.0.1-SNAPSHOT.jar --server.port=8082 --instance.name=Light_Task_Server_2 > light-instance-2.log 2>&1 &
```

For another Heavy Task Server, use `Heavy_Task_Server/target/instance-2-0.0.1-SNAPSHOT.jar`, a different unused port (for example, 8083), and a distinct `--instance.name`. Spring Boot command-line options override the defaults in `application.properties`.

To include additional Java processes in automatic startup, add matching launch commands to `run-windows.bat` and `run-linux.sh`.

## Add more Docker instances

Docker Compose can build another container from either existing project folder. Add a service to the root `docker-compose.yml`; do not copy the project folder. For example, to run a second Light Task Server, add:

```yaml
  light-task-server-2:
    build:
      context: ./instances
      dockerfile: Dockerfile
      args:
        PROJECT_DIR: Light_Task_Server
        ARTIFACT_ID: instance-1
    expose:
      - "8080"
    command: ["--instance.name=Light_Task_Server_2"]
```

Then rebuild and start it:

```bash
docker compose up --build -d light-task-server-2
```

Use a unique Compose service name and instance name for each additional container. To send load-balanced requests to it, also add its URL to the load balancer configuration.
