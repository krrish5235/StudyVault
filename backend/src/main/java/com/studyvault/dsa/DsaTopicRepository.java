package com.studyvault.dsa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DsaTopicRepository extends JpaRepository<DsaTopic, Integer> {

    List<DsaTopic> findAllByOrderBySortOrderAsc();
}
