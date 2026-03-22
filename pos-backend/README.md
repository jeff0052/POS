# POS Backend

Spring Boot backend skeleton for the POS project.

## Included

- Spring Boot Maven project
- Unified API response wrapper
- CORS config
- JPA + MySQL configuration
- Database-backed entities and repositories for:
  - store
  - store settings
  - categories
  - products
  - orders
- Controllers and services for:
  - auth
  - store
  - categories
  - products
  - orders
  - reports

## Current status

This is now a database-ready skeleton aligned to the frontend service layer.
Auth and report are still mock/simple, while store/category/product/order are prepared to read from MySQL.
It is intended to be the next step before adding:

1. real persistence
2. security
3. payment/refund/print modules
4. write operations and validations

## Next recommended steps

1. Add payment APIs
2. Add refund APIs
3. Add print record APIs
4. Add order items, payment records, refund records entities
5. Add Flyway or Liquibase for schema management
