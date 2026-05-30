package com.switchplatform.platform.service.auth;

import com.switchplatform.platform.model.auth.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class MfaService {

    private static final int TOTP_PERIOD = 30;
    private static final int TOTP_DIGITS = 6;

    private final AuthUserService authUserService;

    public MfaSetupData setupMfa(String username) {
        AuthUser user = authUserService.findByUsername(username);
        if (user == null) throw new IllegalArgumentException("User not found");
        if (user.isMfaEnabled()) throw new IllegalStateException("MFA already enabled");

        byte[] secretBytes = new byte[20];
        new SecureRandom().nextBytes(secretBytes);
        String secret = new Base32().encodeToString(secretBytes).replaceAll("=", "");

        user.setMfaSecret(secret);
        authUserService.updateUser(user.getId(), null, user.getRole(), user.isEnabled());

        String issuer = "Switch+Platform";
        String uri = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                issuer, username, secret, issuer, TOTP_DIGITS, TOTP_PERIOD);

        log.info("MFA setup initiated for user: {}", username);
        return new MfaSetupData(secret, uri);
    }

    public boolean verifyAndEnable(String username, String code) {
        AuthUser user = authUserService.findByUsername(username);
        if (user == null) throw new IllegalArgumentException("User not found");
        if (user.getMfaSecret() == null) throw new IllegalStateException("MFA not set up");

        if (!verifyCode(user.getMfaSecret(), code)) return false;

        user.setMfaEnabled(true);
        authUserService.updateUser(user.getId(), null, user.getRole(), user.isEnabled());
        log.info("MFA enabled for user: {}", username);
        return true;
    }

    public boolean disableMfa(String username, String code) {
        AuthUser user = authUserService.findByUsername(username);
        if (user == null) throw new IllegalArgumentException("User not found");
        if (!verifyCode(user.getMfaSecret(), code)) return false;

        user.setMfaSecret(null);
        user.setMfaEnabled(false);
        authUserService.updateUser(user.getId(), null, user.getRole(), user.isEnabled());
        log.info("MFA disabled for user: {}", username);
        return true;
    }

    public boolean validateCode(String username, String code) {
        AuthUser user = authUserService.findByUsername(username);
        if (user == null || !user.isMfaEnabled() || user.getMfaSecret() == null) return false;
        return verifyCode(user.getMfaSecret(), code);
    }

    private boolean verifyCode(String secret, String code) {
        try {
            long counter = Instant.now().getEpochSecond() / TOTP_PERIOD;
            for (int i = -1; i <= 1; i++) {
                String expected = generateTOTP(secret, counter + i);
                if (expected.equals(code)) return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("MFA verification failed: {}", e.getMessage());
            return false;
        }
    }

    private String generateTOTP(String secret, long counter) throws Exception {
        Base32 base32 = new Base32();
        byte[] key = base32.decode(secret);

        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (counter & 0xff);
            counter >>= 8;
        }

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0xf;
        int binary = ((hash[offset] & 0x7f) << 24) |
                     ((hash[offset + 1] & 0xff) << 16) |
                     ((hash[offset + 2] & 0xff) << 8) |
                     (hash[offset + 3] & 0xff);

        int otp = binary % (int) Math.pow(10, TOTP_DIGITS);
        return String.format("%0" + TOTP_DIGITS + "d", otp);
    }

    public boolean isMfaEnabled(String username) {
        AuthUser user = authUserService.findByUsername(username);
        return user != null && user.isMfaEnabled();
    }

    public record MfaSetupData(String secret, String uri) {}
}
