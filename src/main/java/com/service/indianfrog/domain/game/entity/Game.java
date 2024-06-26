package com.service.indianfrog.domain.game.entity;

import com.service.indianfrog.domain.gameroom.entity.GameRoom;
import com.service.indianfrog.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Table(name = "game")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private GameRoom gameRoom;

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING) // Enum 타입을 저장
    private Set<Card> usedCards = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private User playerOne;

    @ManyToOne(fetch = FetchType.LAZY)
    private User playerTwo;

    @Enumerated(EnumType.STRING) // 카드 Enum을 저장
    private Card playerOneCard;

    @Enumerated(EnumType.STRING) // 카드 Enum을 저장
    private Card playerTwoCard;

    private int betAmount;

    private int pot; // 현재 라운드의 포트

    @ManyToOne(fetch = FetchType.LAZY)
    private User foldedUser;

    // 플레이어가 라운드에서 획득한 포인트
    private int playerOneRoundPoints;
    private int playerTwoRoundPoints;

    // 라운드 정보
    private int round;

    private boolean checkStatus;
    private boolean raiseStatus;

    private boolean roundEnded;
    private boolean roundStarted;

    private boolean cardAllocation;

    // Constructor and methods
    @Builder
    public Game(User playerOne, User playerTwo, GameRoom gameRoom) {
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.gameRoom = gameRoom;
        this.usedCards = new HashSet<>();
    }

    public Game() {

    }

    public void addUsedCard(Card card) {
        this.usedCards.add(card);
    }

    public void incrementRound() {
        this.round++;
    }

    public void setPlayerOneCard(Card card) {
        this.playerOneCard = card;
    }

    public void setPlayerTwoCard(Card card) {
        this.playerTwoCard = card;
    }

    public void setBetAmount(int betAmount) {
        this.betAmount = betAmount;
    }

    public void updatePot(int point) {
        this.pot = pot + point;
    }

    // 게임에서 포기한 유저를 설정합니다.
    public void setFoldedUser(User user) {
        this.foldedUser = user;
    }

    public void addPlayerOneRoundPoints(int points) {
        this.playerOneRoundPoints += points;
    }

    public void addPlayerTwoRoundPoints(int points) {
        this.playerTwoRoundPoints += points;
    }

    public void resetRound() {
        /* 라운드 정보 초기화
         * 베팅액, 각 플레이어 카드 정보 초기화*/
        this.checkStatus = false;
        this.raiseStatus = false;
        this.roundEnded = false;
        this.roundStarted = false;
        this.pot = 0;
        this.cardAllocation = false;
        this.foldedUser = null;
    }

    // 게임과 관련된 상태를 초기화하는 메서드
    public void resetGame() {
        /* 게임에 사용된 카드 정보,
         * 게임에서 각 유저가 획득한 포인트,
         * 라운드 정보 초기화*/
        usedCards.clear();
        this.round = 0;
        this.checkStatus = false;
        this.raiseStatus = false;
    }

    public void updateCheck() {
        this.checkStatus = true;
    }

    public void updateRaise() {
        this.raiseStatus = true;
    }

    public void startGame(User playerOne, User playerTwo) {
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.playerOneRoundPoints = 0;
        this.playerTwoRoundPoints = 0;
        this.foldedUser = null;
        this.pot = 0;
    }

    public void updateRoundEnded() {
        this.roundEnded = true;
    }

    public void updateRoundStarted() {
        this.roundStarted = true;
    }

    public void updateCardAllocation(){
        this.cardAllocation = true;
    }
}
