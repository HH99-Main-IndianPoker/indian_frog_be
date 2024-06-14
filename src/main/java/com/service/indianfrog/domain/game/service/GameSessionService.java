package com.service.indianfrog.domain.game.service;

import com.service.indianfrog.domain.game.dto.GameResponseDto.*;
import com.service.indianfrog.domain.game.dto.UserChoices;
import com.service.indianfrog.domain.game.entity.GameState;
import com.service.indianfrog.domain.game.utils.GameValidator;
import com.service.indianfrog.domain.gameroom.entity.GameRoom;
import com.service.indianfrog.domain.user.entity.User;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Tag(name = "게임 종료 후 유저 선택 서비스 클래스", description = "게임 종료 후 유저의 선택에 따라 서비스를 수행하는 로직")
@Slf4j
@Service
public class GameSessionService {

    private final GameValidator gameValidator;
    private final MeterRegistry registry;
    private final Timer totalUserChoiceTimer;

    @PersistenceContext
    private EntityManager em;

    private final ConcurrentMap<Long, ConcurrentMap<String, String>> gameChoices = new ConcurrentHashMap<>();

    public GameSessionService(GameValidator gameValidator, MeterRegistry registry) {
        this.gameValidator = gameValidator;
        this.registry = registry;
        this.totalUserChoiceTimer = registry.timer("totalChoice.time");
    }

    @Transactional
    public Object processUserChoices(Long gameRoomId, UserChoices choices) {
        return totalUserChoiceTimer.record(() -> {
            log.info("Processing user choices for gameRoomId={} with nickname={}", gameRoomId, choices.getNickname());

            GameRoom gameRoom = em.find(GameRoom.class, gameRoomId, LockModeType.PESSIMISTIC_READ);
            User player = gameValidator.findUserByNickname(choices.getNickname());
            String nickname = player.getNickname();
            String choice = choices.getUserChoice().toString();

            log.debug("User choice received: gameRoomId={}, nickname={}, choice={}", gameRoomId, nickname, choice);

            saveUserChoice(gameRoomId, nickname, choice);

            if (areAllChoicesIn(gameRoomId)) {
                GameState nextState = determineActionAndProceed(gameRoomId);
                log.info("All user choices received for gameRoomId={}, proceeding with action", gameRoomId);
                return new GameStatus(gameRoomId, nickname, nextState);
            }

            log.info("Waiting for other player's choices in gameRoomId={}", gameRoomId);
            return "다른 플레이어의 선택을 기다려주세요";
        });
    }

    private void saveUserChoice(Long gameRoomId, String nickname, String choice) {
        Timer.Sample saveChoiceTimer = Timer.start(registry);
        gameChoices.computeIfAbsent(gameRoomId, k -> new ConcurrentHashMap<>()).put(nickname, choice);
        saveChoiceTimer.stop(registry.timer("saveChoice.time"));
        log.debug("Current game choices: {}", gameChoices.get(gameRoomId));
    }

    private boolean areAllChoicesIn(Long gameRoomId) {
        return gameChoices.get(gameRoomId).size() == 2;
    }

    private GameState determineActionAndProceed(Long gameRoomId) {
        Map<String, String> roomChoices = gameChoices.get(gameRoomId);
        boolean allSame = new HashSet<>(roomChoices.values()).size() == 1;
        gameChoices.remove(gameRoomId);

        log.debug("Determining action for gameRoomId={}, all choices same: {}", gameRoomId, allSame);

        if (allSame) {
            GameState finalState = roomChoices.values().iterator().next().equals("PLAY_AGAIN") ? GameState.START : GameState.LEAVE;
            log.info("Action determined for gameRoomId={}, state: {}", gameRoomId, finalState);
            return finalState;
        } else {
            log.info("Conflicting choices in gameRoomId={}, defaulting to ENTER state", gameRoomId);
            GameRoom gameRoom = gameValidator.validateAndRetrieveGameRoom(gameRoomId);
            gameRoom.endCurrentGame();
            return GameState.ENTER;
        }
    }
}

