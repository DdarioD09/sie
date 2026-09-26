package com.sie.core.product.dto;

/** What the API sends back. Separate from the request DTO because responses need the id and active flag. */
public record ProductTypeResponse(Long id, String name, boolean active) {}
