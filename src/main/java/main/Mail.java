package main;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Mail {

	private static final String TO = "bschmid@mpi-cbg.de"; // TODO change to huiskenlab@mpi-cbg.de:w


	public static void main(String[] args) {
		send("EduSPIM error", "test");
	}

	public static void sendError(String text) {
		send("EduSPIM error", text);
	}

	public static void send(String subject, String text) {
		send(subject, TO, text);
	}

	public static void send(String subject, String to, String text) {
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
					InternetAddress.parse("bschmid@mpi-cbg.de")); // TODO CC to huiskenlab
			message.setSubject(subject);
			message.setText(text);

			Transport.send(message);
		} catch (MessagingException e) {
			ExceptionHandler.handleException("Error sending mail", e);
		}
	}
}
