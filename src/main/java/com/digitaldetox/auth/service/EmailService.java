package com.digitaldetox.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Async
    public void sendVerificationEmail(String toEmail, String username, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("mydetoxapp@gmail.com", "DigitalDetox");
            helper.setTo(toEmail);
            helper.setSubject("Conferma la tua registrazione — DigitalDetox");

            String verifyUrl = baseUrl + "/api/auth/verify-email?token=" + token;

            String html = """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; background: #0E0C1E; color: #E2DEFF; border-radius: 16px; padding: 32px;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <div style="font-size: 48px;">🥚</div>
                        <h1 style="color: #C4B5FD; font-size: 22px; margin: 8px 0;">DigitalDetox</h1>
                    </div>
                    <h2 style="color: #E2DEFF; font-size: 18px;">Ciao %s! 👋</h2>
                    <p style="color: #8B83C0; line-height: 1.6;">
                        Grazie per esserti registrato su DigitalDetox. Clicca il pulsante qui sotto per confermare il tuo indirizzo email e attivare il tuo account.
                    </p>
                    <div style="text-align: center; margin: 32px 0;">
                        <a href="%s"
                           style="background: #7C5CFC; color: white; padding: 14px 32px; border-radius: 12px; text-decoration: none; font-weight: bold; font-size: 15px; display: inline-block;">
                            Conferma Email →
                        </a>
                    </div>
                    <p style="color: #8B83C0; font-size: 12px; text-align: center;">
                        Il link scade tra 24 ore. Se non hai creato un account, ignora questa email.
                    </p>
                    <hr style="border: 1px solid #221E3A; margin: 24px 0;">
                    <p style="color: #8B83C0; font-size: 11px; text-align: center;">
                        DigitalDetox — Riduci il tuo tempo sullo schermo 📱
                    </p>
                </div>
                """.formatted(username, verifyUrl);

            helper.setText(html, true);
            mailSender.send(message);

            log.info("Email di verifica inviata a: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Errore invio email a {}: {}", toEmail, e.getMessage());
        } catch (Exception e) {
            log.error("Errore generico invio email: {}", e.getMessage());
        }
    }
}