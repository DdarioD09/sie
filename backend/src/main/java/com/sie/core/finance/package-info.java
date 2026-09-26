/**
 * Money side of the business. {@code MoneyTransaction} (type: INCOME/EXPENSE; category:
 * SALE_ONLINE, SALE_CENTRO, RAW_MATERIAL, SERVICE, OTHER; amount; date; optional reference back to
 * the sales order / centro shipment / material purchase that caused it) and {@code Investment}
 * (amount, date, notes) for the founding capital and any later injections. The finance dashboard
 * is: sum(income) - sum(expense) since inception, plus total invested, plus a running balance.
 */
package com.sie.core.finance;
