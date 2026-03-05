package ru.ryabov.studentperformance.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.ryabov.studentperformance.security.JwtAuthenticationFilter;
import ru.ryabov.studentperformance.security.UserDetailsServiceImpl;
import ru.ryabov.studentperformance.security.WebAuthFailureHandler;
import ru.ryabov.studentperformance.security.WebAuthSuccessHandler;
import ru.ryabov.studentperformance.security.WebLogoutHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private WebAuthSuccessHandler webAuthSuccessHandler;

    @Autowired
    private WebAuthFailureHandler webAuthFailureHandler;

    @Autowired
    private WebLogoutHandler webLogoutHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /** Веб-интерфейс: form login, сессия. Учитываем context-path /api: матчим /api/web/** и /web/** */
    @Bean
    @Order(1)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/web/**", "/web/**")
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/web/login", "/api/web/login?*", "/web/login", "/web/login?*").permitAll()
                        .requestMatchers("/api/web/error", "/web/error", "/error").permitAll()
                        .requestMatchers("/api/web/change-password", "/web/change-password").permitAll()
                        .requestMatchers("/api/web/forgot-password", "/web/forgot-password").permitAll()
                        .requestMatchers("/api/web/reset-password", "/web/reset-password", "/web/reset-password?*").permitAll()
                        .requestMatchers("/api/web/css/**", "/api/web/js/**", "/web/css/**", "/web/js/**").permitAll()
                        .requestMatchers("/api/web/**", "/web/**").hasAnyRole("ADMIN", "DEANERY", "TEACHER", "STUDENT"))
                .formLogin(f -> f
                        .loginPage("/web/login")
                        .loginProcessingUrl("/web/login")
                        .successHandler(webAuthSuccessHandler)
                        .failureHandler(webAuthFailureHandler))
                .logout(l -> l
                        .logoutUrl("/web/logout")
                        .logoutSuccessUrl("/web/login?logout")
                        .addLogoutHandler(webLogoutHandler)
                        .invalidateHttpSession(true))
                .authenticationProvider(authenticationProvider());
        return http.build();
    }

    /** REST API: JWT, без сессии (для мобильного приложения). */
    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/css/**", "/api/js/**", "/api/images/**", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/auth/**", "/health", "/health/**", "/error").permitAll()
                        .requestMatchers("/actuator/**", "/h2-console/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/deanery/**").hasAnyRole("ADMIN", "DEANERY")
                        .requestMatchers("/teacher/**").hasAnyRole("ADMIN", "DEANERY", "TEACHER")
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
