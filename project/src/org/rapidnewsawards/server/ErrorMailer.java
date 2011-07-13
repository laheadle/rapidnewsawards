package org.rapidnewsawards.server;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withPayload;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.appengine.api.taskqueue.DeferredTask;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;

public class ErrorMailer extends Handler implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String email;
	

	ErrorMailer() {
		this.email = "laheadle@gmail.com";
	}
	
	@Override
	public void publish(LogRecord record) {
		if (record.getLevel().intValue() > Level.WARNING.intValue()) {
			final String message = record.getMessage();
			Queue queue = QueueFactory.getQueue("mail");
			queue.add(withPayload(new DeferredTask() {
				private static final long serialVersionUID = 1L;
				@Override
				public void run() {
			        Properties props = new Properties();
			        Session session = Session.getDefaultInstance(props, null);

			        try {
			            Message msg = new MimeMessage(session);
			            msg.setFrom(new InternetAddress(email, "newskraft Admin"));
			            msg.addRecipient(Message.RecipientType.TO,
			                             new InternetAddress(email, "Lyn Headley"));
			            msg.setSubject("error report!");
			            msg.setText(message);
			            Transport.send(msg);

			        } catch (AddressException e) {
			        	DAO.log.severe("mail failed 1");
			        } catch (MessagingException e) {
			        	DAO.log.severe("mail failed 2");
			        } catch (UnsupportedEncodingException e) {
			        	DAO.log.severe("mail failed 3");			        	
					}
				}
		}));
		}
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() throws SecurityException {
	}

}
