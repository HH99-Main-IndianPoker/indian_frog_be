package com.service.indianfrog.domain.game.entity;

import lombok.Getter;

@Getter
public enum GameState {
    /* 게임 상태 Enum Class
     * ENTER : 게임방 입장
     * READY : 게임 준비
     * ALL_READY : 모두 게임 준비
     * START : 게임 시작
     * ACTION : 유저 행동
     * BET : 배팅
     * END : 라운드 종료
     * LEAVE : 게임 방 떠나기
     * GAME_END : 게임 종료
     * USER_CHOICE : 유저 선택(게임 재시작, 게임 나가기)*/
    ENTER("ENTER"),
    READY("READY"),
    UNREADY("UNREADY"),
    NO_ONE_READY("NO_ONE_READY"),
    ALL_READY("ALL_READY"),
    START("START"),
    ACTION("ACTION"),
    END("END"),
    BET("BET"),
    LEAVE("LEAVE"),
    GAME_END("GAME_END"),
    USER_CHOICE("USER_CHOICE"),
    ALONE("ALONE");

    private final String gameState;

    GameState(String gameState) {
        this.gameState = gameState;
    }
}
