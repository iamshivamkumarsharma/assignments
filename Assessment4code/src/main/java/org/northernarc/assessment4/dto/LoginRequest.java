package org.northernarc.assessment4.dto;

public record LoginRequest(
        String email,
        String password
) {}
