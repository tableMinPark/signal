package com.ssafysignal.api.signalweek.repository;

import com.ssafysignal.api.signalweek.entity.Signalweek;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SignalweekRepository extends JpaRepository<Signalweek, Integer> {
    Optional<Signalweek> findBySignalweekSeq(Integer signalweekSeq);

    Optional<Signalweek> findByProject(Integer projectSeq);

    Page<Signalweek> findByTitleContaining(String keyword, PageRequest signalweekSeq);
}