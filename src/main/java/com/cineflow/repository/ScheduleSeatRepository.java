package com.cineflow.repository;

import com.cineflow.domain.ScheduleSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ScheduleSeatRepository extends JpaRepository<ScheduleSeat, Long> {

    @EntityGraph(attributePaths = {"seatTemplate", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater"})
    List<ScheduleSeat> findByScheduleIdOrderBySeatTemplateSeatRowAscSeatTemplateSeatNumberAsc(Long scheduleId);

    @EntityGraph(attributePaths = {"seatTemplate"})
    List<ScheduleSeat> findByScheduleIdAndReservedFalseOrderBySeatTemplateSeatRowAscSeatTemplateSeatNumberAsc(Long scheduleId);

    @EntityGraph(attributePaths = {"seatTemplate"})
    List<ScheduleSeat> findByScheduleIdAndReservedTrueOrderBySeatTemplateSeatRowAscSeatTemplateSeatNumberAsc(Long scheduleId);

    @EntityGraph(attributePaths = {"seatTemplate"})
    List<ScheduleSeat> findByScheduleIdAndSeatTemplateSeatCodeIn(Long scheduleId, Collection<String> seatCodes);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ss
            from ScheduleSeat ss
            join fetch ss.seatTemplate st
            where ss.schedule.id = :scheduleId
              and st.seatCode in :seatCodes
            order by st.seatRow asc, st.seatNumber asc
            """)
    List<ScheduleSeat> findForUpdateByScheduleIdAndSeatCodes(
            @Param("scheduleId") Long scheduleId,
            @Param("seatCodes") Collection<String> seatCodes
    );

    long countByScheduleIdAndReservedFalse(Long scheduleId);

    long countByScheduleIdAndReservedTrue(Long scheduleId);

    void deleteByScheduleId(Long scheduleId);
}
