package com.service.indianfrog.domain.game.service;

import com.service.indianfrog.domain.game.dto.GameResponseDto.*;
import com.service.indianfrog.domain.game.entity.GameState;
import com.service.indianfrog.domain.game.utils.GameValidator;
import com.service.indianfrog.domain.gameroom.entity.GameRoom;
import com.service.indianfrog.domain.gameroom.entity.ValidateRoom;
import com.service.indianfrog.domain.gameroom.repository.GameRoomRepository;
import com.service.indianfrog.domain.gameroom.repository.ValidateRoomRepository;
import com.service.indianfrog.domain.user.entity.User;
import com.service.indianfrog.domain.user.repository.UserRepository;
import com.service.indianfrog.global.exception.ErrorCode;
import com.service.indianfrog.global.exception.RestApiException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

import static com.service.indianfrog.global.exception.ErrorCode.INSUFFICIENT_POINTS;

@Service
public class ReadyService {

    private final GameRoomRepository gameRoomRepository;
    private final ValidateRoomRepository validateRoomRepository;
    private final UserRepository userRepository;
    private final MeterRegistry registry;
    private final Timer totalGameReadyTimer;
    private final GameValidator gameValidator;

    public ReadyService(GameRoomRepository gameRoomRepository, ValidateRoomRepository validateRoomRepository,
                        UserRepository userRepository, MeterRegistry registry, GameValidator gameValidator) {
        this.gameRoomRepository = gameRoomRepository;
        this.validateRoomRepository = validateRoomRepository;
        this.userRepository = userRepository;
        this.registry = registry;
        this.totalGameReadyTimer = registry.timer("totalReady.time");
        this.gameValidator = gameValidator;
    }

    @Transactional
    public GameStatus gameReady(Long gameRoomId, Principal principal) {
        return totalGameReadyTimer.record(() -> {
            User user = getUserByEmail(principal.getName());
            GameRoom gameRoom = getGameRoomById(gameRoomId);
            ValidateRoom validateRoom = getValidateRoom(gameRoom, user.getNickname());

            if (!hasSufficientPoints(user)) {
                throw new RestApiException(INSUFFICIENT_POINTS.getMessage());
            }

            validateRoom.revert(validateRoom.isReady());
            List<ValidateRoom> readyRooms = getReadyValidateRooms(gameRoom);

            return determineGameStatus(gameRoomId, user.getNickname(), validateRoom, readyRooms);
        });
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RestApiException(ErrorCode.NOT_FOUND_GAME_USER.getMessage()));
    }

    private GameRoom getGameRoomById(Long gameRoomId) {
        return gameRoomRepository.findById(gameRoomId).orElseThrow(() -> new RestApiException(ErrorCode.NOT_FOUND_GAME_ROOM.getMessage()));
    }

    private ValidateRoom getValidateRoom(GameRoom gameRoom, String nickname) {
        return validateRoomRepository.findByGameRoomAndParticipants(gameRoom, nickname).orElseThrow(() -> new RestApiException(ErrorCode.GAME_ROOM_NOW_FULL.getMessage()));
    }

    private boolean hasSufficientPoints(User user) {
        return user.getPoints() > 0;
    }

    private List<ValidateRoom> getReadyValidateRooms(GameRoom gameRoom) {
        Timer.Sample timer = Timer.start(registry);
        List<ValidateRoom> readyRooms = validateRoomRepository.findAllByGameRoomAndReadyTrue(gameRoom);
        timer.stop(registry.timer("readyValidate.time"));
        return readyRooms;
    }

    private GameStatus determineGameStatus(Long gameRoomId, String nickname, ValidateRoom validateRoom, List<ValidateRoom> readyRooms) {
        if (readyRooms.size() == 2) {
            gameValidator.gameValidate(getGameRoomById(gameRoomId));
            return new GameStatus(gameRoomId, nickname, GameState.ALL_READY);
        }

        if (readyRooms.size() == 1) {
            return new GameStatus(gameRoomId, nickname, validateRoom.isReady() ? GameState.READY : GameState.UNREADY);
        }

        return new GameStatus(gameRoomId, nickname, GameState.NO_ONE_READY);
    }
}