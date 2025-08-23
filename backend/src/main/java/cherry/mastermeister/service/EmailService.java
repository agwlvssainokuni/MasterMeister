/*
 * Copyright 2025 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cherry.mastermeister.service;

import cherry.mastermeister.entity.EmailTemplateEntity;
import cherry.mastermeister.enums.TemplateType;
import cherry.mastermeister.repository.EmailTemplateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateRepository emailTemplateRepository;
    private final String baseUrl;
    private final String confirmationPath;
    private final String tokenParam;
    private final String defaultLanguage;

    public EmailService(
            JavaMailSender mailSender,
            EmailTemplateRepository emailTemplateRepository,
            @Value("${mm.app.base-url:http://localhost:8080}") String baseUrl,
            @Value("${mm.app.confirmation-path:/confirm-email}") String confirmationPath,
            @Value("${mm.app.token-param:token}") String tokenParam,
            @Value("${mm.mail.default-language:en}") String defaultLanguage
    ) {
        this.mailSender = mailSender;
        this.emailTemplateRepository = emailTemplateRepository;
        this.baseUrl = baseUrl;
        this.confirmationPath = confirmationPath;
        this.tokenParam = tokenParam;
        this.defaultLanguage = defaultLanguage;
    }

    public void sendEmailConfirmation(String toAddress, String confirmationToken, String language) {
        URI confirmationUrl = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path(confirmationPath)
                .queryParam(tokenParam, confirmationToken)
                .build()
                .toUri();

        Map<String, String> variables = Map.of(
                "username", toAddress,
                "confirmationUrl", confirmationUrl.toString()
        );

        sendTemplatedEmail(TemplateType.EMAIL_CONFIRMATION, toAddress, variables, language);
    }

    public void sendEmailConfirmed(String toAddress, String language) {
        Map<String, String> variables = Map.of("username", toAddress);
        sendTemplatedEmail(TemplateType.EMAIL_CONFIRMED, toAddress, variables, language);
    }

    public void sendAccountApproved(String toAddress, String language) {
        Map<String, String> variables = Map.of("username", toAddress);
        sendTemplatedEmail(TemplateType.ACCOUNT_APPROVED, toAddress, variables, language);
    }

    public void sendAccountRejected(String toAddress, String language) {
        Map<String, String> variables = Map.of("username", toAddress);
        sendTemplatedEmail(TemplateType.ACCOUNT_REJECTED, toAddress, variables, language);
    }

    private void sendTemplatedEmail(TemplateType templateType, String toAddress, Map<String, String> variables, String language) {
        String lang = language != null ? language : defaultLanguage;

        EmailTemplateEntity template = emailTemplateRepository
                .findByTemplateTypeAndLanguageCode(templateType, lang)
                .orElse(emailTemplateRepository
                        .findByTemplateTypeAndLanguageCode(templateType, defaultLanguage)
                        .orElseThrow(() -> new IllegalArgumentException("Email template not found: " + templateType + " for language: " + lang)));

        String subject = replaceVariables(template.getSubject(), variables);
        String body = replaceVariables(template.getBody(), variables);

        sendEmail(toAddress, subject, body, template.getSenderEmail(), template.getSenderName());
    }

    private String replaceVariables(String text, Map<String, String> variables) {
        String result = text;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private void sendEmail(String to, String subject, String text, String fromEmail, String fromName) {
        SimpleMailMessage message = new SimpleMailMessage();

        if (fromName != null && !fromName.trim().isEmpty()) {
            message.setFrom(fromName + " <" + fromEmail + ">");
        } else {
            message.setFrom(fromEmail);
        }

        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }
}
