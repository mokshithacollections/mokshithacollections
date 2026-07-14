package com.ec.mokshitha_collections.controller;

import com.ec.mokshitha_collections.dto.offer.OfferBannerResponse;
import com.ec.mokshitha_collections.service.OfferService;
import com.ec.mokshitha_collections.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final OfferService offerService;

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("featuredProducts", productService.getFeaturedProducts());
        // LIVE + SCHEDULED offers for the announcement banner (empty list = banner hidden).
        List<OfferBannerResponse> banners = offerService.listActiveBanners();
        model.addAttribute("offerBanners", banners);
        // Subset with promo artwork → shown as clickable slides in the hero slider.
        model.addAttribute("heroOffers", banners.stream()
                .filter(b -> b.getBannerImageDesktop() != null && !b.getBannerImageDesktop().isBlank())
                .toList());
        return "home";
    }
}
