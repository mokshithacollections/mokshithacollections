package com.ec.mokshitha_collections.service;

import com.ec.mokshitha_collections.dto.contact.ContactRequest;
import com.ec.mokshitha_collections.dto.enquiry.EnquiryResponse;
import com.ec.mokshitha_collections.entity.Enquiry;
import com.ec.mokshitha_collections.entity.EnquiryStatus;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.EnquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;

    /** Save a new enquiry submitted via the public contact form (status = NEW). */
    @Transactional
    public void submit(ContactRequest req) {
        Enquiry e = Enquiry.builder()
                .name(safeTrim(req.getName()))
                .email(safeTrim(req.getEmail()))
                .phone(safeTrim(req.getPhone()))
                .subject(safeTrim(req.getSubject()))
                .message(safeTrim(req.getMessage()))
                .newsletter(Boolean.TRUE.equals(req.getNewsletter()))
                .status(EnquiryStatus.NEW)
                .build();
        enquiryRepository.save(e);
    }

    /** Admin list, newest first. null status = all. */
    @Transactional(readOnly = true)
    public List<EnquiryResponse> list(EnquiryStatus status) {
        List<Enquiry> items = (status == null)
                ? enquiryRepository.findAllByOrderByCreatedAtDesc()
                : enquiryRepository.findByStatusOrderByCreatedAtDesc(status);
        return items.stream().map(EnquiryService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return enquiryRepository.count();
    }

    @Transactional(readOnly = true)
    public long countByStatus(EnquiryStatus status) {
        return enquiryRepository.countByStatus(status);
    }

    @Transactional
    public void markSeen(Long enquiryId) {
        Enquiry e = enquiryRepository.findById(enquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found"));
        e.setStatus(EnquiryStatus.SEEN);
        enquiryRepository.save(e);
    }

    @Transactional
    public void delete(Long enquiryId) {
        if (!enquiryRepository.existsById(enquiryId)) {
            throw new ResourceNotFoundException("Enquiry not found");
        }
        enquiryRepository.deleteById(enquiryId);
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private static EnquiryResponse toResponse(Enquiry e) {
        return EnquiryResponse.builder()
                .enquiryId(e.getEnquiryId())
                .name(e.getName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .subject(e.getSubject())
                .message(e.getMessage())
                .newsletter(e.getNewsletter())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
