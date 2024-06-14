package com.service.indianfrog.domain.game.service;

import com.service.indianfrog.domain.game.dto.GameResponseDto.*;
import com.service.indianfrog.domain.game.entity.Card;
import com.service.indianfrog.domain.game.entity.Game;
import com.service.indianfrog.domain.game.entity.GameState;
import com.service.indianfrog.domain.game.entity.Turn;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "게임/라운드 종료 서비스", description = "게임/라운드 종료 서비스 로직")
@Slf4j
@Service
public class EndGameService {

    private final GameTurnService gameTurnService;
    private final ValidateRoomRepository validateRoomRepository;
    private final UserRepository userRepository;
    private final MeterRegistry registry;
    private final Timer totalRoundEndTimer;
    private final Timer totalGameEndTimer;
    private final GameValidator gameValidator;

    @PersistenceContext
    private EntityManager em;

    public EndGameService(GameTurnService gameTurnService, ValidateRoomRepository validateRoomRepository, UserRepository userRepository,
                          MeterRegistry registry, GameValidator gameValidator) {
        this.gameTurnService = gameTurnService;
        this.validateRoomRepository = validateRoomRepository;
        this.userRepository = userRepository;
        this.registry = registry;
        this.totalRoundEndTimer = registry.timer("totalRoundEnd.time");
        this.totalGameEndTimer = registry.timer("totalGameEnd.time");
        this.gameValidator = gameValidator;
    }

    @Transactional
    public EndRoundResponse endRound(Long gameRoomId, String email) {
        return totalRoundEndTimer.record(() -> {
            log.info("Ending round for gameRoomId={}", gameRoomId);

            GameRoom gameRoom = em.find(GameRoom.class, gameRoomId, LockModeType.PESSIMISTIC_WRITE);
            Game game = gameRoom.getCurrentGame();

            Timer.Sample gameResultTimer = Timer.start(registry);
            GameResult gameResult = determineGameResult(game);
            gameResultTimer.stop(registry.timer("roundResult.time"));

            Card myCard = email.equals(game.getPlayerOne().getEmail()) ? game.getPlayerOneCard() : game.getPlayerTwoCard();
            Card otherCard = email.equals(game.getPlayerOne().getEmail()) ? game.getPlayerTwoCard() : game.getPlayerOneCard();
            log.info("myCard : {}", myCard);

            int roundPot = game.getPot();

            if (!game.isRoundEnded()) {
                Timer.Sample roundPointsTimer = Timer.start(registry);
                assignRoundPointsToWinner(game, gameResult);
                roundPointsTimer.stop(registry.timer("roundPoints.time"));

                initializeTurnForGame(game, gameResult);
                game.updateRoundEnded();
            } else {
                game.resetRound();
                log.info("Round reset for gameRoomId={}", gameRoomId);
            }

            log.info("Round result determined: winnerId={}, loserId={}", gameResult.winner().getNickname(), gameResult.loser().getNickname());

            String nextState = determineGameState(game);
            log.info("Round ended for gameRoomId={}, newState={}", gameRoomId, nextState);

            return new EndRoundResponse("END", nextState, game.getRound(), gameResult.winner(), gameResult.loser(), roundPot, myCard, otherCard, gameResult.winner().getPoints(), gameResult.loser().getPoints());
        });
    }

    @Transactional
    public EndGameResponse endGame(Long gameRoomId, String email) {
        return totalGameEndTimer.record(() -> {
            log.info("Ending game for gameRoomId={}", gameRoomId);
            GameRoom gameRoom = gameValidator.validateAndRetrieveGameRoom(gameRoomId);

            User user = userRepository.findByEmail(email).orElseThrow(() -> new RestApiException(ErrorCode.NOT_FOUND_USER.getMessage()));
            validateRoomRepository.findByGameRoomAndParticipants(gameRoom, user.getNickname()).orElseThrow(() -> new RestApiException(ErrorCode.NOT_FOUND_GAME_USER.getMessage())).resetReady();

            Game game = gameRoom.getCurrentGame();

            Timer.Sample gameResultTimer = Timer.start(registry);
            GameResult gameResult = processGameResults(game, email);
            gameResultTimer.stop(registry.timer("endGameResult.time"));

            gameRoom.updateGameState(GameState.READY);

            log.info("Game ended for gameRoomId={}, winnerId={}, loserId={}, winnerPot={}, loserPot={}",
                    gameRoomId, gameResult.winner().getNickname(), gameResult.loser().getNickname(), gameResult.winnerPot(), gameResult.loserPot());

            return new EndGameResponse("GAME_END", "READY", gameResult.winner(), gameResult.loser(), gameResult.winnerPot() / 2, gameResult.loserPot() / 2);
        });
    }

    @Transactional
    public GameResult determineGameResult(Game game) {
        User playerOne = game.getPlayerOne();
        User playerTwo = game.getPlayerTwo();

        if (game.getFoldedUser() != null) {
            return game.getFoldedUser().equals(playerOne) ? GameResult.builder().winner(playerTwo).loser(playerOne).build() : GameResult.builder().winner(playerOne).loser(playerTwo).build();
        }

        return getGameResult(game, playerOne, playerTwo);
    }

    @Transactional
    public GameResult getGameResult(Game game, User playerOne, User playerTwo) {
        Card playerOneCard = game.getPlayerOneCard();
        Card playerTwoCard = game.getPlayerTwoCard();

        log.info("{} Card : {}", playerOne.getNickname(), playerOneCard);
        log.info("{} Card : {}", playerTwo.getNickname(), playerTwoCard);

        if (playerOneCard.getNumber() != playerTwoCard.getNumber()) {
            return playerOneCard.getNumber() > playerTwoCard.getNumber() ? GameResult.builder().winner(playerOne).loser(playerTwo).build() : GameResult.builder().winner(playerTwo).loser(playerOne).build();
        }

        return playerOneCard.getDeckNumber() > playerTwoCard.getDeckNumber() ? GameResult.builder().winner(playerOne).loser(playerTwo).build() : GameResult.builder().winner(playerTwo).loser(playerOne).build();
    }

    @Transactional
    public void assignRoundPointsToWinner(Game game, GameResult gameResult) {
        User winner = gameResult.winner();
        int pointsToAdd = game.getPot();

        winner.updatePoint(pointsToAdd);
        if (winner.equals(game.getPlayerOne())) {
            game.addPlayerOneRoundPoints(pointsToAdd);
        } else {
            game.addPlayerTwoRoundPoints(pointsToAdd);
        }

        log.info("Points assigned: winnerId={}, pointsAdded={}", winner.getNickname(), pointsToAdd);
    }

    private String determineGameState(Game game) {
        if (game.getRound() >= 3 || game.getPlayerOne().getPoints() <= 0 || game.getPlayerTwo().getPoints() <= 0) {
            return "GAME_END";
        }
        return "START";
    }

    @Transactional
    public GameResult processGameResults(Game game, String email) {
        int playerOneTotalPoints = game.getPlayerOneRoundPoints();
        int playerTwoTotalPoints = game.getPlayerTwoRoundPoints();

        log.info("playerOneTotalPoint : {}", playerOneTotalPoints);
        log.info("playerTwoTotalPoint : {}", playerTwoTotalPoints);

        User gameWinner = playerOneTotalPoints > playerTwoTotalPoints ? game.getPlayerOne() : game.getPlayerTwo();
        User gameLoser = gameWinner.equals(game.getPlayerOne()) ? game.getPlayerTwo() : game.getPlayerOne();

        if (email.equals(gameWinner.getEmail())) {
            gameWinner.incrementWins();
        }

        if (email.equals(gameLoser.getEmail())) {
            gameLoser.incrementLosses();
        }

        int winnerTotalPoints = gameWinner.equals(game.getPlayerOne()) ? playerOneTotalPoints : playerTwoTotalPoints;
        int loserTotalPoints = gameLoser.equals(game.getPlayerOne()) ? playerOneTotalPoints : playerTwoTotalPoints;

        log.info("winnerTotalPoints : {}", winnerTotalPoints);
        log.info("loserTotalPoints : {}", loserTotalPoints);

        game.resetGame();

        return new GameResult(gameWinner, gameLoser, winnerTotalPoints, loserTotalPoints);
    }

    @Transactional
    public void initializeTurnForGame(Game game, GameResult gameResult) {
        List<User> players = List.of(gameResult.winner(), gameResult.loser());
        gameTurnService.removeTurn(game.getId());
        Turn turn = new Turn(players);
        gameTurnService.setTurn(game.getId(), turn);
    }
}
