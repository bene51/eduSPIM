package main;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mail {

	private static final Logger logger = LoggerFactory.getLogger(Mail.class);

	private static final ExecutorService exec = Executors.newSingleThreadExecutor();

	public static void main(String[] args) throws Exception {
		send("EduSPIM error", "test");
	}

	public static void sendError(String text) {
		send("EduSPIM error", text);
	}

	public static void send(String subject, String text) {
		send(subject, Preferences.getMailto(), Preferences.getMailcc(), text);
	}

	public static void send(final String subject, final String to, final String cc, final String text) {
		send(subject, to, cc, text, false);
	}

	public static void send(final String subject, final String to, final String cc, final String text, boolean wait) {
		send(subject, to, cc, text, Preferences.getSMTPUsername(), Preferences.getSMTPPassword(), wait);
	}

	public static void send(
			final String subject,
			final String to,
			final String cc,
			final String text,
			final String username,
			final String password,
			boolean wait) {
		Future<?> fut = exec.submit(new Runnable() {
			@Override
			public void run() {
				int failed = 0;
				do {
					final String username = "eduspim@gmail.com";
					final String password = "cmlc2GFP";

					Properties props = new Properties();
					props.put("mail.smtp.auth", "true");
					props.put("mail.smtp.starttls.enable", "true");
					props.put("mail.smtp.host", "smtp.gmail.com");
					props.put("mail.smtp.port", "587");

					Session session = Session.getInstance(props,
							new javax.mail.Authenticator() {
								@Override
								protected PasswordAuthentication getPasswordAuthentication() {
									return new PasswordAuthentication(username, password);
								}
							});

					try {
						Message message = new MimeMessage(session);
						message.setFrom(InternetAddress.parse("eduspim@gmail.com")[0]);
						message.setRecipients(Message.RecipientType.TO,
								InternetAddress.parse(to));
						if(cc != null && cc.trim().length() != 0)
							message.setRecipients(Message.RecipientType.CC,
									InternetAddress.parse(cc));
						message.setSubject(subject);
						message.setText(text);

						Transport.send(message);
					} catch (MessagingException e) {
						failed++;
						// ExceptionHandler.handleException("Error sending mail", e);
						e.printStackTrace();
						logger.error("Error sending mail", e);
						try {
							// wait for half a minute so that internet connection can
							// be established in the mean time
							Thread.sleep(30000);
						} catch(InterruptedException ex) {}
					}
				} while(failed > 0 && failed < 10);
			} // run
		});
		if(wait) { // Timeout
			try {
				fut.get();
			} catch (InterruptedException e) {
				ExceptionHandler.handleException("Sending mail interrupted", e);
			} catch (ExecutionException e) {
				ExceptionHandler.handleException("Error sending mail", e);
			}
		}
	}
}
