/**
 * Raw materials inventory: {@code Material} (sku, description, quantity, unit price, purchase
 * date). Recording a purchase writes an EXPENSE {@code MoneyTransaction} (category RAW_MATERIAL).
 * This is a separate, simpler stock ledger from garments - materials aren't sized/colored variants,
 * so they don't need the product/movement machinery.
 */
package com.sie.core.materials;
