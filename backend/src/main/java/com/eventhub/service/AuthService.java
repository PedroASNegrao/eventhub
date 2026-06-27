package com.eventhub.service;

import com.eventhub.domain.User;
import com.eventhub.dto.LoginRequestDTO;
import com.eventhub.dto.RefreshRequestDTO;
import com.eventhub.dto.TokenResponseDTO;
import com.eventhub.exception.InvalidCredentialsException;
import com.eventhub.repository.UserRepository;
import com.eventhub.security.JwtService;
import com.eventhub.security.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues JWT access/refresh token pairs for valid credentials, and rotates them
 * on refresh.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * @throws InvalidCredentialsException if the email is unknown or the password doesn't match
     */
    @Transactional(readOnly = true)
    public TokenResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return buildTokenResponse(user);
    }

    /**
     * @throws InvalidCredentialsException if the refresh token is invalid, expired, not of
     *                                     refresh type, or its owner no longer exists
     */
    @Transactional(readOnly = true)
    public TokenResponseDTO refresh(RefreshRequestDTO dto) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(dto.refreshToken());
        } catch (JwtException e) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        if (!TokenType.REFRESH.name().equals(claims.get("type", String.class))) {
            throw new InvalidCredentialsException("Token is not a refresh token");
        }

        User user = userRepository.findByEmail(claims.getSubject())
                .orElseThrow(() -> new InvalidCredentialsException("User no longer exists"));

        return buildTokenResponse(user);
    }

    private TokenResponseDTO buildTokenResponse(User user) {
        return new TokenResponseDTO(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000
        );
    }
}
