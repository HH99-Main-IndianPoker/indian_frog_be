package com.service.indianfrog.domain.game.dto;

import com.service.indianfrog.domain.game.entity.Card;
import com.service.indianfrog.domain.game.entity.Turn;
import com.service.indianfrog.domain.user.entity.User;
import lombok.Getter;

@Getter
public class GameDto {

    @Getter
    public static class StartRoundResponse {
        private String gameState;
        private int round;
        private User playerOne;
        private User playerTwo;
        private Card otherCard;
        private Turn turn;
        private int firstBet;
        private int roundPot;

        public StartRoundResponse(String gameState, int round, User playerOne, User playerTwo,
                                  Card otherCard, Turn turn, int firstBet, int roundPot) {
            this.gameState = gameState;
            this.round = round;
            this.playerOne = playerOne;
            this.playerTwo = playerTwo;
            this.otherCard = otherCard;
            this.turn = turn;
            this.firstBet = firstBet;
            this.roundPot = roundPot;

        }
    }

    @Getter
    public static class EndRoundResponse {
        private String nowState;
        private String nextState;
        private int round;
        private Card myCard;
        private User roundWinner;
        private User roundLoser;
        private int roundPot;

        public EndRoundResponse(String nowState, String nextState, int round, User roundWinner, User roundLoser, int roundPot, Card myCard) {
            this.nowState = nowState;
            this.nextState = nextState;
            this.round = round;
            this.myCard = myCard;
            this.roundWinner = roundWinner;
            this.roundLoser = roundLoser;
            this.roundPot = roundPot;
        }
    }

    @Getter
    public static class EndGameResponse {
        private String nowState;
        private String nextState;
        private User gameWinner;
        private User gameLoser;
        private int winnerPot;
        private int loserPot;

        public EndGameResponse(String nowState, String nextState, User gameWinner, User gameLoser, int winnerPot, int loserPot) {
            this.nowState = nowState;
            this.nextState = nextState;
            this.gameWinner = gameWinner;
            this.gameLoser = gameLoser;
            this.winnerPot = winnerPot;
            this.loserPot = loserPot;
        }
    }
}
