package com.ec.mokshitha_collections.repository;

import com.ec.mokshitha_collections.entity.Address;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<Address, Long> {

    @Modifying
    @Query("""
        UPDATE Address a
        SET a.isDefault = false
        WHERE a.user.userId = :userId
    """)
    void clearDefaultForUser(@Param("userId") Long userId);
}
