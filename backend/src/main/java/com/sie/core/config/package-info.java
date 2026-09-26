/**
 * Reserved for app-wide config that doesn't belong to one feature package - e.g. an OpenAPI/Swagger
 * bean once you want interactive API docs, or a WebConfig for pagination defaults. CORS and the
 * security filter chain live in {@code auth.SecurityConfig} for now since they're really part of
 * the auth story, not general config.
 */
package com.sie.core.config;
