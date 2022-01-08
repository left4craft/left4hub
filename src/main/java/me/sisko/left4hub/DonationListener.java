package me.sisko.left4hub;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Session;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
   import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.google.gson.JsonObject;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONArray;
import org.json.JSONObject;

public class DonationListener {

	private JSONObject stripeConfig;
	private AmazonSQS  sqs;

	public DonationListener(String confJson) {
		readStripeConfig(confJson);
		
		final JSONObject aws = stripeConfig.getJSONObject("aws");
		final String awsAccessKeyId = aws.getString("id");
		final String awsSecretKey = aws.getString("secret");

		BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsAccessKeyId, awsSecretKey);

		sqs = AmazonSQSClientBuilder.standard()
			.withRegion(Regions.US_EAST_2)
			.withCredentials(new AWSStaticCredentialsProvider(awsCreds))
			.build();
		// SQSConnectionFactory connectionFactory = new SQSConnectionFactory(
		// 	new ProviderConfiguration(),
		// 	AmazonSQSClientBuilder.standard().withRegion(Regions.US_EAST_2).build()
		// );
		// try {
		// 	this.sqs = connectionFactory.createConnection(awsAccessKeyId, awsSecretKey).getAmazonSQSClient();
		// } catch (JMSException e) {
		// 	e.printStackTrace();
		// }
	}

	public void readStripeConfig(String name) {
		File confFile = new File(Main.getPlugin().getDataFolder().getAbsolutePath() + "/" + name);
		if(confFile.exists()) {
			try {
				final String jsonStr = String.join("", Files.readAllLines(Paths.get(confFile.getAbsolutePath())));
				stripeConfig = new JSONObject(jsonStr);

			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			Main.getPlugin().getLogger().warning(name + " could not be found!");
		}
	}

	// async queue polling function
	public void poll() {
		final String queueURL = stripeConfig.getJSONObject("aws").getString("sqs_url");

		// Main.getPlugin().getLogger().info("Polling queue");

		ReceiveMessageRequest receive_request =  new ReceiveMessageRequest(queueURL);			
		List<Message> result = sqs.receiveMessage(receive_request).getMessages();
		for(Message m : result) {
			Main.getPlugin().getLogger().info(m.getBody());
			JSONObject event = new JSONObject(m.getBody());
			handleStripeEvent(event);
			sqs.deleteMessage(queueURL, m.getReceiptHandle());
		}
	}

	// stripe event handler
	private void handleStripeEvent(JSONObject event) {
		if(!event.getString("type").equals("invoice.paid")) {
			Main.getPlugin().getLogger().warning("Recieved unknown event type " + event.getString("type"));
			return;
		}

		final JSONArray lineItems = event
										.getJSONObject("data")
										.getJSONObject("object")
										.getJSONObject("lines")
										.getJSONArray("data");

		for(Object lineItemObj : lineItems) {
			JSONObject lineItem = (JSONObject) lineItemObj;

			// check if line item has minecraft info
			if(!lineItem.getJSONObject("metadata").has("mc_username")) {
				Main.getPlugin().getLogger().warning("Event " + lineItem.getString("id") + " has a line item with no metadata!");
				continue;
			}

			// check the product configuration exists
			String productId = lineItem.getJSONObject("price").getString("product");
			if(!stripeConfig.getJSONObject("products").has(productId)) {
				Main.getPlugin().getLogger().warning("Product " + productId + " is not configured in stripe.json!");
				continue;
			}

			// placeholders that apply to all
			String username = lineItem.getJSONObject("metadata").getString("mc_username");
			String uuid = lineItem.getJSONObject("metadata").getString("mc_uuid");
			int quantity = lineItem.getInt("quantity");

			// placeholders depends on if it's a subscription
			String type = "lifetime";
			String time = "1s";

			// subscription-specific placeholders
			if(lineItem.getString("type").equals("subscription")) {
				long remainingSeconds = lineItem.getJSONObject("period").getLong("end")
							- (System.currentTimeMillis() / 1000l);

				// convert to days with buffer period of 5 days
				long remainingDays = remainingSeconds / 86400l;
				remainingDays += 5;
				time = Long.toString(remainingDays) + "d";

				if(remainingDays > 72l) {
					type = "yearly";
				} else {
					type = "monthly";
				}
			}

			JSONObject product = stripeConfig.getJSONObject("products").getJSONObject(productId);

			List<String> commands = new ArrayList<String>();

			// subscriptions with different commands based on whether time
			if(product.has(type)) {
				for(Object cmdObj : product.getJSONObject(type).getJSONArray("commands")) {
					commands.add((String) cmdObj);
				}
			}

			for(Object cmdObj : product.getJSONArray("commands")) {
				commands.add((String) cmdObj);
			}

			for(int i = 0; i < commands.size(); i++) {
				// fill in placeholders
				String command = commands.get(i);
				command = command.replace("{USERNAME}", username);
				command = command.replace("{UUID}", uuid);
				command = command.replace("{QUANTITY}", Integer.toString(quantity));
				command = command.replace("{TIME}", time);
				commands.set(i, command);
			}

			for(String command : commands) {
				Main.getPlugin().getLogger().info("Running command: " + command);
				executeCommandSync(command);
			}
			
		}


	}

	private void executeCommandSync(String cmd) {
		// must schedule a synchronous task
		new BukkitRunnable() {
			@Override
			public void run() {
				ConsoleCommandSender console = Main.getPlugin().getServer().getConsoleSender();
				Bukkit.dispatchCommand(console, cmd);
			}
		}.runTask(Main.getPlugin());
	}
}
