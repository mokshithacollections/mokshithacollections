package com.ec.mokshitha_collections.controller;

import com.ec.mokshitha_collections.service.OfferService;
import com.ec.mokshitha_collections.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final OfferService offerService;

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("featuredProducts", productService.getFeaturedProducts());
        // LIVE + SCHEDULED offers for the announcement banner (empty list = banner hidden).
        model.addAttribute("offerBanners", offerService.listActiveBanners());
        return "home";
    }
}
