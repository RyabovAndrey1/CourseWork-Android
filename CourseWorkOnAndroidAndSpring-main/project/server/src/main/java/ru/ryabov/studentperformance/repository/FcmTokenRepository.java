package ru.ryabov.studentperformance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ryabov.studentperformance.entity.FcmToken;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    List<FcmToken> findByUserId(Long userId);

    List<FcmToken> findByUserIdIn(List<Long> userIds);

    Optional<FcmToken> findByUserIdAndToken(Long userId, String token);

    void deleteByUserIdAndToken(Long userId, String token);
}
