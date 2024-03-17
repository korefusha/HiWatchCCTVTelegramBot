package anosova.camera.model;


import jakarta.mail.Flags;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
@Getter
@Setter
public class Mail{

    private String fromAddress;
    private String fromName;
    private String toAddress;
    private String toName;
    private String subject;
    private String cc;
    private String bcc;
    private String contentType;

    private String text;

    private Date date;

    private List <File> attachments;

    private Flags flags;

    public Mail(){}
}

