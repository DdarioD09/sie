/**
 * Physical store ("Centro") consignment - two separate concepts, deliberately not one entity:
 *
 * <ul>
 *   <li>{@code CentroShipment} (variant, quantity, shipped date) - what we sent. Recording one
 *       writes a CENTRO_OUT {@code StockMovement}.
 *   <li>{@code CentroIncome} (amount, received date, notes) - money the store pays us later for
 *       an unspecified subset of what we've shipped there. Recording one writes an INCOME
 *       {@code MoneyTransaction} (category SALE_CENTRO).
 * </ul>
 *
 * <p>They are NOT linked to each other: we don't track which shipment a given income payment
 * corresponds to (it might be a partial payment covering pieces of several shipments), only "what
 * we've sent to Centro in total" and "what Centro has paid us in total." See ARCHITECTURE.md
 * section 2 for the reasoning.
 */
package com.sie.core.centro;
