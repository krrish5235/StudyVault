package com.studyvault.dsa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DsaProgressRepository extends JpaRepository<DsaProgress, DsaProgress.Pk> {

    Optional<DsaProgress> findByUserIdAndQuestionId(UUID userId, Integer questionId);

    @Query("select p.questionId from DsaProgress p where p.user.id = :userId and p.done = true")
    List<Integer> findDoneQuestionIds(@Param("userId") UUID userId);
}
