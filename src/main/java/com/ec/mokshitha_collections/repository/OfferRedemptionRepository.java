package com.ec.mokshitha_collections.repository;

import com.ec.mokshitha_collections.entity.OfferRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRedemptionRepository extends JpaRepository<OfferRedemption, Long> {

    /** How many times this customer has already used the given offer. */
    long countByOfferIdAndUserId(Long offerId, Long userId);
}
