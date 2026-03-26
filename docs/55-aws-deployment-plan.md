# AWS Deployment Plan — FounderPOS

## Target

| 项 | 值 |
|----|-----|
| IP | 54.237.230.5 |
| OS | RHEL 10.1 (x86_64) |
| CPU | 2 vCPU |
| RAM | 3.6 GB |
| Disk | 100 GB |
| SSH Key | ~/.ssh/founderPOS-aws.pem |
| SSH User | ec2-user |

## Architecture

```
Internet
   │
   ▼ :80 / :443
┌──────────┐
│  Nginx   │ ← 反向代理 + SSL (later)
└────┬─────┘
     │
     ├── /           → POS Frontend   (Docker :5188)
     ├── /admin      → Admin Frontend (Docker :5187)
     ├── /qr         → QR Frontend    (Docker :4183)
     └── /api/*      → Backend        (Docker :8080)
```

所有服务跑在 Docker 里，Nginx 在宿主机做反向代理。MySQL 也跑在 Docker 里（和本地开发一致）。

## Execution Steps

### Phase 1: System Setup (5 min)

```
1.1 更新系统包
    sudo dnf update -y

1.2 安装基础工具
    sudo dnf install -y git curl wget vim unzip tar

1.3 配置防火墙
    sudo firewall-cmd --permanent --add-service=http
    sudo firewall-cmd --permanent --add-service=https
    sudo firewall-cmd --permanent --add-port=8080/tcp  (临时，调试用)
    sudo firewall-cmd --reload

1.4 配置 SELinux (允许 Nginx 反代)
    sudo setsebool -P httpd_can_network_connect 1
```

### Phase 2: Docker (3 min)

```
2.1 安装 Docker
    sudo dnf install -y docker
    sudo systemctl enable --now docker
    sudo usermod -aG docker ec2-user

2.2 安装 Docker Compose
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
      -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose

2.3 验证
    docker --version
    docker-compose --version
```

### Phase 3: Nginx (3 min)

```
3.1 安装 Nginx
    sudo dnf install -y nginx
    sudo systemctl enable --now nginx

3.2 写反向代理配置
    /etc/nginx/conf.d/founderpos.conf:

    upstream backend {
        server 127.0.0.1:8080;
    }
    upstream pos_frontend {
        server 127.0.0.1:5188;
    }
    upstream admin_frontend {
        server 127.0.0.1:5187;
    }
    upstream qr_frontend {
        server 127.0.0.1:4183;
    }

    server {
        listen 80;
        server_name 54.237.230.5;

        # API
        location /api/ {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Actuator health
        location /actuator/ {
            proxy_pass http://backend;
        }

        # POS Frontend (default)
        location / {
            proxy_pass http://pos_frontend;
            proxy_set_header Host $host;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
        }

        # Admin Frontend
        location /admin/ {
            proxy_pass http://admin_frontend/;
            proxy_set_header Host $host;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
        }

        # QR Frontend
        location /qr/ {
            proxy_pass http://qr_frontend/;
            proxy_set_header Host $host;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
        }
    }

3.3 测试并重载
    sudo nginx -t
    sudo systemctl reload nginx
```

### Phase 4: Clone Repo + Build (10 min)

```
4.1 Clone
    cd /home/ec2-user
    git clone https://github.com/jeff0052/POS.git founderpos
    cd founderpos
    git checkout codex/reservations-transfer-backend

4.2 创建生产 docker-compose
    /home/ec2-user/founderpos/docker-compose.prod.yml:

    services:
      mysql:
        image: mysql:8.4
        container_name: pos-mysql
        restart: always
        environment:
          MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
          MYSQL_DATABASE: pos_v2_db
        ports:
          - "127.0.0.1:3306:3306"    # 只绑本机，不暴露外网
        volumes:
          - mysql_data:/var/lib/mysql
        healthcheck:
          test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p${MYSQL_ROOT_PASSWORD}"]
          interval: 10s
          timeout: 5s
          retries: 10
        command:
          - --character-set-server=utf8mb4
          - --collation-server=utf8mb4_unicode_ci
          - --max-connections=200
          - --innodb-buffer-pool-size=512M
          - --slow-query-log=1
          - --slow-query-log-file=/var/log/mysql/slow.log
          - --long-query-time=2

      pos-backend:
        build:
          context: ./pos-backend
        container_name: pos-backend
        restart: always
        depends_on:
          mysql:
            condition: service_healthy
        environment:
          SPRING_PROFILES_ACTIVE: mysql
          SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/pos_v2_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Singapore&characterEncoding=utf8mb4
          SPRING_DATASOURCE_USERNAME: pos_app
          SPRING_DATASOURCE_PASSWORD: ${POS_APP_PASSWORD}
          VIBECASH_API_URL: ${VIBECASH_API_URL:-https://api.vibecash.dev}
          VIBECASH_SECRET: ${VIBECASH_SECRET:-}
          VIBECASH_WEBHOOK_SECRET: ${VIBECASH_WEBHOOK_SECRET:-}
          VIBECASH_CURRENCY: SGD
          JAVA_OPTS: "-Xmx512m -Xms256m"
        ports:
          - "127.0.0.1:8080:8080"

      pos-frontend:
        build:
          context: ./android-preview-web
          dockerfile: Dockerfile
        container_name: pos-frontend
        restart: always
        ports:
          - "127.0.0.1:5188:5188"

      admin-frontend:
        build:
          context: ./pc-admin
          dockerfile: Dockerfile
        container_name: admin-frontend
        restart: always
        ports:
          - "127.0.0.1:5187:5187"

      qr-frontend:
        build:
          context: ./qr-ordering-web
          dockerfile: Dockerfile
        container_name: qr-frontend
        restart: always
        ports:
          - "127.0.0.1:4183:4183"

    volumes:
      mysql_data:

4.3 创建 .env 文件
    /home/ec2-user/founderpos/.env:

    MYSQL_ROOT_PASSWORD=<strong-random-password>
    POS_APP_PASSWORD=<strong-random-password>

4.4 前端需要 Dockerfile（如果没有的话）
    每个前端创建 Dockerfile:

    FROM node:20-alpine AS build
    WORKDIR /app
    COPY package*.json ./
    RUN npm ci
    COPY . .
    RUN npm run build

    FROM nginx:alpine
    COPY --from=build /app/dist /usr/share/nginx/html
    COPY nginx.conf /etc/nginx/conf.d/default.conf
    EXPOSE <port>

    + 对应的 nginx.conf 处理 SPA routing
```

### Phase 5: MySQL 安全加固 (3 min)

```
5.1 启动后创建应用专用用户（不用 root）
    docker exec -i pos-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} <<SQL
    CREATE USER 'pos_app'@'%' IDENTIFIED BY '${POS_APP_PASSWORD}';
    GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
      ON pos_v2_db.* TO 'pos_app'@'%';
    FLUSH PRIVILEGES;
    SQL

5.2 禁止 root 远程登录
    docker exec -i pos-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} <<SQL
    DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost', '127.0.0.1', '::1');
    FLUSH PRIVILEGES;
    SQL

5.3 MySQL 端口只绑 127.0.0.1（docker-compose 已配置）
    不暴露 3306 到外网

5.4 启用慢查询日志（docker-compose command 已配置）
```

### Phase 6: Build & Start (10 min)

```
6.1 构建所有镜像
    docker-compose -f docker-compose.prod.yml build

6.2 启动
    docker-compose -f docker-compose.prod.yml up -d

6.3 等待 MySQL 健康 + Flyway 迁移
    docker-compose -f docker-compose.prod.yml logs -f pos-backend (等看到 Started)

6.4 创建 MySQL 应用用户（Phase 5.1）

6.5 重启后端用新用户
    docker-compose -f docker-compose.prod.yml restart pos-backend
```

### Phase 7: 验证 (2 min)

```
7.1 健康检查
    curl http://54.237.230.5/api/actuator/health

7.2 API 测试
    curl http://54.237.230.5/api/v2/mcp/tools
    curl http://54.237.230.5/api/v2/ai/advisors

7.3 前端页面
    curl -s -o /dev/null -w "%{http_code}" http://54.237.230.5/
    curl -s -o /dev/null -w "%{http_code}" http://54.237.230.5/admin/
    curl -s -o /dev/null -w "%{http_code}" http://54.237.230.5/qr/

7.4 Nginx 日志
    sudo tail /var/log/nginx/access.log
```

### Phase 8: 生产加固 (可选，后续)

```
8.1 SSL 证书 (Let's Encrypt)
    sudo dnf install -y certbot python3-certbot-nginx
    sudo certbot --nginx -d yourdomain.com

8.2 自动重启
    docker-compose.prod.yml 已配 restart: always
    systemctl enable docker（已配）

8.3 日志轮转
    docker 默认用 json-file driver，配置 max-size

8.4 备份
    cron job: mysqldump 每日备份到 S3

8.5 监控
    配置 /actuator/health 的 uptime 监控
```

## 风险点

| 风险 | 缓解 |
|------|------|
| 3.6GB 内存偏小 | Java -Xmx512m + MySQL innodb-buffer-pool 512M，留余量 |
| 前端可能没有 Dockerfile | 需要为 3 个前端各写一个 |
| Flyway 首次跑可能有 checksum 问题 | 用 validate-on-migrate: false（已配） |
| RHEL 10 的 Docker 包名可能不同 | 可能需要用 podman 或手动装 Docker CE |
| 没有域名，只有 IP | SSL 后续配 |

## 预估时间

| Phase | 时间 |
|-------|------|
| 1-3 系统+Docker+Nginx | 10 min |
| 4 Clone+配置 | 5 min |
| 5 MySQL 加固 | 3 min |
| 6 Build（Maven + npm） | 10-15 min |
| 7 验证 | 2 min |
| **Total** | **~35 min** |
