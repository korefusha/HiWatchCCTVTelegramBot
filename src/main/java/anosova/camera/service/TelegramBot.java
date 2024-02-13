package anosova.camera.service;

import anosova.camera.config.BotConfig;
import anosova.camera.config.MailConfig;
import anosova.camera.mapper.MailMapper;
import anosova.camera.model.Mail;
import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.GetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.util.*;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    final BotConfig botConfig;
    final MailReceiveClient mailReceiveClient;
    final MailConfig mailConfig;
    //Constructor
    public TelegramBot(BotConfig botConfig, MailReceiveClient mailReceiveClient, MailConfig mailConfig){
        this.botConfig = botConfig;
        this.mailReceiveClient = mailReceiveClient;
        this.mailConfig = mailConfig;
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
                    try {
                        startMailListener(chatId);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    sendMessage(chatId, "Mail listener has been started");
                      break;
                case "/test":
                    testMessage(chatId,message);
                    break;
                case "/check":
                    try {
                        getMail(chatId,message);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "/photo":
                    try {
                        mailReceiveClient.receive();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    List<Mail> mailss = mailReceiveClient.receive();
                    log.info("mailReceiveClient.receive().size() = {}", mailss.size());
                    if (mailss.size() != 0) {
                        log.info("Sending new messages");
                        for (Mail mail : mailss) {
                            try {
                                sendPhoto(chatId, mail);
                            } catch (TelegramApiException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } else {
                        sendMessage(chatId, "No mails");
                    }
                    break;
                case "/clean":
                    FileCleanup.deleteFilesOlderThanOneMonth();
                    break;
                case "/help":
                    List<BotCommand> commandsList = getMyCommands();
                    StringBuilder response = new StringBuilder("Список команд бота:\n");
                    for (BotCommand command : commandsList) {
                        response.append("/").append(command.getCommand()).append("\n");
                    }
                    sendMessage(update.getMessage().getChatId(),response.toString());
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
                    log.error("Exception in sendPhoto, more than 1 attachment - {}", e.getMessage());
                    throw new TelegramApiException();
                }
            }
            //One attachment
            else {
                SendPhoto msg = new SendPhoto();
                msg.setChatId(chatId);
                if (mail.getAttachments().get(0).exists()) {

                    file = mail.getAttachments().get(0);
                    log.info("Attachment exists = {}", file.getPath());
                }
                msg.setPhoto(new InputFile(file));
                msg.setCaption(caption);

                try {
                    execute(msg);
                }
                catch (TelegramApiException e) {
                    log.error("Exception in sendPhoto, One attachment - {}", e.getMessage());
                    throw new TelegramApiException();
                }
            }
        }
    }


    private void getMail(long chatId, String textToSend) throws Exception {
        List<Mail> mails = mailReceiveClient.receive();
        sendMessage(chatId, "You have " + mails.size() + " messages");
    }

    public void startMailListener(long chatId) throws Exception{
        String folderName = mailConfig.getFolderName();
        log.info("folderName = {}", folderName);
        String protocol = mailConfig.getProtocol();
        String host = mailConfig.getHost();
        String user = mailConfig.getUser();
        String password = mailConfig.getPassword();

        // Настройка Jakarta Mail для прослушивания почты
        Properties properties = new Properties();
        properties.put("mail.imap.host", host);
        properties.put("mail.imap.user", user);
        properties.put("mail.imap.pass", password);
        Session emailSession = Session.getDefaultInstance(properties, new ImapAuthenticator(user, password));
        Store store;
        try {
            store = emailSession.getStore(protocol);
            store.connect(host, user, password);

            Folder folder = store.getFolder(folderName);
            folder.open(Folder.READ_WRITE);
            new Thread(() -> {
                try {
                    while (true) {
                        // Получение сообщений
                        List<Message> messageList = new LinkedList<Message>(Arrays.asList(folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false))));
                        log.info("New messages = {}", messageList.size());
                        for (Message message : messageList) {
                            if (!message.getFlags().toString().matches(".*Seen.*")) {
                                log.info("Message flag is = {}", message.getFlags().toString());
                                Mail mail = MailMapper.map(message);
                                sendPhoto(chatId, mail);
                            } else {
                                log.info("Message is seen. Delete message");
                                messageList.remove(message);
                            }
                        }
                        log.info("Finish receiving new mails. Have " + messageList.size() + " new mails");
                        // Пауза перед повторным поиском писем
                        Thread.sleep(5000); // 5 секунд
                        }
                } catch (Exception e) {
                    log.error("Exception startMailListener - {}", e.getMessage());
                    e.printStackTrace();
                }
            }).start();
    }
         catch (MessagingException e) {
             log.error("Exception startMailListener - {}", e.getMessage());
            e.printStackTrace();
        }
    }
    private List<BotCommand> getMyCommands() {
        GetMyCommands getMyCommands = new GetMyCommands();
        List<BotCommand> commandsList = null;
        try {
            commandsList = execute(getMyCommands);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return commandsList;
    }
}
