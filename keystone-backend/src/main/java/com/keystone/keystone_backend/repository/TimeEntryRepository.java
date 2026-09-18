package com.keystone.keystone_backend.repository;

import com.keystone.keystone_backend.entity.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findByWorkOrderId(Long workOrderId);

    @Query("SELECT COALESCE(SUM(te.minutes), 0) FROM TimeEntry te WHERE te.workOrder.id = :workOrderId")
    Integer sumMinutesByWorkOrderId(Long workOrderId);
}