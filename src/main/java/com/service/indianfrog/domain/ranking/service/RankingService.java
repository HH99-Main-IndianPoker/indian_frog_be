package com.service.indianfrog.domain.ranking.service;

import com.service.indianfrog.domain.ranking.dto.Ranking.*;
import com.service.indianfrog.domain.user.entity.User;
import com.service.indianfrog.domain.user.repository.UserRepository;
import com.service.indianfrog.global.exception.ErrorCode;
import com.service.indianfrog.global.exception.RestApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class RankingService {

    private final UserRepository userRepository;

    public RankingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public GetRankingInfo getRanking(String username) {

        List<Object[]> results = userRepository.findUsersWithRank();

        List<GetRanking> rankings = results.stream()
                .map(result -> new GetRanking(
                        (String) result[0], // imageUrl
                        ((Long) result[1]).intValue(), // ranking
                        (String) result[2], // nickname
                        (Integer) result[3]  // points
                ))
                .toList();

        User user = userRepository.findByEmail(username).orElseThrow(() -> new RestApiException(ErrorCode.NOT_FOUND_USER.getMessage()));

        List<User> userList = userRepository.findAll();

        int myRanking = IntStream.range(0, userList.size()).filter(i -> userList.get(i).getEmail().equals(username)).findFirst().orElseThrow(() -> new RestApiException(ErrorCode.NOT_FOUND_EMAIL.getMessage())) + 1;

        return new GetRankingInfo(rankings, user.getNickname(), user.getImageUrl(),myRanking, user.getPoints());

    }



}
