package com.ec.mokshitha_collections.repository;

import com.ec.mokshitha_collections.entity.Enquiry;
import com.ec.mokshitha_collections.entity.EnquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {

    List<Enquiry> findAllByOrderByCreatedAtDesc();

    List<Enquiry> findByStatusOrderByCreatedAtDesc(EnquiryStatus status);

    long countByStatus(EnquiryStatus status);
}
