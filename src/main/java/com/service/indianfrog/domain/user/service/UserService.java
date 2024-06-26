package com.service.indianfrog.domain.user.service;

import com.service.indianfrog.domain.user.dto.UserRequestDto.*;
import com.service.indianfrog.domain.user.dto.UserResponseDto.*;
import com.service.indianfrog.domain.user.entity.User;
import com.service.indianfrog.domain.user.repository.UserRepository;
import com.service.indianfrog.global.exception.ErrorCode;
import com.service.indianfrog.global.exception.RestApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 회원가입
    @Transactional
    public SignupResponseDto signup(SignupUserRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.email())) {
            throw new RestApiException(ErrorCode.ALREADY_EXIST_EMAIL.getMessage());
        }

        if (userRepository.existsByNickname(requestDto.nickname())) {
            throw new RestApiException(ErrorCode.ALREADY_EXIST_NICKNAME.getMessage());
        }

        String password = passwordEncoder.encode(requestDto.password());
        User member = userRepository.save(requestDto.toEntity(password));
        LocalDateTime now = LocalDateTime.now();

        return new SignupResponseDto(member.getEmail(), now);
    }

    // 회원 정보 조회
    @Transactional(readOnly = true)
    public GetUserResponseDto getMember(String email) {
        User member = userRepository.findByEmail(email).orElseThrow(() ->
                new RestApiException(ErrorCode.NOT_FOUND_USER.getMessage()));

        return new GetUserResponseDto(member);
    }

    // 이메일 중복 체크
    public boolean emailCheck(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new RestApiException(ErrorCode.ALREADY_EXIST_EMAIL.getMessage());
        }
        return userRepository.existsByEmail(email);
    }

    // 닉네임 중복체크
    public boolean nicknameCheck(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new RestApiException(ErrorCode.ALREADY_EXIST_NICKNAME.getMessage());
        }
        return userRepository.existsByNickname(nickname);
    }

    public MyPoint getMyPoint(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
            () -> new RestApiException(ErrorCode.NOT_FOUND_USER.getMessage()));
        return new MyPoint(user.getPoints());
    }
}
