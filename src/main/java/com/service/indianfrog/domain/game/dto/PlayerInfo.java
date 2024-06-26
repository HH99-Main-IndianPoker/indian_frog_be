package com.service.indianfrog.domain.game.dto;

import com.service.indianfrog.domain.game.entity.Card;
import com.service.indianfrog.domain.user.entity.User;
import lombok.Getter;

@Getter
public class PlayerInfo {
    private String email;
    private Card card;

    public PlayerInfo(User player, Card card) {
        this.email = player.getEmail();
        this.card = card;
    }
}
