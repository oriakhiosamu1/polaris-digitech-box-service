package com.joyboy.polaris_digitech.repository;

import com.joyboy.polaris_digitech.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByBoxTxref(String txref);

    Optional<Item> findByCode(String code);

    boolean existsByCode(String code);
}