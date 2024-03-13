package anosova.camera.service;

import anosova.camera.mapper.MailMapper;
import anosova.camera.model.Mail;
import jakarta.mail.*;
import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.event.MessageCountListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
@Slf4j
public final class MailReceiveClient {
    List<Mail> mails = new ArrayList<>();
    @Value("${mail.folder}")
    private String folderName;
    private final String protocol;
    private final String host;
    private final int port;

    private final boolean imapSSL;
    private final boolean imapAuth;
    private final String user;
    private final String password;

//    private MailMapper mailMapper;

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

        Store emailStore;

        Properties properties = new Properties();
        properties.put("mail.imap.host", host);
        properties.put("mail.imap.user", user);
        properties.put("mail.imap.pass", password);
        Session emailSession = Session.getDefaultInstance(properties, new ImapAuthenticator(user, password));
        try {
            log.debug("Connecting to mail");
            emailStore = emailSession.getStore(protocol);
            emailStore.connect(host, user, password);
            log.debug("Connected to mail successful");
            Folder emailFolder = emailStore.getFolder(folderName);
            emailFolder.open(Folder.READ_WRITE);
            log.info("Received mails from {}. Total emails = {}", emailFolder.getName(), emailFolder.getMessageCount());
            log.info("Start Mail listener");
            emailFolder.addMessageCountListener(new MessageCountListener() {
                @Override
                public void messagesAdded(MessageCountEvent messageCountEvent) {
                    //                        Message[] messages = emailFolder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
                    Message[] messages = messageCountEvent.getMessages();
                    log.info("New messages = {}", messages.length);
                    for (Message message : messages) {
                        Mail mail = MailMapper.map(message);
                        mails.add(mail);
                    }
                    log.info("Finish receiving new mails. Have " + mails.size() + " new mails");
                }

                @Override
                public void messagesRemoved(MessageCountEvent messageCountEvent) {
                    log.info("Message removed");
                }
            });

            while (true) {
                //Проверка новых сообщений каждые 5 секунд
                Thread.sleep(5000);
                emailFolder.getMessageCount();
            }
        } catch (Exception e) {
            log.error("Exception " + e.getMessage());
        }
    return mails;
    }
}