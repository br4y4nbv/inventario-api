package com.br4y4nbv.inventario_api.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}