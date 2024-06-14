package com.service.indianfrog.domain.game.service;

import com.service.indianfrog.domain.game.dto.GameResponseDto.*;
import com.service.indianfrog.domain.game.entity.Card;
import com.service.indianfrog.domain.game.entity.Game;
import com.service.indianfrog.domain.game.entity.GameState;
import com.service.indianfrog.domain.game.entity.Turn;
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

import java.util.*;

@Tag(name = "라운드 시작 서비스", description = "게임(라운드) 시작 서비스 로직")
@Slf4j(topic = "게임 시작 서비스 레이어")
@Service
public class StartGameService {

    /* 생성자를 통한 필드 주입 */
    private final GameTurnService gameTurnService;
    private final Timer totalRoundStartTimer;
    private final Timer performRoundStartTimer;
    @PersistenceContext
    private EntityManager em;

    public StartGameService(GameTurnService gameTurnService, MeterRegistry registry) {
        this.gameTurnService = gameTurnService;
        this.totalRoundStartTimer = registry.timer("totalRoundStart.time");
        this.performRoundStartTimer = registry.timer("performRoundStart.time");
    }

    @Transactional
    public StartRoundResponse startRound(Long gameRoomId, String email) {
        return totalRoundStartTimer.record(() -> {
            log.info("게임룸 ID {}로 라운드 시작", gameRoomId);

            GameRoom gameRoom = em.find(GameRoom.class, gameRoomId, LockModeType.PESSIMISTIC_WRITE);
            gameRoom.updateGameState(GameState.START);

            Game game = gameRoom.getCurrentGame();
            log.info("게임 ID: {}", game.getId());

            performRoundStartTimer.record(() -> performRoundStart(game));
            Card card = getCardForPlayer(game, email);

            int round = game.getRound();
            Turn turn = gameTurnService.getTurn(game.getId());
            int myPoint = getPlayerPoints(game, email);
            int otherPoint = getOpponentPoints(game, email);

            return new StartRoundResponse("ACTION", round, game.getPlayerOne(), game.getPlayerTwo(), card, turn, game.getBetAmount(), game.getPot(), myPoint, otherPoint);
        });
    }

    @Transactional
    public synchronized void performRoundStart(Game game) {
        log.info("게임 ID {}로 라운드 시작 작업 수행 중", game.getId());

        if (!game.isRoundStarted()) {
            game.incrementRound();
            game.updateRoundStarted();
            log.info("라운드 증가: {}", game.getRound());
        }

        handleInitialBet(game);
        handleCardAllocation(game);

        if (game.getRound() == 1) {
            initializeTurnForGame(game);
            log.info("첫 라운드에 턴 초기화");
        }
    }

    private int calculateInitialBet(User playerOne, User playerTwo) {
        int minPoints = Math.min(playerOne.getPoints(), playerTwo.getPoints());
        int bet = Math.max((int) Math.round(minPoints * 0.05), 1);
        return Math.min(bet, 2000);
    }

    private Card getCardForPlayer(Game game, String email) {
        return email.equals(game.getPlayerOne().getEmail()) ? game.getPlayerTwoCard() : game.getPlayerOneCard();
    }

    private int getPlayerPoints(Game game, String email) {
        return email.equals(game.getPlayerOne().getEmail()) ? game.getPlayerOne().getPoints() : game.getPlayerTwo().getPoints();
    }

    private int getOpponentPoints(Game game, String email) {
        return email.equals(game.getPlayerOne().getEmail()) ? game.getPlayerTwo().getPoints() : game.getPlayerOne().getPoints();
    }

    private void handleInitialBet(Game game) {
        if (game.getPot() == 0) {
            int betAmount = calculateInitialBet(game.getPlayerOne(), game.getPlayerTwo());
            log.info("초기 배팅금액: {}", betAmount);

            game.getPlayerOne().decreasePoints(betAmount);
            game.getPlayerTwo().decreasePoints(betAmount);

            game.setBetAmount(0);
            game.updatePot(betAmount * 2);
        }
    }

    private void handleCardAllocation(Game game) {
        if (!game.isCardAllocation()) {
            List<Card> availableCards = prepareAvailableCards(game);
            Collections.shuffle(availableCards);
            assignRandomCardsToPlayers(game, availableCards);

            log.info("플레이어에게 카드 할당됨: {} - {}, {} - {}",
                    game.getPlayerOne().getNickname(), game.getPlayerOneCard(), game.getPlayerTwo().getNickname(), game.getPlayerTwoCard());

            game.updateCardAllocation();
        }
    }

    private List<Card> prepareAvailableCards(Game game) {
        Set<Card> usedCards = game.getUsedCards();
        Set<Card> allCards = EnumSet.allOf(Card.class);
        allCards.removeAll(usedCards);
        return new ArrayList<>(allCards);
    }

    private void assignRandomCardsToPlayers(Game game, List<Card> availableCards) {
        game.setPlayerOneCard(availableCards.get(0));
        game.setPlayerTwoCard(availableCards.get(1));

        game.addUsedCard(availableCards.get(0));
        game.addUsedCard(availableCards.get(1));
    }

    private void initializeTurnForGame(Game game) {
        List<User> players = List.of(game.getPlayerOne(), game.getPlayerTwo());
        Turn turn = new Turn(players);
        gameTurnService.setTurn(game.getId(), turn);
    }
}
