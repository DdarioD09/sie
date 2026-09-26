/**
 * Online sales module: {@code SalesOrder} (customer name/address/phone, status: BOOKED,
 * IN_PROGRESS, DELIVERED, CANCELLED; order date) and {@code SalesOrderItem} (variant, quantity,
 * unit price).
 *
 * <p>Current business rule (cash on delivery): booking an order writes a SALE_OUT
 * {@code StockMovement} per item immediately (stock is committed as soon as it's booked), but NO
 * {@code MoneyTransaction} yet - the money only exists once {@code DELIVERED} is confirmed, which
 * is when the INCOME {@code MoneyTransaction} (category SALE_ONLINE) is written. Cancelling a
 * BOOKED or IN_PROGRESS order reverses only the stock (a positive ADJUSTMENT movement) - there is
 * no money to reverse since none was recorded yet. DELIVERED is terminal: an order can't be
 * cancelled after delivery in this version.
 *
 * <p>Designed to flex to "paid at booking" later: the trigger is just "whichever service method
 * calls {@code MoneyTransactionService.record(...)}", not something baked into the schema - move
 * that one call from the DELIVERED transition to the BOOKED transition and you're done. See
 * ARCHITECTURE.md section 4 for the full table of what each transition should do.
 */
package com.sie.core.sales;
