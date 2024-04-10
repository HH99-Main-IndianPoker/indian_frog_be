package com.service.indianfrog.domain.gameroom.service;

import com.service.indianfrog.domain.gameroom.dto.GameRoomRequestDto.GameRoomCreateRequestDto;
import com.service.indianfrog.domain.gameroom.dto.GameRoomResponseDto.GameRoomCreateResponseDto;
import com.service.indianfrog.domain.gameroom.dto.GameRoomResponseDto.GetGameRoomResponseDto;
import com.service.indianfrog.domain.gameroom.dto.ValidateRoomDto;
import com.service.indianfrog.domain.gameroom.entity.GameRoom;
import com.service.indianfrog.domain.gameroom.entity.ValidateRoom;
import com.service.indianfrog.domain.gameroom.repository.GameRoomRepository;
import com.service.indianfrog.domain.gameroom.repository.ValidateRoomRepository;
import com.service.indianfrog.domain.user.repository.UserRepository;
import com.service.indianfrog.global.exception.ErrorCode;
import com.service.indianfrog.global.exception.RestApiException;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GameRoomService {

    private final GameRoomRepository gameRoomRepository;
    private final ValidateRoomRepository validateRoomRepository;
    private final UserRepository userRepository;
    private Set<String> badWords;
    private Pattern pattern;

    public GameRoomService(GameRoomRepository gameRoomRepository, ValidateRoomRepository validateRoomRepository, UserRepository userRepository) {
        this.gameRoomRepository = gameRoomRepository;
        this.validateRoomRepository = validateRoomRepository;
        this.userRepository = userRepository;
    }

    public GetGameRoomResponseDto getGameRoomById(Long roomId) {
        GameRoom gameRoom = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new RestApiException(ErrorCode.NOT_FOUND_GAME_ROOM.getMessage()));
        return new GetGameRoomResponseDto(gameRoom.getRoomId(), gameRoom.getRoomName());
    }

    public Page<GetGameRoomResponseDto> getAllGameRooms(Pageable pageable) {
        return gameRoomRepository.findAll(pageable).map(gameRoom -> new GetGameRoomResponseDto(gameRoom.getRoomId(), gameRoom.getRoomName()));
    }

    public void deleteGameRoom(Long roomId) {
        gameRoomRepository.deleteById(roomId);
    }

    public boolean existsById(Long roomId) {
        return gameRoomRepository.existsById(roomId);
    }


    @PostConstruct
    public void init() {
        try {
            pattern = Pattern.compile(
                    Files.lines(Paths.get(new ClassPathResource("bad-words.txt").getURI()))
                            .map(Pattern::quote)
                            .collect(Collectors.joining("|")),
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            );
        } catch (IOException e) {
            pattern = Pattern.compile(""); // 비어있는 패턴
        }
    }
    public String filterMessage(String message) {
        return (message == null || message.trim().isEmpty()) ? message : pattern.matcher(message).replaceAll("**");
    }


    @Transactional
    public GameRoomCreateResponseDto createGameRoom(GameRoomCreateRequestDto gameRoomDto, Principal principal) {
        String email = principal.getName();
        userRepository.findByEmail(email).orElseThrow(() -> new RestApiException(ErrorCode.NOT_FOUND_USER.getMessage()));

        LocalDateTime now = LocalDateTime.now();
        GameRoom savedGameRoom = gameRoomRepository.save(gameRoomDto.toEntity());

        ValidateRoom validateRoom = new ValidateRoom();
        validateRoom.setParticipants(email);
        validateRoom.setGameRoom(savedGameRoom);
        validateRoom.setHost(true);
        validateRoomRepository.save(validateRoom);

        return new GameRoomCreateResponseDto(savedGameRoom.getRoomId(), savedGameRoom.getRoomName(), now);
    }


    @Transactional
    public ValidateRoomDto addParticipant(Long roomId, Principal participant) {
        String email = participant.getName();
        userRepository.findByEmail(email)
                .orElseThrow(() -> new RestApiException(ErrorCode.NOT_FOUND_USER.getMessage()));

        GameRoom gameRoom = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new RestApiException(ErrorCode.NOT_FOUND_GAME_ROOM.getMessage()));

        if (gameRoom.getValidateRooms().size() >= 2) {
            throw new RestApiException(ErrorCode.GAME_ROOM_NOW_FULL.getMessage());
        }

        if (validateRoomRepository.findByGameRoomAndParticipants(gameRoom, email).isPresent()) {
            throw new RestApiException(ErrorCode.ALREADY_EXIST_USER.getMessage());
        }

        ValidateRoom validateRoom = new ValidateRoom();
        validateRoom.setParticipants(email);
        validateRoom.setGameRoom(gameRoom);
        validateRoom = validateRoomRepository.save(validateRoom);

        return new ValidateRoomDto(validateRoom.getValidId(), validateRoom.getParticipants());
    }

    @Transactional
    public GetGameRoomResponseDto removeParticipant(Long roomId, Principal participant) {
        String email = participant.getName();
        userRepository.findByEmail(email)
                .orElseThrow(() -> new RestApiException(ErrorCode.NOT_FOUND_USER.getMessage()));

        List<ValidateRoom> validateRooms = validateRoomRepository.findAllByGameRoomRoomIdAndParticipants(roomId, email);
        if (validateRooms.isEmpty()) {
            throw new RestApiException(ErrorCode.NOT_FOUND_USER.getMessage());
        }
        boolean wasHost = validateRooms.stream().anyMatch(ValidateRoom::isHost);
        validateRooms.forEach(validateRoomRepository::delete);

        if (wasHost) {
            // 방장이 방을 나가면 새 방장을 지정
            List<ValidateRoom> remainParticipant = validateRoomRepository.findAllByGameRoomRoomId(roomId);
            if (!remainParticipant.isEmpty()) {
                ValidateRoom newHost = remainParticipant.get(0); //남아있는 첫번째 참가자를 방장으로 지정, 만약 참가자가 여러명이면 거의 랜덤?
                newHost.setHost(true);
                validateRoomRepository.save(newHost);
            }
        }

        //방이 비었는지 검사해서 없으면 방 없애버림.
        boolean isRoomEmpty = !validateRoomRepository.existsByGameRoomRoomId(roomId);
        if (isRoomEmpty) {
            // 방이 비었으므로 삭제하고 null 반환
            gameRoomRepository.deleteById(roomId);
            return null;
        }

        GameRoom gameRoom = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new RestApiException(ErrorCode.NOT_FOUND_GAME_ROOM.getMessage()));

        return new GetGameRoomResponseDto(gameRoom.getRoomId(), gameRoom.getRoomName());
    }

}
