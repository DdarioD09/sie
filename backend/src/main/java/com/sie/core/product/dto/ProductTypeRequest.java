package com.sie.core.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** What the client sends to create/update a product type. Never expose entities directly over REST. */
public record ProductTypeRequest(@NotBlank @Size(max = 80) String name) {}
