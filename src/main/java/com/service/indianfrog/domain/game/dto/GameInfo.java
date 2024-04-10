package com.service.indianfrog.domain.game.dto;

import com.service.indianfrog.domain.game.entity.Card;
import com.service.indianfrog.domain.game.entity.Turn;
import lombok.Getter;

@Getter
public class GameInfo {
    private Card playerCard;
    private Turn turn;

    public GameInfo(Card playerCard, Turn turn) {
        this.playerCard = playerCard;
        this.turn = turn;
    }
}