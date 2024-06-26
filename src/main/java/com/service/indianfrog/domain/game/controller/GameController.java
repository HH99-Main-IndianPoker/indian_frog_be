package com.service.indianfrog.domain.game.controller;

import com.service.indianfrog.domain.game.dto.*;
import com.service.indianfrog.domain.game.dto.GameDto.EndGameResponse;
import com.service.indianfrog.domain.game.dto.GameDto.EndRoundResponse;
import com.service.indianfrog.domain.game.dto.GameDto.StartRoundResponse;
import com.service.indianfrog.domain.game.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Tag(name = "게임 실행 컨트롤러", description = "인디언 포커 게임 실행 및 종료 컨트롤러입니다.")
@Slf4j
@Controller
public class GameController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final StartGameService startGameService;
    private final GamePlayService gamePlayService;
    private final EndGameService endGameService;
    private final ReadyService readyService;

    public GameController(SimpMessageSendingOperations messagingTemplate,
                          StartGameService startGameService, GamePlayService gamePlayService,
                          EndGameService endGameService, ReadyService readyService) {
        this.messagingTemplate = messagingTemplate;
        this.startGameService = startGameService;
        this.gamePlayService = gamePlayService;
        this.endGameService = endGameService;
        this.readyService = readyService;
    }

    /* pub 사용 게임 준비 */
    @MessageMapping("/gameRoom/{gameRoomId}/ready")
    public void gameReady(
            @DestinationVariable Long gameRoomId, Principal principal) {
        log.info("게임 준비 - 게임방 아이디 : {}", gameRoomId);
        GameStatus gameStatus = readyService.gameReady(gameRoomId, principal);
        String destination = "/topic/gameRoom/" + gameRoomId;
        messagingTemplate.convertAndSend(destination, gameStatus);
    }

    @MessageMapping("/gameRoom/{gameRoomId}/{gameState}")
    public void handleGameState(@DestinationVariable Long gameRoomId, @DestinationVariable String gameState,
                                @Payload(required = false) GameBetting gameBetting, Principal principal) {

        log.info("gameState -> {}", gameState);

        switch (gameState) {
            case "START" -> {
                StartRoundResponse response = startGameService.startRound(gameRoomId, principal.getName());
                sendUserGameMessage(response, principal); // 유저별 메시지 전송
            }
            case "ACTION" -> {
                ActionDto response = gamePlayService.playerAction(gameRoomId, gameBetting, gameBetting.getAction());
                String destination = "/topic/gameRoom/" + gameRoomId;
                messagingTemplate.convertAndSend(destination, response);
            }
            case "END" -> {
                EndRoundResponse response = endGameService.endRound(gameRoomId, principal.getName());
                sendUserEndRoundMessage(response, principal);
            }
            case "GAME_END" -> {
                EndGameResponse response = endGameService.endGame(gameRoomId, principal.getName());
                sendUserEndGameMessage(response, principal);
            }

            default -> throw new IllegalStateException("Invalid game state: " + gameState);
        }
    }

    private void sendUserEndRoundMessage(EndRoundResponse response, Principal principal) {

        log.info("who are you? -> {}", principal.getName());
        log.info("player's Card : {}", response.getMyCard());

       try {
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/endRoundInfo", new EndRoundInfo(
                        response.getNowState(),
                        response.getNextState(),
                        response.getRound(),
                        response.getRoundWinner().getNickname(),
                        response.getRoundLoser().getNickname(),
                        response.getRoundPot(),
                        response.getMyCard(),
                        response.getOtherCard(),
                        response.getWinnerPoint(),
                        response.getLoserPoint()));
                log.info("Message sent successfully.");
            }
            catch (Exception e) {
            log.error("Failed to send message", e);
        }

    }

    private void sendUserEndGameMessage(EndGameResponse response, Principal principal) {

        log.info("who are you? -> {}", principal.getName());

        try {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/endGameInfo", new EndGameInfo(
                        response.getNowState(),
                        response.getNextState(),
                        response.getGameWinner().getNickname(),
                        response.getGameLoser().getNickname(),
                        response.getWinnerPot(),
                        response.getLoserPot()));
                log.info("Message sent successfully.");
        } catch (Exception e) {
            log.error("Failed to send message", e);
        }
    }

    private void sendUserGameMessage(StartRoundResponse response, Principal principal) {
        /* 각 Player 에게 상대 카드 정보와 턴 정보를 전송*/
        log.info("who are you? -> {}", principal.getName());
        log.info(response.getGameState(), response.getTurn().toString());
        String playerOne = response.getPlayerOne().getEmail();
        String playerTwo = response.getPlayerTwo().getEmail();
        try {
            if (principal.getName().equals(playerOne)) {
                messagingTemplate.convertAndSendToUser(playerOne, "/queue/gameInfo", new GameInfo(
                        response.getOtherCard(),
                        response.getTurn(),
                        response.getFirstBet(),
                        response.getRoundPot(),
                        response.getRound(),
                        response.getMyPoint(),
                        response.getOtherPoint()));
                log.info("Message sent successfully.");
            }

            if (principal.getName().equals(playerTwo)) {
                messagingTemplate.convertAndSendToUser(playerTwo, "/queue/gameInfo", new GameInfo(
                        response.getOtherCard(),
                        response.getTurn(),
                        response.getFirstBet(),
                        response.getRoundPot(),
                        response.getRound(),
                        response.getMyPoint(),
                        response.getOtherPoint()));
                log.info("Message sent successfully.");
            }
        } catch (Exception e) {
            log.error("Failed to send message", e);
        }
    }

}