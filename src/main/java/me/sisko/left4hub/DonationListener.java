package me.sisko.left4hub;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

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
		if(event.isNull("username") || event.isNull("uuid")) {
			Main.getPlugin().getLogger().warning("Recieved webhook with no commands to execute");
			return;
		}
		executeCommandSync("announce &9» &a" + event.getString("username") + " &bjust made a purchase!");
		executeCommandSync("announce &9» &bshop now at &aleft4craft.org/shop!");

		JSONArray commands = new JSONArray(event.getString("commands"));
		for(Object command : commands) {
			String command_str = (String) command;
			Main.getPlugin().getLogger().info("Running command: " + command_str);
			if(!event.getBoolean("livemode")) {
				Main.getPlugin().getLogger().warning("Not actually executing command since livemode is false for this event");
			} else {
				executeCommandSync(command_str);
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
