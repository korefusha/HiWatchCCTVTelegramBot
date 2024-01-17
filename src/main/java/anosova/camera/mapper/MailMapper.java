package anosova.camera.mapper;

import anosova.camera.model.Mail;
import jakarta.mail.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
@Component
@Slf4j
public final class MailMapper {

    public static Mail map(Message message) {
        Mail mail;
        try {
            log.debug("Extract mail complete");
            mail = extractMail(message);
        } catch (Exception e) {
            log.error("Exception in map() - {} /n {}", e.getMessage(), e.getStackTrace());
            throw new RuntimeException(e);
        }
        return mail;
    }

    public static Mail extractMail(Message message) throws Exception {
        Mail mail = new Mail();
        Multipart messageMultipart;
        try {
            messageMultipart = convertToMultipart(message);
            fillMailFromMimeMessage(mail, message);
            getAttachments(mail, messageMultipart);
            return mail;
        } catch (Exception e) {
            log.error("Exception in extractMail - {}", e.getMessage());
            throw new Exception();
        }
    }

    public static Multipart convertToMultipart(Message message) throws MessagingException, IOException {
        Multipart multipartMessage;
        try {
            log.debug("Convert mail to Multipart successful = {}", message.getContentType());
            multipartMessage = (Multipart) message.getContent();
            log.debug("mimeMessage contentType = {}", multipartMessage.getContentType());
            return multipartMessage;
        } catch (MessagingException | RuntimeException | IOException e) {
            log.error("Exception in convertToMimeMessage - {}" + e.getMessage());
            throw new MessagingException();
        }
    }

    public static void fillMailFromMimeMessage(Mail mail, Message message) throws Exception {
        String subject = message.getSubject();
        Address[] addressFrom = message.getFrom();
        Address[] addressTo = message.getRecipients(Message.RecipientType.TO);
        Date date = message.getSentDate();
        String from = (addressFrom != null ? addressFrom[0].toString() : null);
        String to = (addressTo != null ? addressTo[0].toString() : null);
        mail.setSubject(subject != null ? subject : "No Subject");
        mail.setFromAddress(from != null ? from : "No FromAddress");
        mail.setFromName(from != null ? from : "No From");
        mail.setToAddress(to != null ? to : "No ToAddress");
        mail.setToName(to != null ? to : "No To");
        mail.setAttachments(new ArrayList<>());
        mail.setDate(date);
//        getAttachments(mail, inputMiMeMessage);
//        InternetAddress[] toAddresses = InternetAddress.parse(InternetAddress.toString(inputMiMeMessage.getAllRecipients()));
//        mail.setToAddress(InternetAddress.toString(toAddresses));
//        mail.setToName(toAddresses[0].getPersonal());
        log.debug("Filling mail complete");
    }

    public static void getAttachments(Mail mail, Multipart multipart) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
                    StringUtils.isBlank(bodyPart.getFileName())) {
                continue; // dealing with attachments only
            }
            InputStream is = bodyPart.getInputStream();
            // -- EDIT -- SECURITY ISSUE --
            // do not do this in production code -- a malicious email can easily contain this filename: "../etc/passwd", or any other path: They can overwrite _ANY_ file on the system that this code has write access to!
            if (bodyPart.getFileName().matches(".*\\.jpg")) {
                File file = new File("src/tmp/" + bodyPart.getFileName());
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buf = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buf)) != -1) {
                    fos.write(buf, 0, bytesRead);
                }
                fos.close();
                mail.getAttachments().add(file);
            }

        }
    }

/*    public void getText(Mail mail, Multipart multipart) throws MessagingException {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if(bodyPart.getContentType().matches("");
            if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
                    StringUtils.isBlank(bodyPart.getFileName())) {
                continue; // dealing with attachments only
            }
    }*/
}



