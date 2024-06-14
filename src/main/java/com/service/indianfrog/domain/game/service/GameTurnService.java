package com.service.indianfrog.domain.game.service;

import com.service.indianfrog.domain.game.entity.Turn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Tag(name = "게임 내 턴 관리 서비스 로직")
@Slf4j
@Service
public class GameTurnService {

    /* 게임 종료 시 MAP에 있는 해당 게임의 턴 정보 삭제하는 메서드 추가 및 endGame에서 호출*/
    private final Map<Long, Turn> gameTurns = new ConcurrentHashMap<>();

    public void setTurn(Long gameId, Turn turn) {
        gameTurns.put(gameId, turn);
    }

    public Turn getTurn(Long gameId) {
        return gameTurns.get(gameId);
    }

    public void removeTurn(Long gameId) {
        gameTurns.remove(gameId);
    }
}
