package anosova.camera.service;

import anosova.camera.mapper.MailMapper;
import anosova.camera.model.Mail;
import jakarta.mail.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
@Slf4j
public final class MailReceiveClient {
    @Value("${mail.folder}")
    private String folderName;

    //    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MailReceiveClient.class);
    private final String protocol;
    private final String host;
    private final int port;

    private final boolean imapSSL;
    private final boolean imapAuth;
    private final String user;
    private final String password;

    //Constructor
    public MailReceiveClient(@Value("${mail.protocol}") String protocol,
                             @Value("${mail.imap.host}") String host,
                             @Value("${mail.port}") int port,
                             @Value("${mail.imap.ssl}") boolean imapSSL,
                             @Value("${mail.imap.auth}") boolean imapAuth,
                             @Value("${mail.user}") String user,
                             @Value("${mail.password}") String password) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.imapSSL = imapSSL;
        this.imapAuth = imapAuth;
        this.user = user;
        this.password = password;
    }

    //method Get new mail
    public List<Mail> receive() {
        Store emailStore = null;
        Folder emailFolder = null;
        Properties properties = new Properties();
        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", port);
        properties.put("mail.imap.ssl", imapSSL);

        properties.put("mail.imap.auth", imapAuth);
        properties.put("mail.imap.user", user);
        properties.put("mail.imap.pass", password);
        Session emailSession = Session.getDefaultInstance(properties, new ImapAuthenticator(user, password));


        try {
            log.debug("Connecting to mail");
            emailStore = emailSession.getStore(protocol);
            emailStore.connect(host, user, password);
            log.debug("Connected to mail successful");
            emailFolder = emailStore.getFolder(folderName);
            emailFolder.open(Folder.READ_WRITE);
            log.debug("Received mails");
            log.debug("Total emails = " + emailFolder.getMessageCount());
            return getNewMails(emailFolder);

        } catch (MessagingException e) {
            log.error("Exception " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                if (emailFolder != null && emailFolder.isOpen()) {
                    emailFolder.close(false);
                }
                if (emailStore != null && emailStore.isConnected()) {
                    emailStore.close();
                }
            } catch (MessagingException e) {
                log.error("Exception " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    private List<Mail> getNewMails(Folder emailFolder) throws MessagingException {
        List<Mail> mails = new ArrayList<>();
        log.debug("Start receiving new mails");
        for (Message message : emailFolder.getMessages()) {
            log.debug("In email folder {} mails", emailFolder.getMessageCount());
            Mail mail = MailMapper.map(message);
            mails.add(mail);
            message.setFlag(Flags.Flag.DELETED, true);
        }
        //Off only UNSEEN message
/*            if (!message.getFlags().contains(Flags.Flag.SEEN)) {
                log.info("Message flag is - {}", message.getFlags().toString());
                Mail mail = MailMapper.map(message);
                log.info("Mail subject is - {}", mail.getSubject());
                mails.add(mail);

                message.setFlags(new Flags(Flags.Flag.SEEN), true);
                log.info("Set flag Seen - {}",message.getFlags().toString());
                }
        }
        log.debug("Finish receiving new mails. Have " + mails.size() + " mails");*/
        return mails;
    }
}

