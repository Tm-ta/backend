package com.example.tmta.auth.verification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.MessageRejectedException;
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
                [틈타] 서비스 가입을 위한 인증 코드입니다.

                안녕하세요, 틈타입니다.

                본인 확인을 위해 아래 인증 코드를 입력 창에 입력해 주세요.

                인증 번호: %s

                * 본 인증 코드는 3분간 유효합니다.
                * 인증 유효 시간이 지났을 경우, '재전송' 버튼을 눌러 다시 시도해 주세요.
                * 본인이 요청한 것이 아니라면 본 메일을 즉시 삭제해 주시기 바랍니다.

                감사합니다.
                틈타 팀 드림
                """.formatted(code);
        String htmlBody = """
                <h2>[틈타] 서비스 가입을 위한 인증 코드입니다.</h2>
                <p>안녕하세요, <strong>틈타</strong>입니다.</p>
                <p>본인 확인을 위해 아래 인증 코드를 입력 창에 입력해 주세요.</p>
                <blockquote>
                  <h3>인증 번호: <strong>%s</strong></h3>
                </blockquote>
                <ul>
                  <li>본 인증 코드는 <strong>3분간</strong> 유효합니다.</li>
                  <li>인증 유효 시간이 지났을 경우, '재전송' 버튼을 눌러 다시 시도해 주세요.</li>
                  <li>본인이 요청한 것이 아니라면 본 메일을 즉시 삭제해 주시기 바랍니다.</li>
                </ul>
                <p>감사합니다.<br/><strong>틈타 팀 드림</strong></p>
                """.formatted(code);

        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(sesMailProperties.getFromAddress())
                .destination(Destination.builder().toAddresses(email).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder().data("[틈타] 서비스 가입을 위한 인증 코드입니다.").charset("UTF-8").build())
                                .body(Body.builder()
                                        .text(Content.builder().data(textBody).charset("UTF-8").build())
                                        .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                        .build())
                                .build())
                        .build())
                .build();

        try {
            sesV2Client.sendEmail(request);
        } catch (MessageRejectedException e) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_REJECTED);
        }
    }
}
