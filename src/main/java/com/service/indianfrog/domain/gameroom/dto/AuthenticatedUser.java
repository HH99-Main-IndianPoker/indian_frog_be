package com.service.indianfrog.domain.gameroom.dto;

import java.security.Principal;

public class AuthenticatedUser implements Principal {
    private String name;

    public AuthenticatedUser(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
