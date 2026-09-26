/**
 * Fabric/production incomes. Deliberately thin: recording a production income is just creating a
 * PRODUCTION_IN {@code StockMovement} for a variant with a quantity and date. This package holds
 * the read side (a small service/controller listing production-in movements) rather than a
 * duplicate entity - reuse the ledger from {@code movement} instead of inventing a second one.
 */
package com.sie.core.production;
