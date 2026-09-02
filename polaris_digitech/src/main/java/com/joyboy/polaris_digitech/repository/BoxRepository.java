package com.joyboy.polaris_digitech.repository;

import com.joyboy.polaris_digitech.model.Box;
import com.joyboy.polaris_digitech.model.BoxState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoxRepository extends JpaRepository<Box, Long> {

    Optional<Box> findByTxref(String txref);

    boolean existsByTxref(String txref);

    // Find boxes that are available for loading
    // (IDLE or LOADING) + battery >= 25
    @Query("SELECT b FROM Box b WHERE b.state IN :states AND b.batteryCapacity >= 25")
    List<Box> findAvailableBoxes(List<BoxState> states);
}