-- ============================================================================
-- V1: initial schema.
--
-- Flyway convention: every migration file is named V<version>__<description>.sql and, once it has
-- been applied to any database (including your own laptop), it must never be edited again - Flyway
-- checksums each applied migration and refuses to start if a past one changed. Need to fix
-- something? Write a new V2__... migration that alters the table. This is what makes it safe for
-- two people (you and your coworker) to each run their own local Postgres and still end up with
-- identical schemas, and what makes deploying a schema change to Railway/Render just "run the app,
-- Flyway does the rest".
-- ============================================================================

-- Login accounts (you + your coworker). See auth.User / Role.
CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(60)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'VIEWER')),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Garment categories: "Sweater" today, more later. See product.ProductType (already implemented).
CREATE TABLE product_types (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(80) NOT NULL UNIQUE,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- A sellable design, e.g. "Classic Crewneck". Sizes/colors live one level down in product_variants.
CREATE TABLE products (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku             VARCHAR(40)     NOT NULL UNIQUE,
    name            VARCHAR(150)    NOT NULL,
    description     TEXT,
    product_type_id BIGINT          NOT NULL REFERENCES product_types (id),
    base_price      NUMERIC(10, 2)  NOT NULL CHECK (base_price >= 0),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_product_type ON products (product_type_id);

-- The actual stockable unit: one size+color combo of a product. This is what a StockMovement,
-- SalesOrderItem, or CentroShipment always points at - never the parent Product directly.
CREATE TABLE product_variants (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id   BIGINT      NOT NULL REFERENCES products (id),
    sku_variant  VARCHAR(50) NOT NULL UNIQUE,
    size         VARCHAR(20) NOT NULL,
    color        VARCHAR(40) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (product_id, size, color)
);
CREATE INDEX idx_variants_product ON product_variants (product_id);

-- The inventory ledger. current stock for a variant = SUM(quantity) WHERE product_variant_id = ?
-- quantity is stored positive for incomes (PRODUCTION_IN) and negative for outcomes
-- (SALE_OUT, CENTRO_OUT), so a plain SUM gives the running balance - no separate "stock" column to
-- keep in sync. reference_type/reference_id optionally point back at the row that caused the
-- movement (e.g. 'SALES_ORDER_ITEM', 123) purely for traceability/debugging.
CREATE TABLE stock_movements (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_variant_id BIGINT      NOT NULL REFERENCES product_variants (id),
    movement_type      VARCHAR(20) NOT NULL CHECK (movement_type IN ('PRODUCTION_IN', 'SALE_OUT', 'CENTRO_OUT', 'ADJUSTMENT')),
    quantity            INTEGER     NOT NULL CHECK (quantity <> 0),
    occurred_at        DATE        NOT NULL,
    reference_type     VARCHAR(30),
    reference_id       BIGINT,
    notes              VARCHAR(255),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_movements_variant ON stock_movements (product_variant_id);
CREATE INDEX idx_movements_type_date ON stock_movements (movement_type, occurred_at);

-- Online orders. status drives what happens next (see finance/sales package-info + ARCHITECTURE.md).
CREATE TABLE sales_orders (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_name    VARCHAR(150) NOT NULL,
    customer_address VARCHAR(255) NOT NULL,
    customer_phone   VARCHAR(30)  NOT NULL,
    status           VARCHAR(20)  NOT NULL CHECK (status IN ('BOOKED', 'IN_PROGRESS', 'DELIVERED', 'CANCELLED')),
    order_date       DATE         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE sales_order_items (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sales_order_id     BIGINT         NOT NULL REFERENCES sales_orders (id) ON DELETE CASCADE,
    product_variant_id BIGINT         NOT NULL REFERENCES product_variants (id),
    quantity           INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price         NUMERIC(10, 2) NOT NULL CHECK (unit_price >= 0),
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX idx_order_items_order ON sales_order_items (sales_order_id);

-- Physical store consignment: what we sent (stock side only). Deliberately does NOT carry an
-- income amount - see centro_incomes below for why the two are separate tables.
CREATE TABLE centro_shipments (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_variant_id BIGINT      NOT NULL REFERENCES product_variants (id),
    quantity           INTEGER     NOT NULL CHECK (quantity > 0),
    shipped_date       DATE        NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Money the physical store pays us, days after a shipment, for an unspecified subset of what was
-- sent (no per-variant/per-shipment breakdown available). Kept as its own table - NOT a foreign
-- key to centro_shipments - because a single income payment can't be attributed to one shipment:
-- it might cover half of one shipment and a third of another. The only link between the two sides
-- is "both belong to the Centro channel"; the money side also writes a matching row in
-- money_transactions (category SALE_CENTRO) so it rolls up into the finance totals the same way
-- every other income does.
CREATE TABLE centro_incomes (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    amount       NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    received_date DATE          NOT NULL,
    notes        VARCHAR(255),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Raw materials. A separate, simpler stock ledger from garments (no size/color variants).
CREATE TABLE materials (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku           VARCHAR(40)    NOT NULL UNIQUE,
    description   VARCHAR(255)   NOT NULL,
    quantity      NUMERIC(10, 2) NOT NULL CHECK (quantity >= 0),
    unit_price    NUMERIC(10, 2) NOT NULL CHECK (unit_price >= 0),
    purchase_date DATE           NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- All money movements: online sales, centro income, material purchases, service payments.
-- reference_type/reference_id optionally link back to the sales_order / centro_shipment /
-- material row that generated the transaction.
CREATE TABLE money_transactions (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type           VARCHAR(10)    NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    category       VARCHAR(20)    NOT NULL CHECK (category IN ('SALE_ONLINE', 'SALE_CENTRO', 'RAW_MATERIAL', 'SERVICE', 'OTHER')),
    amount         NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    occurred_at    DATE           NOT NULL,
    reference_type VARCHAR(30),
    reference_id   BIGINT,
    notes          VARCHAR(255),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX idx_money_type_date ON money_transactions (type, occurred_at);

-- Founding capital, and any later capital injections. Kept as a table (not a single number) so a
-- second investment later doesn't require a schema change.
CREATE TABLE investments (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    amount     NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    invested_on DATE          NOT NULL,
    notes      VARCHAR(255),
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT now()
);
