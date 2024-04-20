package com.service.indianfrog.domain.gameroom.repository;

import com.service.indianfrog.domain.gameroom.entity.GameRoom;
import com.service.indianfrog.domain.gameroom.entity.ValidateRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ValidateRoomRepository extends JpaRepository<ValidateRoom, Long> {

    Optional<ValidateRoom> findByGameRoomAndParticipants(GameRoom gameRoom, String participant);

    List<ValidateRoom> findAllByGameRoomRoomId(Long roomId);

    List<ValidateRoom> findAllByReadyTrue();

    List<ValidateRoom> findAllByParticipants(String email);

    ValidateRoom findByGameRoomRoomIdAndParticipants(Long roomId, String nickname);

    ValidateRoom findByGameRoomRoomId(Long roomId);

    int countByGameRoomRoomId(Long roomId);

}

