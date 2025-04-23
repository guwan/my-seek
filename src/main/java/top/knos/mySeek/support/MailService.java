package top.knos.mySeek.support;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Properties;

@Service
public class MailService {
    Logger logger = LoggerFactory.getLogger(MailService.class);
    @Value("${mail.send.mail}")
    private String email;// 发件人邮箱
    @Value("${mail.send.token}")
    private String token;// 激活码
    @Value("${mail.send.host}")
    private String host;// 地址
    JavaMailSenderImpl mailSender;

    @PostConstruct
    public void init() throws GeneralSecurityException {
        mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(465);
        mailSender.setUsername(email);
        mailSender.setPassword(token);
        mailSender.setProtocol("smtp");
        Properties properties = mailSender.getJavaMailProperties();
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.debug", "false");
        properties.setProperty("mail.smtp.timeout", "0");
        properties.setProperty("mail.smtp.socketFactory.class", "jakarta.net.ssl.SSLSocketFactory");
        properties.setProperty("mail.smtp.socketFactory.port", "465");
    }

    public void send(String recipient, String mailContent, String subject) throws MessagingException {
        //使用JavaMail的MimeMessage，支付更加复杂的邮件格式和内容
        MimeMessage msg = mailSender.createMimeMessage();
//创建MimeMessageHelper对象，处理MimeMessage的辅助类
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());
        //使用辅助类MimeMessage设定参数
        helper.setFrom(mailSender.getUsername());
        helper.setTo(recipient);
        helper.setSubject(subject);
        helper.setText(mailContent,true);
        //加载文件资源，作为附件
        //ClassPathResource file = new ClassPathResource("Chrysanthemum.jpg");
        //加入附件
        //helper.addAttachment("attachment.jpg", file);
        // 发送邮件
        mailSender.send(msg);
    }

    public void sendSimpleMail(String recipient, String mailContent, String subject) throws MessagingException {
        SimpleMailMessage smm = new SimpleMailMessage();
        smm.setFrom(mailSender.getUsername());
        smm.setTo(recipient);
        smm.setSubject(subject);
        smm.setText(mailContent);
        mailSender.send(smm);
    }
}
