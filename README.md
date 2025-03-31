#  🐘  PostgreSQL & pgAdmin Setup with Docker 🐳

This guide helps you set up **PostgreSQL** 🐘 and **pgAdmin** using **Docker Compose** and connect to them easily.

---

## 🛠️ Setup & Running the Containers

### 1️⃣ Start PostgreSQL 🐘 & pgAdmin Containers 🖥️
Run the following command to bring up the containers:
```sh
docker compose --env-file src/main/resources/docker/.env up -d
```

### 2️⃣ Verify Running Containers 🔍
```sh
docker ps
```
Expected Output:
```
CONTAINER ID   IMAGE                   COMMAND                  CREATED         STATUS         PORTS                                     NAMES
82cf91576175   postgres:16.1           "docker-entrypoint.s…"   X seconds ago  Up X seconds   0.0.0.0:5432->5432/tcp                    postgresdb
003709f7dc1e   dpage/pgadmin4:latest   "/entrypoint.sh"         X seconds ago  Up X seconds   80/tcp, 443/tcp, 0.0.0.0:7000->6000/tcp   pgadmin
```

### 3️⃣ Open pgAdmin 🌐
📌 Open **http://localhost:7000/browser/** in your browser.

---

## 🔗 Connecting pgAdmin to PostgreSQL 🐘

### **4️⃣ Add a New Server in pgAdmin**
1. Click **Servers** → **Create** → **Server**
2. Go to the **General** tab → Enter **any name** (e.g., `Postgres Server`)
3. Go to the **Connection** tab:
   - **Host Name / Address**:
     - 💪 **If using Docker Desktop:** Use `postgresdb` (Service Name)
     - 🌍 **If using a remote server:** Use the **server’s IP address**
     - 🛠️ **To find container IP manually**, run:
       ```sh
       docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' postgresdb
       ```
   - **Port**: `5432`
   - **Username**: `admin` (from `.env` file)
   - **Password**: `admin` (from `.env` file)

4. Click **Save** 🎉

---

## 🛠️ Useful Docker Commands 🐳

### 🚀 Start Containers
```sh
docker compose --env-file src/main/resources/docker/.env up -d
```

### 🛑 Stop Containers
```sh
docker compose down
```

### 🧹 Stop and Remove Containers with Volumes
```sh
docker compose down --volumes
```

### 📜 Check Running Containers
```sh
docker ps
```

### 🔍 Check All Containers (Running & Stopped)
```sh
docker ps -a
```

### 📂 Check Docker Volumes
```sh
docker volume ls
```

### 🔄 Restart Containers
```sh
docker compose restart
```

### 🚀 Start a Specific Service
```sh
docker compose up -d postgresdb  # or pgadmin
```

### 🛠️ View Docker Logs
```sh
docker logs -f postgresdb  # Check logs for PostgreSQL
```
```sh
docker logs -f pgadmin  # Check logs for pgAdmin
```

### 🗑️ Remove Unused Docker Data
```sh
docker system prune -a
```

### 🏗️ Build Containers Without Using Cache
```sh
docker compose build --no-cache
```

### 🔧 Enter PostgreSQL Container
```sh
docker exec -it postgresdb bash
```

### 🛠️ Connect to PostgreSQL Inside Container 🐘
```sh
docker exec -it postgresdb psql -U admin -d securedoc
```

### 🔄 Restart Docker Service (If Needed)
```sh
sudo systemctl restart docker  # Linux/macOS
```
```sh
wsl --shutdown  # Windows (For WSL Users)
```

---

## 🛠️ Troubleshooting 🛑

### 🔄 Restart the Containers
```sh
docker compose down --volumes
```
```sh
docker compose --env-file src/main/resources/docker/.env up -d
```

### 🔍 Check Logs
```sh
docker logs postgresdb
```
```sh
docker logs pgadmin
```

### ❌ Remove and Recreate Containers
```sh
docker compose down --volumes
```
```sh
docker system prune -a
```
```sh
docker compose --env-file src/main/resources/docker/.env up -d
```

### 📺 Check PostgreSQL Connection
```sh
docker exec -it postgresdb psql -U admin -d securedoc
```
```sql
\l  -- List all databases
\dt -- List tables
```

---

## 🎯 Notes 🔐
- 🖥️ If deploying on a **remote server**, replace `localhost` with the **server’s IP**.
- 💡 **Host Name in pgAdmin**:
  - If using **Docker Compose**, use the **service name** (`postgresdb`).
  - If using a **remote server**, use the **server’s IP address**.
- 🔄 If `pgAdmin` fails to connect, check if PostgreSQL is running with:
  ```sh
  docker ps
  ```
- 🔐 Always **change default passwords** for security!

Happy Coding! 🚀

