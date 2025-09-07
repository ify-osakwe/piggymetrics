package github.com.rexfilius.notification.service;

import github.com.rexfilius.notification.domain.NotificationType;
import github.com.rexfilius.notification.domain.Recipient;

import javax.mail.MessagingException;
import java.io.IOException;

public interface EmailService {

	void send(NotificationType type, Recipient recipient, String attachment) throws MessagingException, IOException;

}
