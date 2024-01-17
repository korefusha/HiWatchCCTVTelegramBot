package anosova.camera.service;

import anosova.camera.config.BotConfig;
import anosova.camera.model.Mail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot{
    final BotConfig botConfig;

    final MailReceiveClient mailReceiveClient;
    String path = "C:\\Users\\rebel\\Desktop\\camera\\src\\main\\resources\\img\\image_2023-12-30_16-50-04.png";

    //Constructor
    public TelegramBot(BotConfig botConfig, MailReceiveClient mailReceiveClient){
        this.botConfig = botConfig;
        this.mailReceiveClient = mailReceiveClient;
    }


    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {

            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (message) {
                case "/start":
                        startCommandReceived(chatId, update.getMessage().getFrom().getFirstName());
                        break;
                case "/test":
                    testMessage(chatId,message);
                    break;
                case "/check":
                    getMail(chatId,message);
                    break;
                case "/photo":
                    if (mailReceiveClient.receive().size() != 0) {
                        for (Mail mail : mailReceiveClient.receive()) {
                            try {
                                sendPhoto(chatId, mail);
                            } catch (TelegramApiException e) {
                                log.error("Exception in onUpdateReceived.sendPhoto - {}", e.getMessage());
                                throw new RuntimeException(e);
                            }
                        }
                    } else {
                        sendMessage(chatId, "No mails");
                    }
                    break;
                default:
                    sendMessage(chatId, "Sorry, try again later");
            }

        }

    }

    private void startCommandReceived (long chatId,String name){
        String answer = "Hi " + name + ", nice to meet you!";
        sendMessage(chatId, answer);
    }

    private void testMessage(long chatId, String text) {
        String answer = "This is a new test post created by bot";
        String comment = "This is new comment";
        sendMessage(chatId,answer + "\n" + comment);
    }

    private void sendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
            System.out.println(e.getMessage());
        }
    }

    private void sendPhoto(long chatId, Mail mail) throws TelegramApiException {
        //file example if attachment can't be found
        File file = new File("C:\\Users\\rebel\\Desktop\\camera\\src\\main\\resources\\img\\image_2023-12-30_16-50-04.png");
        String caption = System.lineSeparator() + mail.getDate().toString() +
                System.lineSeparator() +
                mail.getSubject();

        List<InputMedia> inputMediaPhotos = new ArrayList<>();
        SendMediaGroup sendMedia = new SendMediaGroup();
        if (!mail.getAttachments().isEmpty()) {
            //more than 1 attachment
            if (mail.getAttachments().size() > 1) {
                for (int i = 0; i < mail.getAttachments().size(); i++) {
                    File fileAttachment = new File(mail.getAttachments().get(i).toURI());
                    InputMedia inputMedia = new InputMedia() {
                        @Override
                        public String getType() {
                            return "photo";
                        }
                    };
                    inputMedia.setMedia(fileAttachment, fileAttachment.getName());
                    inputMediaPhotos.add(inputMedia);
                }
                inputMediaPhotos.get(0).setCaption(caption);
                sendMedia.setChatId(chatId);
                sendMedia.setMedias(inputMediaPhotos);
                try {
                    execute(sendMedia);

                } catch (TelegramApiException e) {
                    log.error("Exception in sendPhoto - {}", e.getMessage());
                    throw new TelegramApiException();
                }
            }
            //One attachment
            else {
                SendPhoto msg = new SendPhoto();
                msg.setChatId(chatId);
                file = mail.getAttachments().get(0);
                msg.setPhoto(new InputFile(file));

                msg.setCaption(caption);

                try {
                    execute(msg);
                }
                catch (TelegramApiException e) {
                    log.error("Exception in sendPhoto - {}", e.getMessage());
                    throw new TelegramApiException();
                }
            }
        }
    }


    private void getMail(long chatId, String textToSend) {
        List<Mail> mails =  mailReceiveClient.receive();
        sendMessage(chatId, "You have " + mails.size() + " messages");
    }

}
