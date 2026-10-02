package com.studyvault.dsa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DsaQuestionRepository extends JpaRepository<DsaQuestion, Integer> {

    List<DsaQuestion> findAllByTopicIdOrderByPositionAsc(Integer topicId);

    List<DsaQuestion> findAllByOrderByTitleAsc();

    Optional<DsaQuestion> findBySlug(String slug);
}
