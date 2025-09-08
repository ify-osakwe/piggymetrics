package github.com.rexfilius.notification.service;

import github.com.rexfilius.notification.client.AccountServiceClient;
import github.com.rexfilius.notification.domain.NotificationType;
import github.com.rexfilius.notification.domain.Recipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationServiceImpl  {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AccountServiceClient client;
	private final RecipientServiceImpl recipientService;
	private final EmailServiceImpl emailService;

	public NotificationServiceImpl(AccountServiceClient client,
									RecipientServiceImpl recipientService,
									EmailServiceImpl emailService) {
		this.client = client;
		this.recipientService = recipientService;
		this.emailService = emailService;
	}

	@Scheduled(cron = "${backup.cron}")
	public void sendBackupNotifications() {

		final NotificationType type = NotificationType.BACKUP;

		List<Recipient> recipients = recipientService.findReadyToNotify(type);
		log.info("found {} recipients for backup notification", recipients.size());

		recipients.forEach(recipient -> CompletableFuture.runAsync(() -> {
			try {
				String attachment = client.getAccount(recipient.getAccountName());
				emailService.send(type, recipient, attachment);
				recipientService.markNotified(type, recipient);
			} catch (Throwable t) {
				log.error("an error during backup notification for {}", recipient, t);
			}
		}));
	}

	@Scheduled(cron = "${remind.cron}")
	public void sendRemindNotifications() {

		final NotificationType type = NotificationType.REMIND;

		List<Recipient> recipients = recipientService.findReadyToNotify(type);
		log.info("found {} recipients for remind notification", recipients.size());

		recipients.forEach(recipient -> CompletableFuture.runAsync(() -> {
			try {
				emailService.send(type, recipient, null);
				recipientService.markNotified(type, recipient);
			} catch (Throwable t) {
				log.error("an error during remind notification for {}", recipient, t);
			}
		}));
	}
}
