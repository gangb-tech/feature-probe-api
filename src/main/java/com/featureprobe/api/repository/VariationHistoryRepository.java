package com.featureprobe.api.repository;

import com.featureprobe.api.entity.VariationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VariationHistoryRepository extends JpaRepository<VariationHistory, Long>,
        JpaSpecificationExecutor<VariationHistory> {


    List<VariationHistory> findByProjectKeyAndEnvironmentKeyAndToggleKey(String projectKey,
                                                                         String environmentKey, String toggleKey);
}
