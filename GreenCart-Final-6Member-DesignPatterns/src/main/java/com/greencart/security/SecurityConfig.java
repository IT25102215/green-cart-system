package com.greencart.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Set<String> MANAGEMENT_AUTHORITIES = Set.of(
            "ROLE_ADMIN",
            "ROLE_BUSINESS_OWNER",
            "ROLE_PRODUCT_MANAGER",
            "ROLE_ORDER_ADMIN",
            "ROLE_SUPPLIER_MANAGER",
            "ROLE_FEEDBACK_ADMIN"
    );

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filter(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/home", "/shop", "/shop/**", "/about", "/contact", "/contact/**",
                                "/login", "/register", "/forgot-password", "/reset-password",
                                "/css/**", "/js/**", "/images/**", "/uploads/**", "/webjars/**",
                                "/error", "/categories", "/my-account", "/offers",
                                "/track-order", "/store-locator")
                        .permitAll()

                        .requestMatchers("/admin/products/**", "/admin/categories/**", "/admin/inventory/**")
                        .hasAnyRole("ADMIN", "BUSINESS_OWNER", "PRODUCT_MANAGER")
                        .requestMatchers("/admin/orders/**")
                        .hasAnyRole("ADMIN", "BUSINESS_OWNER", "ORDER_ADMIN")
                        .requestMatchers("/admin/deliveries", "/admin/deliveries/**")
                        .hasAnyRole("ADMIN", "BUSINESS_OWNER", "DELIVERY_MANAGER")
                        .requestMatchers("/admin/suppliers/**", "/admin/supplier-products/**", "/admin/purchase-orders/**")
                        .hasAnyRole("ADMIN", "BUSINESS_OWNER", "SUPPLIER_MANAGER")
                        .requestMatchers("/admin/feedback/**", "/admin/complaints/**", "/admin/inquiries/**",
                                "/admin/reviews/**", "/admin/site-reviews/**", "/admin/contact-messages/**")
                        .hasAnyRole("ADMIN", "BUSINESS_OWNER", "FEEDBACK_ADMIN")
                        .requestMatchers("/admin/users/**", "/admin/income/**", "/admin/audit-logs/**")
                        .hasAnyRole("ADMIN", "BUSINESS_OWNER")
                        .requestMatchers("/admin/dashboard", "/admin/visit-site", "/admin/exit-site-preview")
                        .hasAnyRole("ADMIN", "BUSINESS_OWNER", "PRODUCT_MANAGER", "ORDER_ADMIN",
                                "SUPPLIER_MANAGER", "FEEDBACK_ADMIN")
                        .requestMatchers("/admin/**")
                        .hasAnyRole("ADMIN", "BUSINESS_OWNER")

                        .requestMatchers("/delivery/**").hasRole("DELIVERY_PERSON")
                        .requestMatchers(
                                "/user/**", "/cart", "/cart/**", "/checkout", "/checkout/**",
                                "/orders", "/orders/**", "/profile", "/profile/**",
                                "/reviews", "/reviews/**", "/site-reviews", "/site-reviews/**", "/payment/**")
                        .hasRole("USER")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            boolean management = authentication.getAuthorities().stream()
                                    .anyMatch(granted -> MANAGEMENT_AUTHORITIES.contains(granted.getAuthority()));
                            boolean deliveryManager = authentication.getAuthorities().stream()
                                    .anyMatch(granted -> granted.getAuthority().equals("ROLE_DELIVERY_MANAGER"));
                            boolean delivery = authentication.getAuthorities().stream()
                                    .anyMatch(granted -> granted.getAuthority().equals("ROLE_DELIVERY_PERSON"));
                            response.sendRedirect(deliveryManager
                                    ? "/admin/deliveries"
                                    : (management
                                        ? "/admin/dashboard"
                                        : (delivery ? "/delivery/dashboard" : "/")));
                        })
                        .failureUrl("/login?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1))
                .authenticationProvider(authProvider());

        return http.build();
    }
}
