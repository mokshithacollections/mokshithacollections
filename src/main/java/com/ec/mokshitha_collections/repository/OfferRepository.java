package com.ec.mokshitha_collections.repository;

import com.ec.mokshitha_collections.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    Optional<Offer> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    /** Admin list — highest priority first, then newest. */
    List<Offer> findAllByOrderByPriorityDescOfferIdDesc();

    /**
     * Active offers that haven't ended yet — i.e. LIVE or SCHEDULED. Used by the
     * home banner. Ordered so the highest-priority / soonest-starting shows first.
     */
    List<Offer> findByActiveTrueAndEndAtAfterOrderByPriorityDescStartAtAsc(LocalDateTime now);
}
