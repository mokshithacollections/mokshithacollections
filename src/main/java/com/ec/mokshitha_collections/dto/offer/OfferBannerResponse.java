package com.ec.mokshitha_collections.dto.offer;

import lombok.Builder;
import lombok.Getter;

/**
 * One slide of the home-page offer banner. The countdown is driven by
 * {@code secondsRemaining} computed on the server (the browser just ticks it
 * down), so it's immune to client-clock / timezone differences.
 */
@Getter
@Builder
public class OfferBannerResponse {
    private final Long offerId;
    private final String name;
    private final String code;
    private final String description;
    /** Pre-formatted, e.g. "25% OFF" or "₹500 OFF". */
    private final String discountLabel;
    /** "SCHEDULED" (counts down to start) or "LIVE" (counts down to end). */
    private final String mode;
    /** "Starts in" / "Ends in" — the countdown caption. */
    private final String countdownLabel;
    /** Whole seconds until the target moment (start for SCHEDULED, end for LIVE). */
    private final long secondsRemaining;
    /** Human validity window, shown beside the badge, e.g. "23 Jun → 30 Jun 2026". */
    private final String validityText;
    /** Optional promo artwork for the hero slider (null = no hero slide for this offer). */
    private final String bannerImageDesktop;
    private final String bannerImageMobile;
}
