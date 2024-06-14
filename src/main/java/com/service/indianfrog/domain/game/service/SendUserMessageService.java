package com.service.indianfrog.domain.game.service;

import com.service.indianfrog.domain.game.dto.GameResponseDto.*;
import com.service.indianfrog.domain.game.dto.SendUserDto.EndGameInfo;
import com.service.indianfrog.domain.game.dto.SendUserDto.EndRoundInfo;
import com.service.indianfrog.domain.game.dto.SendUserDto.GameInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
@Slf4j
public class SendUserMessageService {

    private final SimpMessageSendingOperations messagingTemplate;

    public SendUserMessageService(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendUserEndRoundMessage(EndRoundResponse response, Principal principal) {
        logUserInfo(principal);
        log.info("Player's Card: {}", response.myCard());

        sendMessage(principal, "/queue/endRoundInfo", new EndRoundInfo(
                response.nowState(),
                response.nextState(),
                response.round(),
                response.winner().getNickname(),
                response.loser().getNickname(),
                response.roundPot(),
                response.myCard(),
                response.otherCard(),
                response.winnerPoint(),
                response.loserPoint()
        ));
    }

    public void sendUserEndGameMessage(EndGameResponse response, Principal principal) {
        logUserInfo(principal);

        sendMessage(principal, "/queue/endGameInfo", new EndGameInfo(
                response.nowState(),
                response.nextState(),
                response.gameWinner().getNickname(),
                response.gameLoser().getNickname(),
                response.winnerPot(),
                response.loserPot()
        ));
    }

    public void sendUserGameMessage(StartRoundResponse response, Principal principal) {
        logUserInfo(principal);
        log.info("Game state: {}, Turn: {}", response.gameState(), response.turn().toString());

        sendMessage(principal, "/queue/gameInfo", new GameInfo(
                response.otherCard(),
                response.turn(),
                response.firstBet(),
                response.roundPot(),
                response.round(),
                response.myPoint(),
                response.otherPoint()
        ));
    }

    private void logUserInfo(Principal principal) {
        log.info("Who are you? -> {}", principal.getName());
    }

    private void sendMessage(Principal principal, String destination, Object message) {
        try {
            messagingTemplate.convertAndSendToUser(principal.getName(), destination, message);
            log.info("Message sent successfully to {}", principal.getName());
        } catch (Exception e) {
            log.error("Failed to send message to {}", principal.getName(), e);
        }
    }
}
