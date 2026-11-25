-- =====================================================================
-- SupportDesk V1: initial schema
-- Tenant isolation: every tenant-owned table carries company_id, and
-- composite foreign keys (company_id, x_id) make it impossible for a row
-- to reference a parent that belongs to a different company.
-- =====================================================================

-- ---------- companies ----------
CREATE TABLE companies (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    name            VARCHAR(150)  NOT NULL,
    slug            VARCHAR(60)   NOT NULL,
    email           VARCHAR(255)  NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    ticket_sequence BIGINT        NOT NULL DEFAULT 0,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_companies PRIMARY KEY (id),
    CONSTRAINT uq_companies_slug UNIQUE (slug),
    CONSTRAINT chk_companies_status CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT chk_companies_slug CHECK (
        CHAR_LENGTH(slug) >= 3
        AND REGEXP_LIKE(slug, '^[a-z0-9]+(-[a-z0-9]+)*$', 'c')),
    CONSTRAINT chk_companies_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_companies_sequence CHECK (ticket_sequence >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------- users ----------
CREATE TABLE users (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    company_id           BIGINT       NOT NULL,
    name                 VARCHAR(100) NOT NULL,
    email                VARCHAR(255) NOT NULL,
    password_hash        VARCHAR(100) NOT NULL,
    role                 VARCHAR(20)  NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    must_change_password BOOLEAN      NOT NULL DEFAULT FALSE,
    password_changed_at  DATETIME(6)  NULL,
    last_login_at        DATETIME(6)  NULL,
    created_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),                 -- globally unique (decision 2)
    CONSTRAINT uq_users_company_id UNIQUE (company_id, id),   -- target of composite FKs
    CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT chk_users_role CHECK (role IN ('COMPANY_ADMIN', 'SUPPORT_AGENT', 'CUSTOMER')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_users_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    INDEX idx_users_company_role_status (company_id, role, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------- categories ----------
CREATE TABLE categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    company_id  BIGINT       NOT NULL,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255) NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_categories_company_name UNIQUE (company_id, name),
    CONSTRAINT uq_categories_company_id UNIQUE (company_id, id),
    CONSTRAINT fk_categories_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT chk_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_categories_name CHECK (CHAR_LENGTH(TRIM(name)) > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------- tickets ----------
CREATE TABLE tickets (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    company_id        BIGINT       NOT NULL,
    ticket_number     VARCHAR(30)  NOT NULL,
    customer_id       BIGINT       NOT NULL,
    assigned_agent_id BIGINT       NULL,
    category_id       BIGINT       NOT NULL,
    subject           VARCHAR(200) NOT NULL,
    description       TEXT         NOT NULL,
    priority          VARCHAR(10)  NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at       DATETIME(6)  NULL,
    closed_at         DATETIME(6)  NULL,
    version           BIGINT       NOT NULL DEFAULT 0,   -- optimistic locking (@Version)
    CONSTRAINT pk_tickets PRIMARY KEY (id),
    CONSTRAINT uq_tickets_company_number UNIQUE (company_id, ticket_number),
    CONSTRAINT uq_tickets_company_id UNIQUE (company_id, id),
    CONSTRAINT fk_tickets_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_tickets_customer FOREIGN KEY (company_id, customer_id) REFERENCES users (company_id, id),
    CONSTRAINT fk_tickets_agent FOREIGN KEY (company_id, assigned_agent_id) REFERENCES users (company_id, id),
    CONSTRAINT fk_tickets_category FOREIGN KEY (company_id, category_id) REFERENCES categories (company_id, id),
    CONSTRAINT chk_tickets_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_tickets_status CHECK (status IN
        ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'PENDING_CUSTOMER', 'ESCALATED', 'RESOLVED', 'CLOSED', 'REOPENED')),
    CONSTRAINT chk_tickets_subject CHECK (CHAR_LENGTH(TRIM(subject)) > 0),
    CONSTRAINT chk_tickets_description CHECK (CHAR_LENGTH(TRIM(description)) > 0),
    -- Indexes: company_id always first so every tenant-scoped query is covered.
    INDEX idx_tickets_company_status (company_id, status, created_at),
    INDEX idx_tickets_company_customer (company_id, customer_id, created_at),
    INDEX idx_tickets_company_agent (company_id, assigned_agent_id, status),
    INDEX idx_tickets_company_category (company_id, category_id),
    INDEX idx_tickets_company_priority (company_id, priority),
    INDEX idx_tickets_company_created (company_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------- ticket_comments ----------
CREATE TABLE ticket_comments (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    company_id BIGINT      NOT NULL,
    ticket_id  BIGINT      NOT NULL,
    author_id  BIGINT      NOT NULL,
    message    TEXT        NOT NULL,
    visibility VARCHAR(10) NOT NULL DEFAULT 'PUBLIC',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_ticket_comments PRIMARY KEY (id),
    CONSTRAINT fk_comments_ticket FOREIGN KEY (company_id, ticket_id) REFERENCES tickets (company_id, id),
    CONSTRAINT fk_comments_author FOREIGN KEY (company_id, author_id) REFERENCES users (company_id, id),
    CONSTRAINT chk_comments_visibility CHECK (visibility IN ('PUBLIC', 'INTERNAL')),
    CONSTRAINT chk_comments_message CHECK (CHAR_LENGTH(TRIM(message)) > 0),
    -- visibility is in the index so the customer-facing query (PUBLIC only) stays fast
    INDEX idx_comments_ticket_visibility (company_id, ticket_id, visibility, created_at),
    INDEX idx_comments_author (company_id, author_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------- ticket_history ----------
CREATE TABLE ticket_history (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    company_id BIGINT       NOT NULL,
    ticket_id  BIGINT       NOT NULL,
    event_type VARCHAR(30)  NOT NULL,
    old_value  VARCHAR(100) NULL,    -- readable snapshot (e.g. agent name), not an ID
    new_value  VARCHAR(100) NULL,
    changed_by BIGINT       NOT NULL,
    changed_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_ticket_history PRIMARY KEY (id),
    CONSTRAINT fk_history_ticket FOREIGN KEY (company_id, ticket_id) REFERENCES tickets (company_id, id),
    CONSTRAINT fk_history_user FOREIGN KEY (company_id, changed_by) REFERENCES users (company_id, id),
    CONSTRAINT chk_history_event CHECK (event_type IN
        ('CREATED', 'STATUS_CHANGED', 'ASSIGNED', 'REASSIGNED', 'PRIORITY_CHANGED')),
    INDEX idx_history_ticket (company_id, ticket_id, changed_at),
    INDEX idx_history_company_time (company_id, changed_at),   -- "recent activity" on dashboards
    INDEX idx_history_user (company_id, changed_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
