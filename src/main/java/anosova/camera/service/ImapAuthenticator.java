package anosova.camera.service;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;

public class ImapAuthenticator extends Authenticator {
    private String username;
    private String password;
    public ImapAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
//        super();
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        if ((username != null) && (username.length() > 0) && (password != null)
                && (password.length   () > 0)) {

            return new PasswordAuthentication(username, password);
        }
        return null;
    }
}
