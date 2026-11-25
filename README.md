# SupportDesk

Customer support and ticket management SaaS (Java 21, Spring Boot, MySQL, vanilla JS).

## Current status
Milestone 0: project setup (schema, error handling, health check).

## Quick start
1. Create the MySQL database and user (see below).
2. Copy `.env.example` to `.env` and set `DB_PASSWORD`.
3. In VS Code press F5 and choose "SupportDesk (dev)", or run `mvn spring-boot:run`
   with `SPRING_PROFILES_ACTIVE=dev` and `DB_PASSWORD` set in your terminal.
4. Open http://localhost:8080/api/system/health

## MySQL setup
```sql
CREATE DATABASE supportdesk CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'supportdesk_app'@'localhost' IDENTIFIED BY 'choose-a-strong-password';
GRANT ALL PRIVILEGES ON supportdesk.* TO 'supportdesk_app'@'localhost';
FLUSH PRIVILEGES;
```
