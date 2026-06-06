package com.ec.mokshitha_collections.config;

import com.ec.mokshitha_collections.security.CsrfCookieFilter;
import com.ec.mokshitha_collections.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Symmetric key used to sign remember-me cookies. Must be set in any
     * non-dev environment via REMEMBER_ME_KEY — the dev default is fine
     * locally but useless if leaked to prod.
     */
    @Value("${app.remember-me.key:dev-only-replace-in-prod-at-least-32-chars}")
    private String rememberMeKey;

    /** Days the remember-me cookie/token stays valid (default 14). */
    @Value("${app.remember-me.validity-days:14}")
    private int rememberMeValidityDays;

    /** Set under prod profile so we emit Strict-Transport-Security and require HTTPS cookies. */
    @Value("${app.security.hsts-enabled:false}")
    private boolean hstsEnabled;

    /** BCrypt with strength 12 — slow enough to resist GPU attacks. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** Exposes the AuthenticationManager so AuthController can drive login. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    /** Persistent (DB-backed) repository for remember-me series + tokens. */
    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        // Table schema is provided by PersistentLogin entity via Hibernate ddl.
        return repo;
    }

    /**
     * Persistent-token remember-me: single-use tokens rotated every request,
     * stored server-side. Cookie holds opaque series:token only — never
     * credentials. Compromised cookies invalidate themselves on next use.
     */
    @Bean
    public RememberMeServices rememberMeServices(CustomUserDetailsService uds,
                                                 PersistentTokenRepository tokenRepository) {
        PersistentTokenBasedRememberMeServices services =
                new PersistentTokenBasedRememberMeServices(rememberMeKey, uds, tokenRepository);
        services.setParameter("rememberMe");
        services.setCookieName("MC_REMEMBER_ME");
        services.setTokenValiditySeconds(rememberMeValidityDays * 24 * 60 * 60);
        services.setUseSecureCookie(false); // dev: HTTP. Prod profile flips this via Phase-7 polish.
        return services;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RememberMeServices rememberMeServices) throws Exception {

        // CSRF: cookie-based so the AJAX frontend can read XSRF-TOKEN and
        // echo it back as X-XSRF-TOKEN. HttpOnly is intentionally false on
        // the cookie (token, not the session), which is the documented pattern.
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null); // resolve only when accessed

        http
            .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfRepo)
                    .csrfTokenRequestHandler(csrfHandler))
            .authorizeHttpRequests(auth -> auth
                    // Public pages — /user_redirect is a router that itself
                    // decides whether to render the account or login page.
                    .requestMatchers(
                            "/", "/home", "/shop", "/about", "/contact",
                            "/login_register", "/user_redirect", "/product-detail/**",
                            "/error", "/favicon.ico"
                    ).permitAll()
                    // Static assets + uploaded images
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/static/**", "/webjars/**", "/uploads/**").permitAll()
                    // Auth endpoints (login/register/logout/check-session)
                    .requestMatchers("/auth/**").permitAll()
                    // Public catalog browsing
                    .requestMatchers(org.springframework.http.HttpMethod.GET,
                            "/api/products/**", "/api/categories/**", "/api/pincode/**").permitAll()
                    // Admin area (REST + any future page routes)
                    .requestMatchers("/api/admin/**", "/admin/**").hasRole("ADMIN")
                    // Authenticated user areas (page and REST routes both gated)
                    .requestMatchers(
                            "/account/**", "/wishlist/**",
                            "/cart/**", "/api/cart/**",
                            "/orders/**", "/api/orders/**",
                            "/checkout/**"
                    ).authenticated()
                    .anyRequest().authenticated())
            // Disable defaults we replace
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            // Return 401 JSON for unauthenticated AJAX rather than a redirect to /login
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .sessionManagement(s -> s
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .sessionFixation(sf -> sf.migrateSession())
                    .maximumSessions(1)
                    .maxSessionsPreventsLogin(false))
            .rememberMe(rm -> rm
                    .rememberMeServices(rememberMeServices)
                    .key(rememberMeKey))
            .headers(h -> {
                h.frameOptions(f -> f.deny())
                 .contentTypeOptions(c -> {})
                 .referrerPolicy(r -> r.policy(
                         org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN));
                if (hstsEnabled) {
                    // 1-year HSTS, applied to subdomains. Only enable under prod where you actually
                    // serve over HTTPS — turning this on over HTTP locks browsers out of the dev site.
                    h.httpStrictTransportSecurity(hsts -> hsts
                            .maxAgeInSeconds(31_536_000L)
                            .includeSubDomains(true));
                } else {
                    h.httpStrictTransportSecurity(hsts -> hsts.disable());
                }
            })
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }
}
