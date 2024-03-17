package anosova.camera.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data
@PropertySource("application.properties")
public class MailConfig {
    @Value("${mail.folder}")
     String folderName;
    @Value("${mail.protocol}")
    String protocol;
    @Value("${mail.imap.host}")
    String host;
    @Value("${mail.port")
    String port;

    @Value("${mail.user}")
    String user;
    @Value("${mail.password}")
    String password;

    public MailConfig(@Value("${mail.protocol}") String protocol,
                      @Value("${mail.imap.host}") String host,
                      @Value("${mail.port}") String port,
                      @Value("${mail.user}") String user,
                      @Value("${mail.password}") String password) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }
}
