package com.service.indianfrog.domain.game.dto;

import com.service.indianfrog.domain.game.entity.Betting;
import com.service.indianfrog.domain.game.entity.GameState;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ActionDto {

    String nowState;
    String nextState;
    String actionType;
    String currentPlayer;
    String previousPlayer;
    int nowBet;
    int pot;
    int myPoint;
    int otherPoint;

    @Builder
    public ActionDto(GameState nowState, GameState nextState, Betting actionType, int nowBet, int pot, String currentPlayer, String previousPlayer,int myPoint, int otherPoint){
        this.nowState = nowState.getGameState();
        this.nextState = nextState.getGameState();
        this.actionType = actionType.getBetting();
        this.nowBet = nowBet;
        this.pot = pot;
        this.currentPlayer = currentPlayer;
        this.previousPlayer = previousPlayer;
        this.myPoint = myPoint;
        this.otherPoint = otherPoint;
    }
}
