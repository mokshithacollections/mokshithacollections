package com.ec.mokshitha_collections.service.admin;

import com.ec.mokshitha_collections.dto.admin.OfferRequest;
import com.ec.mokshitha_collections.dto.offer.OfferResponse;
import com.ec.mokshitha_collections.entity.Offer;
import com.ec.mokshitha_collections.entity.OfferDiscountType;
import com.ec.mokshitha_collections.exception.BadRequestException;
import com.ec.mokshitha_collections.exception.ConflictException;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminOfferService {

    private final OfferRepository offerRepository;

    @Transactional(readOnly = true)
    public List<OfferResponse> list() {
        return offerRepository.findAllByOrderByPriorityDescOfferIdDesc()
                .stream().map(AdminOfferService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OfferResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public OfferResponse create(OfferRequest req) {
        String code = normalizeCode(req.getCode());
        if (offerRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("An offer with code '" + code + "' already exists");
        }
        validate(req);
        Offer o = new Offer();
        apply(o, req, code);
        return toResponse(offerRepository.save(o));
    }

    @Transactional
    public OfferResponse update(Long id, OfferRequest req) {
        Offer o = find(id);
        String code = normalizeCode(req.getCode());
        offerRepository.findByCodeIgnoreCase(code).ifPresent(other -> {
            if (!other.getOfferId().equals(id)) {
                throw new ConflictException("An offer with code '" + code + "' already exists");
            }
        });
        validate(req);
        apply(o, req, code);
        return toResponse(offerRepository.save(o));
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        Offer o = find(id);
        o.setActive(active);
        offerRepository.save(o);
    }

    @Transactional
    public void delete(Long id) {
        if (!offerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Offer not found");
        }
        offerRepository.deleteById(id);
    }

    /* ---------- helpers ---------- */

    private Offer find(Long id) {
        return offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));
    }

    private void validate(OfferRequest req) {
        if (req.getEndAt().isBefore(req.getStartAt()) || req.getEndAt().isEqual(req.getStartAt())) {
            throw new BadRequestException("End date/time must be after the start date/time");
        }
        if (req.getDiscountType() == OfferDiscountType.PERCENTAGE
                && req.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("A percentage discount can't exceed 100%");
        }
    }

    private void apply(Offer o, OfferRequest req, String code) {
        o.setName(req.getName().trim());
        o.setCode(code);
        o.setDescription(req.getDescription());
        o.setDiscountType(req.getDiscountType());
        o.setDiscountValue(req.getDiscountValue());
        o.setStartAt(req.getStartAt());
        o.setEndAt(req.getEndAt());
        o.setMinOrderAmount(req.getMinOrderAmount());
        o.setMaxDiscountAmount(req.getMaxDiscountAmount());
        o.setPerCustomerLimit(req.getPerCustomerLimit());
        o.setActive(req.getActive() == null ? Boolean.TRUE : req.getActive());
        o.setPriority(req.getPriority() == null ? 0 : req.getPriority());
        o.setProductIds(req.getProductIds() == null ? new HashSet<>() : new HashSet<>(req.getProductIds()));
        o.setCategoryIds(req.getCategoryIds() == null ? new HashSet<>() : new HashSet<>(req.getCategoryIds()));
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private static OfferResponse toResponse(Offer o) {
        return OfferResponse.builder()
                .offerId(o.getOfferId())
                .name(o.getName())
                .code(o.getCode())
                .description(o.getDescription())
                .discountType(o.getDiscountType())
                .discountValue(o.getDiscountValue())
                .startAt(o.getStartAt())
                .endAt(o.getEndAt())
                .minOrderAmount(o.getMinOrderAmount())
                .maxDiscountAmount(o.getMaxDiscountAmount())
                .perCustomerLimit(o.getPerCustomerLimit())
                .active(o.getActive())
                .priority(o.getPriority())
                .productIds(new HashSet<>(o.getProductIds()))     // force LAZY init inside tx
                .categoryIds(new HashSet<>(o.getCategoryIds()))
                .state(computeState(o))
                .build();
    }

    /** SCHEDULED | LIVE | EXPIRED | INACTIVE — for the admin badge. */
    private static String computeState(Offer o) {
        if (o.getActive() == null || !o.getActive()) return "INACTIVE";
        LocalDateTime now = LocalDateTime.now();
        if (o.getStartAt() != null && now.isBefore(o.getStartAt())) return "SCHEDULED";
        if (o.getEndAt() != null && now.isAfter(o.getEndAt())) return "EXPIRED";
        return "LIVE";
    }
}
