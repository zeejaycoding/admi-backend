# Database Migrations - Liquibase

This directory contains all database migrations for the Powercity International Platform using Liquibase.

## Directory Structure

```
db/changelog/
├── README.md                          # This file
├── db.changelog-master.yaml           # Master changelog (entry point)
├── migrations/                        # XML migration definitions
│   ├── 000-initial-schema.xml        # Initial schema creation
│   ├── 001-drop-unnecessary-book-columns.xml
│   └── 002-create-cart-tables-and-add-ngn-price.xml
└── sql/                              # SQL scripts referenced by migrations
    ├── 000-initial-schema.sql        # Complete initial schema
    └── 003-add-performance-indexes.sql
```

## Migration Philosophy

### SQL Files vs XML
We use **separate SQL files** for all schema definitions and complex queries, referenced from XML files. This approach provides:

✅ **Better Readability** - SQL is easier to read in .sql files than embedded in XML
✅ **Version Control** - SQL diffs are cleaner and more meaningful
✅ **IDE Support** - Better syntax highlighting and code completion
✅ **Reusability** - SQL files can be used in documentation and testing
✅ **Team Collaboration** - Database experts can work with familiar SQL files

### Naming Convention

**Migration Files (XML):**
```
<sequence>-<description>.xml
Example: 003-add-user-preferences-table.xml
```

**SQL Scripts:**
```
<sequence>-<description>.sql
Example: 003-add-user-preferences-table.sql
```

**ChangeSet IDs:**
```
<sequence>-<substep>-<description>
Example: 003-001-create-user-preferences-table
```

## Production Standards

### 🔒 Data Integrity
- All enum values enforced with `CHECK` constraints
- Foreign keys with appropriate `CASCADE` behavior
- `NOT NULL` constraints on required fields
- Unique constraints on business keys (email, order_number, etc.)

### ⚡ Performance
- Indexes on all foreign keys
- Composite indexes for common query patterns
- Descending indexes for timestamp-based queries
- Partial indexes where appropriate (e.g., only active records)

### 📊 Audit Trail
- All tables include audit fields:
  - `created_at` - Timestamp when record was created
  - `updated_at` - Timestamp when record was last updated
  - `created_by` - User ID who created the record
  - `updated_by` - User ID who last updated the record

### 💰 Financial Data
- All monetary amounts use `DECIMAL(38,2)` for precision
- Currency stored as VARCHAR enum (USD, GBP, NGN, GHS, ZAR)
- Payment table includes comprehensive audit trail for compliance

## Schema Overview

### Core Tables

#### `users`
- User authentication and profile information
- Email-based authentication
- Regional settings and preferences
- Account security (failed attempts, lockouts)

#### `roles` & `user_roles`
- Role-based access control (RBAC)
- Many-to-many relationship via join table
- Support for USER, ADMIN, SUPER_ADMIN roles

#### `books`
- Digital product catalog (PDF + Audio books)
- Multi-currency pricing (USD, GBP, NGN, GHS, ZAR)
- Sales tracking and featured products
- S3 integration for file storage

#### `orders`
- E-commerce order management
- Support for multiple payment gateways
- Order lifecycle tracking (pending → processing → completed)
- Refund tracking

#### `order_items`
- Line items for orders
- Snapshot of product data at purchase time
- Prevents data loss if products are modified/deleted

#### `payments`
- **Production-ready payment tracking**
- Multiple payment gateway support (Stripe, PayPal, etc.)
- Comprehensive audit trail for financial compliance
- Fee tracking (gateway fees, processing fees)
- Refund management
- State transition timestamps

#### `carts` & `cart_items`
- Shopping cart before checkout
- Support for guest carts via session_id
- Multi-product support (books, events, etc.)

## Working with Migrations

### Creating a New Migration

1. **Create SQL file** in `sql/` directory:
```sql
-- sql/004-add-wishlist-table.sql
CREATE TABLE wishlist (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

CREATE INDEX idx_wishlist_user_id ON wishlist(user_id);
```

2. **Create XML migration** in `migrations/` directory:
```xml
<?xml version="1.0" encoding="utf-8"?>
<databaseChangeLog
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">

    <changeSet id="004-001-create-wishlist-table" author="your-name" failOnError="true">
        <comment>Add wishlist feature for users to save favorite books</comment>

        <preConditions onFail="MARK_RAN">
            <not><tableExists tableName="wishlist"/></not>
        </preConditions>

        <sqlFile
                path="db/changelog/sql/004-add-wishlist-table.sql"
                relativeToChangelogFile="false"
                splitStatements="true"
                stripComments="false"
                encoding="UTF-8"/>

        <rollback>
            DROP TABLE IF EXISTS wishlist CASCADE;
        </rollback>
    </changeSet>

</databaseChangeLog>
```

3. **Add to master changelog** (`db.changelog-master.yaml`):
```yaml
  - include:
      file: db/changelog/migrations/004-add-wishlist-table.xml
```

### Best Practices

#### ✅ DO
- Use `preConditions` to make migrations idempotent
- Always provide `rollback` scripts
- Use `MARK_RAN` for preconditions to skip if already applied
- Include comments explaining the purpose of each migration
- Test migrations on a copy of production data before deploying
- Keep SQL files well-documented with comments
- Use consistent formatting and naming

#### ❌ DON'T
- Never modify existing migration files that have been deployed
- Don't use `drop-first: true` in production
- Avoid large data migrations in the same changeset as schema changes
- Don't hardcode sensitive data in migrations
- Don't skip version numbers in sequence

## Environment Configuration

### Development
```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
    drop-first: false
  jpa:
    hibernate:
      ddl-auto: validate  # Only validate, don't auto-create
```

### Production
```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
    drop-first: false  # NEVER true in production!
  jpa:
    hibernate:
      ddl-auto: validate
```

## Troubleshooting

### Migration fails with "Table already exists"
**Solution:** The precondition should prevent this. If it happens:
1. Check if the table was created manually
2. Add precondition: `<not><tableExists tableName="..."/></not>`
3. Or mark the changeset as run: `liquibase:mark-next-changeset-ran`

### Need to rollback a migration
```bash
# Rollback the last changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Rollback to a specific tag
mvn liquibase:rollback -Dliquibase.rollbackTag=v1.0.0
```

### Check migration status
```bash
# View pending changesets
mvn liquibase:status

# Generate changelog from existing database (useful for documentation)
mvn liquibase:generateChangeLog
```

## Database Schema Standards

### Monetary Values
```sql
amount DECIMAL(38,2)  -- Always use DECIMAL for money, never FLOAT
```

### Timestamps
```sql
created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP  -- Microsecond precision
```

### Enums
```sql
-- Use CHECK constraints instead of database enums
ALTER TABLE orders ADD CONSTRAINT orders_status_check
    CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'CANCELLED'));
```

### Foreign Keys
```sql
-- Always name constraints explicitly
CONSTRAINT fk_orders_user_id FOREIGN KEY (user_id) REFERENCES users(id)
```

### Indexes
```sql
-- Name indexes descriptively
CREATE INDEX idx_orders_user_status ON orders(user_id, status);

-- Use partial indexes for filtered queries
CREATE INDEX idx_orders_active ON orders(status) WHERE status != 'CANCELLED';
```

## Support

For questions or issues with database migrations:
1. Check this README first
2. Review existing migrations for examples
3. Consult the Liquibase documentation: https://docs.liquibase.com/
4. Contact the database team

---

**Last Updated:** 2025-12-29
**Schema Version:** 1.0.0
**Database:** PostgreSQL 14+
