package com.service.indianfrog.global.config;

import com.service.indianfrog.domain.user.repository.UserRepository;
import com.service.indianfrog.global.jwt.JwtUtil;
import com.service.indianfrog.global.security.JwtAuthenticationFilter;
import com.service.indianfrog.global.security.JwtAuthorizationFilter;
import com.service.indianfrog.global.security.UserDetailsServiceImpl;
import com.service.indianfrog.global.security.oauth2.CustomOAuth2UserService;
import com.service.indianfrog.global.security.oauth2.MyAuthenticationFailureHandler;
import com.service.indianfrog.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.service.indianfrog.global.security.token.TokenBlacklistService;
import com.service.indianfrog.global.security.token.TokenService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@Slf4j
public class WebSecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler customSuccessHandler;
    private final MyAuthenticationFailureHandler authenticationFailureHandler;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    public WebSecurityConfig(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService,
        AuthenticationConfiguration authenticationConfiguration,
        CustomOAuth2UserService customOAuth2UserService,
        OAuth2AuthenticationSuccessHandler customSuccessHandler,
        MyAuthenticationFailureHandler authenticationFailureHandler, UserRepository userRepository,
        TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.authenticationConfiguration = authenticationConfiguration;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customSuccessHandler = customSuccessHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.userRepository = userRepository;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
        throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil);
        filter.setAuthenticationManager(authenticationManager(authenticationConfiguration));
        return filter;
    }

    @Bean
    public JwtAuthorizationFilter jwtAuthorizationFilter() {
        return new JwtAuthorizationFilter(jwtUtil, userDetailsService, userRepository,tokenBlacklistService);
    }

    // 시큐리티 CORS 설정
//    @Bean
//    CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        // 배포시 허용할 출처 추가하기
//        configuration.addAllowedOriginPattern("*");
//        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
//        configuration.addAllowedHeader("*");
//        configuration.setExposedHeaders(List.of("Authorization","Set-Cookie"));
//        configuration.setAllowCredentials(true);
//        configuration.setAllowedHeaders(Arrays.asList("Authorization", "TOKEN_ID", "X-Requested-With", "Content-Type", "Content-Length", "Cache-Control","Set-Cookie"));
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }

    public CorsConfigurationSource configurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedOriginPattern("https://indianfrog.com");
        configuration.addAllowedOriginPattern("http://localhost:3000");
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(
            List.of("Authorization", "Set-Cookie", "Cache-Control", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF 설정
        http.csrf((csrf) -> csrf.disable());

        // 시큐리티 CORS 빈 설정
        http.cors((cors) -> cors.configurationSource(configurationSource()));

        // JWT 방식을 사용하기 위한 설정
        http.sessionManagement((sessionManagement) ->
            sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        /*oauth2*/
        http
                .oauth2Login((oauth2) -> oauth2
                        .userInfoEndpoint((userInfoEndpointConfig) -> userInfoEndpointConfig
                                .userService(customOAuth2UserService))// OAuth2 로그인 성공 이후 사용자 정보를 가져올 때 설정 담당
//                        .failureHandler(authenticationFailureHandler)
                        .successHandler(customSuccessHandler));// OAuth2 로그인 성공 시, 후작업을 진행할 UserService 인터페이스 구현체 등록

        http.authorizeHttpRequests((authorizeHttpRequests) ->
                authorizeHttpRequests
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                    .permitAll() // resources 접근 허용 설정
                    .requestMatchers("/", "/user/**",
                        "/login/**", "/oauth2/**", "/token/**"
                        , "/**", "/indian-frog-management/prometheus/**", "/error/**", "/monitoring/grafana/**")
                    .permitAll() // 메인 페이지 요청 허가
                    .requestMatchers("/ws/**").permitAll() // WebSocket 경로 허가
                    .requestMatchers("/topic/**").permitAll() // WebSocket 메시지 브로커 경로 허가
//                        .requestMatchers("/app/**").permitAll()
//                        .requestMatchers("/user/queue/**").permitAll()
                    .requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
                    .anyRequest().authenticated() // 그 외 모든 요청 인증처리
        );

        http.formLogin(AbstractHttpConfigurer::disable);
        http
            .logout(logout -> logout
                .logoutUrl("/logout") // 로그아웃 처리할 URL 지정
                .logoutSuccessUrl("/") // 로그아웃 성공 시 리다이렉트할 URL
                .deleteCookies("refreshToken") // 쿠키 삭제 (예: 세션 쿠키)
                .clearAuthentication(true) // 인증 정보 클리어
            );

        // 필터 관리
        http.addFilterBefore(jwtAuthorizationFilter(), JwtAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        // 예외 핸들러
        //http.exceptionHandling(handler -> handler.authenticationEntryPoint(new CustomAuthenticationEntryPoint()));

        return http.build();
    }
}