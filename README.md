# 企業智慧維運 AI Agent

F01 建立本機可啟動、可測試並由 GitHub Actions 驗證的 Spring Boot 專案骨架。此階段只提供健康檢查，不包含業務功能。

## 環境需求

- JDK 21
- Docker Desktop，並啟動 Docker Engine
- Git

Maven 由 Maven Wrapper 自動下載，不需要另外安裝。

## 本機啟動

在 repo 根目錄執行：

```bash
docker compose up --build
```

Compose 會啟動 PostgreSQL 16 與 Java 21 應用程式。第一次啟動需要下載容器映像與 Maven 依賴。啟動完成後確認：

```bash
curl --fail http://localhost:8080/actuator/health
```

預期回應包含：

```json
{"status":"UP"}
```

停止服務：

```bash
docker compose down
```

本機 Compose 使用 `PLACEHOLDER` 作為資料庫密碼預設值，不可用於正式環境。需要時可在啟動前設定 `POSTGRES_PASSWORD` 環境變數。

## 測試與格式檢查

```bash
./mvnw test
./mvnw spotless:check
./mvnw spotless:apply
```

Windows PowerShell 對應命令：

```powershell
.\mvnw.cmd test
.\mvnw.cmd spotless:check
.\mvnw.cmd spotless:apply
```

CI 會在每個 Pull Request 執行 `./mvnw --batch-mode spotless:check verify`。

## 目錄

```text
src/main/java/      Spring Boot 應用程式
src/main/resources/ 應用程式設定
src/test/java/      自動化測試
.github/workflows/  Pull Request CI
```
