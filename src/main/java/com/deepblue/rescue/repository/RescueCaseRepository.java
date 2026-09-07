package com.deepblue.rescue.repository;


import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RescueCaseRepository extends JpaRepository<RescueCase, Long> {

    Optional<RescueCase> findByCaseCode(String caseCode);

    List<RescueCase> findAllByStatusOrderByRescueDateAsc(RescueStatus status);

    List<RescueCase> findAllByRescueCenter_Code(String code);

    List<RescueCase> findAllByRescueCenter_CodeAndStatus(String code, RescueStatus status);

    List<RescueCase> findAllByRescueDateAfterOrderByRescueDateDesc(LocalDateTime date);

}