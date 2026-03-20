package com.flowcollect.api.v1.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.flowcollect.api.v1.auth.dto.LoginRequest;
import com.flowcollect.api.v1.auth.dto.LoginResponse;
import com.flowcollect.api.v1.auth.dto.RegisterRequest;
import com.flowcollect.api.v1.auth.dto.RegisterResponse;
import com.flowcollect.api.v1.auth.dto.ResendVerificationRequest;
import com.flowcollect.api.v1.auth.dto.VerifyPhoneRequest;
import com.flowcollect.application.auth.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Called when the user clicks the verification link in their email.
     * Returns a JWT so the user is immediately logged in after verifying.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<LoginResponse> verifyEmail(@RequestParam String token) {
        LoginResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification-email")
    public ResponseEntity<Void> resendVerificationEmail(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerificationEmail(request.getOrganizationId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-phone")
    public ResponseEntity<Void> verifyPhone(@Valid @RequestBody VerifyPhoneRequest request) {
        authService.verifyPhone(request.getOrganizationId(), request.getOtp());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-phone-otp")
    public ResponseEntity<Void> resendPhoneOtp(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendPhoneOtp(request.getOrganizationId());
        return ResponseEntity.noContent().build();
    }
}
