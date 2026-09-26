package com.sie.core.auth;

/**
 * Only two roles for now, matching the requirement "only my coworker and I can log in":
 * ADMIN sees and manages everything; VIEWER is read-only on the catalog. Both of you will be
 * seeded as ADMIN - VIEWER exists so the catalog-only view has a real role to gate on later
 * (e.g. if you ever add a customer-facing account) instead of hardcoding "admin or not".
 */
public enum Role {
  ADMIN,
  VIEWER
}
