package com.flowcollect.application.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.User;
import com.flowcollect.domain.verification.EmailVerificationToken;
import com.flowcollect.domain.verification.PasswordResetToken;
import com.flowcollect.domain.verification.PhoneOtp;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.config.NotificationEmailProperties;
import com.flowcollect.infrastructure.config.TwilioProperties;
import com.flowcollect.infrastructure.email.ResendEmailClient;
import com.flowcollect.infrastructure.persistence.verification.EmailVerificationTokenJpaRepository;
import com.flowcollect.infrastructure.persistence.verification.PasswordResetTokenJpaRepository;
import com.flowcollect.infrastructure.persistence.verification.PhoneOtpJpaRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

/**
 * Handles email and phone verification for newly registered organizations.
 *
 * Email: generates a UUID token, emails a verification link, and marks the user
 * ACTIVE once they verify. Token expires in 24 hours.
 *
 * Phone (optional): generates a 6-digit OTP, sends it via SMS, and marks the
 * org phone as verified once the user submits the correct code. OTP expires in 10 minutes.
 */
@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);
    private static final int EMAIL_TOKEN_HOURS = 24;
    private static final int RESET_TOKEN_HOURS = 1;
    private static final int PHONE_OTP_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationTokenJpaRepository emailTokenRepository;
    private final PasswordResetTokenJpaRepository resetTokenRepository;
    private final PhoneOtpJpaRepository phoneOtpRepository;
    private final ResendEmailClient emailClient;
    private final NotificationEmailProperties emailProperties;
    private final TwilioProperties twilioProperties;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String appFrontendUrl;

    public VerificationService(
            EmailVerificationTokenJpaRepository emailTokenRepository,
            PasswordResetTokenJpaRepository resetTokenRepository,
            PhoneOtpJpaRepository phoneOtpRepository,
            ResendEmailClient emailClient,
            NotificationEmailProperties emailProperties,
            TwilioProperties twilioProperties
    ) {
        this.emailTokenRepository = emailTokenRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.phoneOtpRepository = phoneOtpRepository;
        this.emailClient = emailClient;
        this.emailProperties = emailProperties;
        this.twilioProperties = twilioProperties;
    }

    // -------------------------------------------------------------------------
    // Email verification
    // -------------------------------------------------------------------------

    @Transactional
    public void sendVerificationEmail(User user) {
        emailTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(EMAIL_TOKEN_HOURS, ChronoUnit.HOURS);
        emailTokenRepository.save(new EmailVerificationToken(user, token, expiresAt));

        sendEmailVerificationMail(user.getEmail(), user.getName(), token);
        log.info("Sent verification email to {}", user.getEmail());
    }

    @Transactional
    public User validateEmailToken(String token) {
        EmailVerificationToken evt = emailTokenRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException("Invalid or expired verification link"));

        if (evt.isUsed()) {
            throw new ValidationException("This verification link has already been used");
        }
        if (evt.isExpired()) {
            throw new ValidationException("This verification link has expired. Please request a new one");
        }

        evt.markUsed();
        emailTokenRepository.save(evt);
        return evt.getUser();
    }

    // -------------------------------------------------------------------------
    // Phone OTP
    // -------------------------------------------------------------------------

    @Transactional
    public void sendPhoneOtp(Organization organization, String phone) {
        phoneOtpRepository.deleteByOrganizationId(organization.getId());

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(PHONE_OTP_MINUTES, ChronoUnit.MINUTES);
        phoneOtpRepository.save(new PhoneOtp(organization, phone, otp, expiresAt));

        sendSmsOtp(phone, otp, organization.getName());
        log.info("Sent phone OTP to {} for org {}", phone, organization.getId());
    }

    @Transactional
    public String validatePhoneOtp(UUID organizationId, String submittedOtp) {
        PhoneOtp phoneOtp = phoneOtpRepository
                .findTopByOrganizationIdAndUsedFalseOrderByCreatedAtDesc(organizationId)
                .orElseThrow(() -> new ValidationException("No pending phone verification found. Please request a new OTP"));

        if (phoneOtp.isExpired()) {
            throw new ValidationException("OTP has expired. Please request a new one");
        }
        if (!phoneOtp.getOtp().equals(submittedOtp)) {
            throw new ValidationException("Invalid OTP");
        }

        phoneOtp.markUsed();
        phoneOtpRepository.save(phoneOtp);
        return phoneOtp.getPhone();
    }

    // -------------------------------------------------------------------------
    // Password reset
    // -------------------------------------------------------------------------

    @Transactional
    public void sendPasswordResetEmail(User user) {
        resetTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(RESET_TOKEN_HOURS, ChronoUnit.HOURS);
        resetTokenRepository.save(new PasswordResetToken(user, token, expiresAt));

        sendPasswordResetMail(user.getEmail(), user.getName(), token);
        log.info("Sent password reset email to {}", user.getEmail());
    }

    @Transactional
    public User validatePasswordResetToken(String token) {
        PasswordResetToken prt = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException("Invalid or expired password reset link"));

        if (prt.isUsed()) {
            throw new ValidationException("This reset link has already been used");
        }
        if (prt.isExpired()) {
            throw new ValidationException("This reset link has expired. Please request a new one");
        }

        prt.markUsed();
        resetTokenRepository.save(prt);
        return prt.getUser();
    }

    // -------------------------------------------------------------------------
    // Private senders
    // -------------------------------------------------------------------------

    private void sendEmailVerificationMail(String to, String name, String token) {
        if (!emailClient.isConfigured()) {
            log.warn("Resend not configured — skipping verification email to {}", to);
            return;
        }
        String fromAddress = emailProperties.getAuthFromAddress();
        String fromName    = emailProperties.getFromName();
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("notification.email.auth-from-address not set — skipping verification email");
            return;
        }

        String verifyUrl = appFrontendUrl + "/verify-email?token=" + token;
        String html = buildVerificationEmailHtml(name, verifyUrl, fromName);

        try {
            emailClient.send(fromName + " <" + fromAddress + ">", to,
                    "Verify your " + fromName + " account", html);
        } catch (Exception ex) {
            throw new InternalException("Failed to send verification email: " + ex.getMessage());
        }
    }

    private void sendPasswordResetMail(String to, String name, String token) {
        if (!emailClient.isConfigured()) {
            log.warn("Resend not configured — skipping password reset email to {}", to);
            return;
        }
        String fromAddress = emailProperties.getAuthFromAddress();
        String fromName    = emailProperties.getFromName();
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("notification.email.auth-from-address not set — skipping password reset email");
            return;
        }

        String resetUrl = appFrontendUrl + "/reset-password?token=" + token;
        String html = buildPasswordResetEmailHtml(name, resetUrl, fromName);

        try {
            emailClient.send(fromName + " <" + fromAddress + ">", to,
                    "Reset your " + fromName + " password", html);
        } catch (Exception ex) {
            throw new InternalException("Failed to send password reset email: " + ex.getMessage());
        }
    }

    private void sendSmsOtp(String phone, String otp, String orgName) {
        if (!twilioProperties.isEnabled()) {
            log.warn("Twilio disabled — skipping OTP SMS to {}", phone);
            return;
        }
        try {
            Twilio.init(twilioProperties.getAccountSid(), twilioProperties.getAuthToken());
            Message.creator(
                    new PhoneNumber(phone.trim()),
                    new PhoneNumber(twilioProperties.getSmsFrom()),
                    orgName + " verification code: " + otp + ". Valid for " + PHONE_OTP_MINUTES + " minutes. Do not share this code."
            ).create();
        } catch (Exception ex) {
            throw new InternalException("Failed to send OTP: " + ex.getMessage());
        }
    }

    private String buildPasswordResetEmailHtml(String name, String resetUrl, String appName) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f4f5;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr><td align="center" style="padding:40px 20px;">
                      <table cellpadding="0" cellspacing="0"
                             style="width:100%%;max-width:560px;background:#ffffff;border-radius:8px;padding:36px;
                                    box-shadow:0 1px 4px rgba(0,0,0,.08);">
                        <tr><td style="font-size:15px;line-height:1.7;color:#374151;">
                          <p>Hi <strong>%s</strong>,</p>
                          <p>We received a request to reset your <strong>%s</strong> password. Click the button below to choose a new one.</p>
                          <p style="text-align:center;margin:32px 0;">
                            <a href="%s"
                               style="background:#2563eb;color:#ffffff;padding:12px 28px;
                                      border-radius:6px;text-decoration:none;font-weight:bold;
                                      font-size:15px;display:inline-block;">
                              Reset Password
                            </a>
                          </p>
                          <p style="color:#6b7280;font-size:13px;">
                            This link expires in 1 hour. If you did not request a password reset, you can safely ignore this email.
                          </p>
                          <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0;">
                          <p style="color:#9ca3af;font-size:12px;">
                            If the button doesn't work, copy and paste this link:<br>
                            <span style="color:#2563eb;word-break:break-all;">%s</span>
                          </p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(name, appName, resetUrl, resetUrl);
    }

    private String buildVerificationEmailHtml(String name, String verifyUrl, String appName) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f4f5;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr><td align="center" style="padding:40px 20px;">
                      <table cellpadding="0" cellspacing="0"
                             style="width:100%%;max-width:560px;background:#ffffff;border-radius:8px;padding:36px;
                                    box-shadow:0 1px 4px rgba(0,0,0,.08);">
                        <tr><td style="font-size:15px;line-height:1.7;color:#374151;">
                          <p>Hi <strong>%s</strong>,</p>
                          <p>Thanks for signing up for <strong>%s</strong>. Please verify your email address to activate your account.</p>
                          <p style="text-align:center;margin:32px 0;">
                            <a href="%s"
                               style="background:#2563eb;color:#ffffff;padding:12px 28px;
                                      border-radius:6px;text-decoration:none;font-weight:bold;
                                      font-size:15px;display:inline-block;">
                              Verify Email Address
                            </a>
                          </p>
                          <p style="color:#6b7280;font-size:13px;">
                            This link expires in 24 hours. If you did not create an account, you can safely ignore this email.
                          </p>
                          <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0;">
                          <p style="color:#9ca3af;font-size:12px;">
                            If the button doesn't work, copy and paste this link:<br>
                            <span style="color:#2563eb;word-break:break-all;">%s</span>
                          </p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(name, appName, verifyUrl, verifyUrl);
    }
}
