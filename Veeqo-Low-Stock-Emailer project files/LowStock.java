import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


/**
 * A program that allows the user to recieve 
 * an email that contains a list of products 
 * that have low stock so they can go ahead 
 * and order some more
 * 
 */




public class LowStock {
	private static String USER_NAME = "veeqo.low.stock.list"; // Gmail user name (just the part before "@gmail.com")
	private static String PASSWORD = "327fefe9302ac540077dc787a010f895"; // GMail password
	public static String apiKey;// get key from config file
	public static String recipient;// email address to recieve email

	public static void main(String[] args) {
		
		
		System.out.println("Reading config file.");
		getConfigData();

		ArrayList<String> title = new ArrayList<String>();// store all product titles
		ArrayList<String> sku = new ArrayList<String>();// store all products sku codes
		ArrayList<String> stockLevelLow = new ArrayList<String>();// store all products low stock level value

		Properties props = new Properties();// properties object to connect to simple mail transfer protocol (smtp)
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");
		System.out.println("Connecting to veeqo to gather data.");
		Client client = ClientBuilder.newClient();
		Response response = client.target("https://api.veeqo.com/products").request(MediaType.APPLICATION_JSON_TYPE)
				.header("x-api-key", apiKey).get();// connect to api all products endpoint
		String data = response.readEntity(String.class);// get all product data

		if (!data.equals("{\"error_messages\":\"You're not authorized\"}")) {// successfully gathered data
			Scanner s = new Scanner(data);// initialise scanner object to read the data
			String temp;
			while (s.hasNext()) {
				s.useDelimiter("\"id\":");// used to find the start of each product
				s.next();// consume data
				s.useDelimiter(",");// used to find the end of each product ID
				if (s.hasNext()) {// check there is another product

					s.useDelimiter("\"sku_code\":");// find the next sku code
					s.next();
					s.useDelimiter(",");
					temp = s.next();
					sku.add(temp.substring(12, temp.length() - 1));// extract data from "sku_code":"example" and add it
																	// to the sku arraylist

					s.useDelimiter("\"full_title\":");// find the next full title
					s.next();
					s.useDelimiter(",");
					temp = s.next();
					title.add(temp.substring(14, temp.length() - 1));// extract full title data and add it to the title
																		// arraylist

					s.useDelimiter("\"stock_running_low\":");// find the Stock running low from the current product
					s.next();
					s.useDelimiter(",");
					temp = s.next();
					stockLevelLow.add(temp.substring(20, temp.length()));// extract low stock level data and add it to
																			// the arraylist

					s.useDelimiter("\"id\":");
					s.next();

				}

			}
			s.close();// close the scanner
			System.out.println("Data Gathered.");
			String from = USER_NAME;
			String pass = PASSWORD;
			String to = recipient; // Users email address
			String subject = "Veeqo Low Stock List";
			String body = "\nHello, \n \nHere is all the products that have low Stock. \n \n";

			for (int i = 0; i < stockLevelLow.size(); i++) {// add the data to the body of the email from the arrays
				if (stockLevelLow.get(i).equals("true")) {
					body += title.get(i) + "\n" + sku.get(i) + "\n \n";
				}

			}
			
			System.out.println("Sending Email.");
			sendFromGMail(from, pass, to, subject, body);// call the methos to send the email
	
			
		} else if (apiKey.equals("Replace with your veeqo API key")) {
			System.out.println("Error: Please edit config file");

		} else {
			System.out.println(data);// unable to connect display error
		}
	}

	/*
	 * get api key from config file
	 * 
	 */

	public static void getConfigData() {

		try {
			String line;
			// FileReader reads text files in the default encoding.
			
			
			FileReader fileReader = new FileReader("config.txt");

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			apiKey = bufferedReader.readLine().substring(8);
			recipient = bufferedReader.readLine().substring(14);

			// Always close files.
			bufferedReader.close();
		} catch (Exception ex) {
			System.out.println("Unable to open file");
		}

	}

	private static void sendFromGMail(String from, String pass, String to, String subject, String body) {
		Properties props = System.getProperties();

		// set up properties for mail object
		String host = "smtp.gmail.com";
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.user", from);
		props.put("mail.smtp.password", pass);
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.auth", "true");

		Session session = Session.getDefaultInstance(props);
		MimeMessage message = new MimeMessage(session);

		try {
			message.setFrom(new InternetAddress(from));// email address to send from
			InternetAddress toAddress = new InternetAddress(to);// email address to send to

			message.addRecipient(Message.RecipientType.TO, toAddress);

			message.setSubject(subject);// add email subject line
			message.setText(body); // add email body(data)
			Transport transport = session.getTransport("smtp");
			transport.connect(host, from, pass);// connect
			transport.sendMessage(message, message.getAllRecipients());// send
			transport.close();// close connection
			System.out.println("Email sent to: " + recipient);

		} catch (AddressException ae) {
			System.out.println("Error: Please correct your email address in config file");
			ae.printStackTrace();

		} catch (MessagingException me) {
			me.printStackTrace();
		}
	}
}
