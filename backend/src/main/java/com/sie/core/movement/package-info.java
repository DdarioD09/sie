/**
 * The inventory ledger. {@code StockMovement} (variant, {@code MovementType}: PRODUCTION_IN,
 * SALE_OUT, CENTRO_OUT, ADJUSTMENT; quantity; occurred_at; optional reference to the sales order /
 * centro shipment / production batch that caused it) is the single source of truth for stock.
 *
 * <p>Current stock for a variant = sum of its movement quantities (incomes positive, outcomes
 * negative). The Dashboard reads a query over this table (or a materialized view once
 * performance matters) rather than a mutable "quantity" column anywhere else - see
 * ARCHITECTURE.md, "Why a ledger and not a counter".
 */
package com.sie.core.movement;
