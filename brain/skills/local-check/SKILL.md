---
name: local-check
description: Confirm that the Financial Ledger project builds and passes checks on a team member's local machine.
---

# Local Check Skill

## Purpose

The `local-check` skill (SE-1, SK-02 in SRS v1.4) standardizes verification on a team member's local environment. It verifies that the project builds cleanly, automated unit/integration tests pass, the application can start against an isolated temporary database, and the health endpoint returns `UP`.

## Procedure Steps

1. **Step 1: Build the Project**
   - Execute Maven validation and compilation:
     ```bash
     mvn clean compile --batch-mode
     ```
   - *Expected outcome*: Exit code 0, all classes compiled without errors.

2. **Step 2: Run Automated Test Suite**
   - Execute unit and integration tests against an isolated database:
     ```bash
     mvn test --batch-mode
     ```
   - *Expected outcome*: All tests pass (0 failures, 0 errors).

3. **Step 3: Start Application with Temporary Database**
   - Launch Spring Boot application using a clean temporary SQLite database:
     ```bash
     mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:sqlite:target/local-check.db"
     ```
   - *Expected outcome*: Application starts and reaches ready state within 60 seconds.

4. **Step 4: Invoke Health Endpoint**
   - Query the read-only health check:
     ```bash
     curl -s http://localhost:8080/actuator/health
     ```
   - *Expected outcome*: HTTP 200 with JSON payload `{"status":"UP"}`.

5. **Step 5: Report & Clean Up**
   - Report PASS/FAIL status for each step.
   - Terminate the background application process.
   - Remove temporary database files (`target/local-check.db*`).
   - Stop immediately at the first failure and mark subsequent steps as `NOT RUN`.

## Invariants

- Must never modify persistent production or reference databases.
- Must clean up all spawned processes and temporary test files upon completion.
