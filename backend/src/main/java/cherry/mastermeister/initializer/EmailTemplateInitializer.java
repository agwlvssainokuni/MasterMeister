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

package cherry.mastermeister.initializer;

import cherry.mastermeister.entity.EmailTemplate;
import cherry.mastermeister.repository.EmailTemplateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EmailTemplateInitializer implements ApplicationRunner {

    private final EmailTemplateRepository emailTemplateRepository;
    private final boolean initializeEmailTemplates;
    private final String defaultSenderEmail;
    private final String defaultSenderName;

    public EmailTemplateInitializer(
            EmailTemplateRepository emailTemplateRepository,
            @Value("${mm.mail.templates.initialize:true}") boolean initializeEmailTemplates,
            @Value("${mm.mail.from:noreply@mastermeister.local}") String defaultSenderEmail,
            @Value("${mm.mail.sender-name:MasterMeister}") String defaultSenderName
    ) {
        this.emailTemplateRepository = emailTemplateRepository;
        this.initializeEmailTemplates = initializeEmailTemplates;
        this.defaultSenderEmail = defaultSenderEmail;
        this.defaultSenderName = defaultSenderName;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (initializeEmailTemplates) {
            initializeDefaultTemplates();
        }
    }

    private void initializeDefaultTemplates() {
        initializeTemplate(EmailTemplate.TemplateType.EMAIL_CONFIRMATION, "en", 
                "MasterMeister - Email Confirmation Required",
                """
                Dear {username},
                
                Thank you for registering with MasterMeister.
                
                To complete your registration, please click the link below to confirm your email address:
                {confirmationUrl}
                
                If you did not register for this account, please ignore this email.
                
                Best regards,
                MasterMeister Team
                """);

        initializeTemplate(EmailTemplate.TemplateType.EMAIL_CONFIRMATION, "ja", 
                "MasterMeister - メールアドレス確認が必要です",
                """
                {username} 様
                
                MasterMeisterへのご登録ありがとうございます。
                
                登録を完了するため、以下のリンクをクリックしてメールアドレスを確認してください：
                {confirmationUrl}
                
                このメールに心当たりがない場合は、無視してください。
                
                MasterMeisterチーム
                """);

        initializeTemplate(EmailTemplate.TemplateType.EMAIL_CONFIRMED, "en", 
                "MasterMeister - Email Address Confirmed",
                """
                Dear {username},
                
                Your email address has been successfully confirmed.
                
                Your account is now pending administrator approval.
                You will receive another notification once your account has been approved.
                
                Best regards,
                MasterMeister Team
                """);

        initializeTemplate(EmailTemplate.TemplateType.EMAIL_CONFIRMED, "ja", 
                "MasterMeister - メールアドレス確認完了",
                """
                {username} 様
                
                メールアドレスの確認が完了しました。
                
                現在、アカウントは管理者の承認待ちとなっております。
                承認完了後に改めてお知らせいたします。
                
                MasterMeisterチーム
                """);

        initializeTemplate(EmailTemplate.TemplateType.ACCOUNT_APPROVED, "en", 
                "MasterMeister - Account Approved",
                """
                Dear {username},
                
                Your MasterMeister account has been approved by the administrator.
                
                You can now log in and start using the application.
                
                Best regards,
                MasterMeister Team
                """);

        initializeTemplate(EmailTemplate.TemplateType.ACCOUNT_APPROVED, "ja", 
                "MasterMeister - アカウント承認完了",
                """
                {username} 様
                
                管理者によりアカウントが承認されました。
                
                ログインしてアプリケーションをご利用いただけます。
                
                MasterMeisterチーム
                """);

        initializeTemplate(EmailTemplate.TemplateType.ACCOUNT_REJECTED, "en", 
                "MasterMeister - Account Registration Rejected",
                """
                Dear {username},
                
                We regret to inform you that your MasterMeister account registration has been rejected by the administrator.
                
                If you have any questions, please contact the administrator.
                
                Best regards,
                MasterMeister Team
                """);

        initializeTemplate(EmailTemplate.TemplateType.ACCOUNT_REJECTED, "ja", 
                "MasterMeister - アカウント登録却下",
                """
                {username} 様
                
                申し訳ございませんが、管理者によりアカウント登録が却下されました。
                
                ご質問がございましたら管理者にお問い合わせください。
                
                MasterMeisterチーム
                """);
    }

    private void initializeTemplate(EmailTemplate.TemplateType templateType, String languageCode, String subject, String body) {
        if (!emailTemplateRepository.existsByTemplateTypeAndLanguageCode(templateType, languageCode)) {
            EmailTemplate template = new EmailTemplate();
            template.setTemplateType(templateType);
            template.setLanguageCode(languageCode);
            template.setSenderEmail(defaultSenderEmail);
            template.setSenderName(defaultSenderName);
            template.setSubject(subject);
            template.setBody(body);
            emailTemplateRepository.save(template);
        }
    }
}