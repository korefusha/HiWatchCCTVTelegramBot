package anosova.camera.scheduler;

import anosova.camera.service.MailReceiveClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MailReceiveScheduler {

    private final MailReceiveClient mailReceiveClient;
    public MailReceiveScheduler(MailReceiveClient mailReceiveClient){
        this.mailReceiveClient = mailReceiveClient;
    }

    @Scheduled(fixedDelayString = "3000")
    public void receiveMails() {
        mailReceiveClient.receive();
    }
}
