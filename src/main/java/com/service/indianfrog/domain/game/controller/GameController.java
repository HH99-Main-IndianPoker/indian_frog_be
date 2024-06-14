package com.service.indianfrog.domain.game.controller;

import com.service.indianfrog.domain.game.dto.ActionDto;
import com.service.indianfrog.domain.game.dto.GameRequestDto.*;
import com.service.indianfrog.domain.game.dto.GameResponseDto.*;
import com.service.indianfrog.domain.game.service.*;
import com.service.indianfrog.domain.game.utils.ConcurrencyControlService;
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
    private final SendUserMessageService sendUserMessageService;
    private final ConcurrencyControlService concurrencyControlService;

    public GameController(SimpMessageSendingOperations messagingTemplate,
                          StartGameService startGameService, GamePlayService gamePlayService,
                          EndGameService endGameService, ReadyService readyService,
                          SendUserMessageService sendUserMessageService, ConcurrencyControlService concurrencyControlService) {
        this.messagingTemplate = messagingTemplate;
        this.startGameService = startGameService;
        this.gamePlayService = gamePlayService;
        this.endGameService = endGameService;
        this.readyService = readyService;
        this.sendUserMessageService = sendUserMessageService;
        this.concurrencyControlService = concurrencyControlService;
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

        if (concurrencyControlService.tryAcquireLock(gameRoomId, principal.getName())) {
            try {
                switch (gameState) {
                    case "START" -> {
                        StartRoundResponse response = startGameService.startRound(gameRoomId, principal.getName());
                        sendUserMessageService.sendUserGameMessage(response, principal); // 유저별 메시지 전송
                    }
                    case "ACTION" -> {
                        ActionDto response = gamePlayService.playerAction(gameRoomId, gameBetting, gameBetting.action());
                        String destination = "/topic/gameRoom/" + gameRoomId;
                        messagingTemplate.convertAndSend(destination, response);
                    }
                    case "END" -> {
                        EndRoundResponse response = endGameService.endRound(gameRoomId, principal.getName());
                        sendUserMessageService.sendUserEndRoundMessage(response, principal);
                    }
                    case "GAME_END" -> {
                        EndGameResponse response = endGameService.endGame(gameRoomId, principal.getName());
                        sendUserMessageService.sendUserEndGameMessage(response, principal);
                    }
                    default -> throw new IllegalStateException("Invalid game state: " + gameState);
                }
            } finally {
                concurrencyControlService.releaseLock(gameRoomId, principal.getName());
            }
        } else {
            log.warn("동시성 제어 - 다른 요청이 처리 중입니다.");
        }
    }
}