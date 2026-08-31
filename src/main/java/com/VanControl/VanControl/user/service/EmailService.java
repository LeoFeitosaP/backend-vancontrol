package com.VanControl.VanControl.user.service;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void enviarEmailToken(String para, String nomeUsuario, String pin) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom("vancontrol77@gmail.com");
        message.setTo(para);
        message.setSubject("VanControl - Recuperação de Senha");

        String corpoEmail = String.format(
                "Olá, %s!\n\n" +
                        "Recebemos uma solicitação para redefinir a sua senha no VanControl.\n" +
                        "Use o código de verificação abaixo para prosseguir no aplicativo:\n\n" +
                        "%s\n\n" +
                        "Este código é válido por 15 minutos. Se você não solicitou essa alteração, ignore este e-mail.\n\n" +
                        "Abraços,\nEquipe VanControl.",
                nomeUsuario, pin
        );

        message.setText(corpoEmail);
        mailSender.send(message);
    }
}
