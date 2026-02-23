package com.example.tmta.auth.verification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

@Component
@Profile("prod")
@ConditionalOnProperty(prefix = "tmta.email.ses", name = "enabled", havingValue = "true")
public class SesEmailVerificationSender implements EmailVerificationSender {

    private final SesV2Client sesV2Client;
    private final SesMailProperties sesMailProperties;

    public SesEmailVerificationSender(SesV2Client sesV2Client, SesMailProperties sesMailProperties) {
        this.sesV2Client = sesV2Client;
        this.sesMailProperties = sesMailProperties;
    }

    @Override
    public void sendVerificationCode(String email, String code) {
        String textBody = """
                TMTA 이메일 인증 코드입니다.

                인증 코드: %s
                5분 내에 입력해주세요.
                """.formatted(code);

        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(sesMailProperties.getFromAddress())
                .destination(Destination.builder().toAddresses(email).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder().data("[TMTA] 이메일 인증 코드").charset("UTF-8").build())
                                .body(Body.builder()
                                        .text(Content.builder().data(textBody).charset("UTF-8").build())
                                        .build())
                                .build())
                        .build())
                .build();

        sesV2Client.sendEmail(request);
    }
}
