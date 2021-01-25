package ChatServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class ServerWorker extends Thread {

	private final Socket clientSocket;
	private final Server server;
	private String login = null;
	private boolean loggedIn = false;
	private OutputStream outputStream;
	private boolean isAdmin = false;

	public ServerWorker(Server server, Socket clientSocket) {
		this.server = server;
		this.clientSocket = clientSocket;
	}

	@Override
	public void run() {
		try {
			System.out.println("~ Worker established succesfully.");
			handleClient();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// This function handles the stream between the user and the server, along with
	// handling commands (chat) sent by user.
	private void handleClient() throws IOException, InterruptedException {
		// Code
		InputStream inputStream = this.clientSocket.getInputStream();
		this.outputStream = this.clientSocket.getOutputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String line;

		// Send user successful login message.
		this.outputStream.write((server.LOGIN_MSG + "\n").getBytes());

		// Main commands loop. (Chat while)
		try {
			while ((line = reader.readLine()) != null) {
				String[] tokens = StringUtils.split(line);
				if (tokens != null && tokens.length > 0) { // Valid message
					String cmd = tokens[0];

					if (cmd.charAt(0) == '!') { // This is a command.
						// Shared among logged and null users
						if ("!logoff".equalsIgnoreCase(cmd) || "!quit".equalsIgnoreCase(cmd)) {
							handleLogoff();
							break;
						}

						// Null users commands
						if (this.loggedIn == false) {
							if ("!login".equalsIgnoreCase(cmd)) { // Login command
								handleLogin(outputStream, tokens);
							} else {
								String msg = "[SERVER] Unknown command " + cmd + "\n";
								outputStream.write(msg.getBytes());
							}
						} else { // Logged users commands.
							if ("!online".equalsIgnoreCase(cmd)) {
								cmd_online();
							} else if ("!pm".equalsIgnoreCase(cmd) || "!whisper".equalsIgnoreCase(cmd)) {
								String[] privateMessage = StringUtils.split(line, null, 3);
								if (privateMessage.length > 2)
									privateMessage(privateMessage);
								else
									sendMessage("[SERVER] Private message cannot be empty message.\n");
							} else if ("!rcon".equalsIgnoreCase(cmd)) { // Rcon login
								String[] rconPassword = StringUtils.split(line, null, 2);
								if (rconPassword.length > 1)
									rconLogin(rconPassword);
								else
									sendMessage("[SERVER] Please use !rcon <password> to continue.\n");
							} else if ("!credits".equalsIgnoreCase(cmd)) { // Credits command
								cmd_credits();
							} else if ("!help".equalsIgnoreCase(cmd) || "!cmds".equalsIgnoreCase(cmd)) { // Help command
								cmd_help();
							}

							// Administrator commands.
							else if (this.isAdmin == true && this.loggedIn == true) {
								if ("!motd".equalsIgnoreCase(cmd)) {
									String[] userSwitch = StringUtils.split(line, null, 2);
									if (userSwitch.length > 1)
										cmd_aMotd(userSwitch);
									else
										sendMessage("[SERVER] Error, prefix is !motd <on/off>.\n");
								} else if ("!kick".equalsIgnoreCase(cmd)) {
									String[] kickUser = StringUtils.split(line, null, 2);
									if (kickUser.length > 1)
										cmd_aKick(kickUser);
									else
										sendMessage("[SERVER] Error, prefix is !kick <username>.\n");
								}
							}

							// Unknown commands
							else {
								String msg = "[SERVER] Unknown command " + cmd + "\n";
								outputStream.write(msg.getBytes());
							}
						}
					} else if (this.loggedIn == true) { // This was a message
						sendUserGlobalMessage(line);
					} else {
						String error = "Please login using !login <name> in-order to continue.\n";
						outputStream.write(error.getBytes());
					}

				}

			}
		} catch (java.net.SocketException e) {
			System.out.println("User has been kicked out of the server.");
		}

	}

	private void rconLogin(String[] rconPassword) throws IOException {
		final String PASSWORD = rconPassword[1];
		final String RCON_PASSWORD = "1234";
		String msgFormat;
		if (this.isAdmin == true) {
			msgFormat = "[SERVER] You are already logged in as an administrator.\n";

		} else if (PASSWORD.equals(RCON_PASSWORD) && this.isAdmin == false) {
			msgFormat = "[SERVER] You have succesfully logged in as an administrator.\n";
			this.isAdmin = true;

		} else {
			msgFormat = "[SERVER] Incorrect password.\n";
		}
		sendMessage(msgFormat);

	}

	// This function sends a user global chat message.
	private void sendUserGlobalMessage(String msg) throws IOException {
		String format = this.login + ": " + msg + "\n";
		sendGlobalMessage(format, this.login);

	}

	// This function sends a private message to a user.
	private void privateMessage(String[] tokens) throws IOException {
		String sendTo = tokens[1];
		if (sendTo.equals(this.login)) {
			sendMessage("[SERVER] You cannot message yourself.\n");
			return;
		}

		String body = tokens[2];
		List<ServerWorker> workerList = server.getWorkerList();
		for (ServerWorker worker : workerList) {
			if (sendTo.equals(worker.getLogin())) {
				String format = "[PM]" + this.login + ": " + body + "\n";
				worker.sendMessage(format);
			}
		}

	}

	private void handleLogoff() throws IOException {
		if (this.login != null) {
			String msg = "[SERVER] " + this.login + " has logged off.\n";
			sendGlobalMessage(msg, this.login);
			sendMessage("You have logged off.\n");
			System.out.printf("User %s logged off.\n", this.login);
			this.loggedIn = false;
			this.isAdmin = false;
			server.removeWorker(this);
		}
		clientSocket.close();

	}

	public String getLogin() {
		return this.login;
	}

	private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
		if (tokens.length == 2) {
			String login = tokens[1];

			if (login == null || login.length() < 3) {
				this.outputStream.write("[SERVER] Please pick a username longer than 3 characters.\n".getBytes());
				return;
			} else {
				this.login = login;
				this.loggedIn = true;
				String msg = "[SERVER] You have succesfully logged in, welcome " + login + ".\n";
				sendMessage(msg);

				// Send MOTD to the user.
				if (server.motdAvailable == true) {
					final String motdFormat = "[MOTD] " + server.SERVER_MOTD + "\n";
					sendMessage(motdFormat);
				}

				System.out.printf("User %s logged in succesfully.\n", login); // Debug msg.
				String onlineMsg = "[SERVER] " + login + " is now online!" + "\n";
				sendGlobalMessage(onlineMsg, login);
			}

//			String password = tokens[2];
//
//			if (login.equals("guest") && password.equals("guest")
//					|| login.equals("jayco") && password.equals("jayco")) {
//				this.login = login;
//				this.loggedIn = true;
//				String msg = "[SERVER] You have succesfully logged in, welcome " + login + ".\n";
//				outputStream.write(msg.getBytes());
//				System.out.printf("User %s logged in succesfully.\n", login); // Debug msg.
//				String onlineMsg = "[SERVER] " + login + " is now online!" + "\n";
//				sendGlobalMessage(onlineMsg, login);
//			} else {
//				String msg = "[SERVER] Error! Couln't find a user with this matching cardentials.\n";
//				outputStream.write(msg.getBytes());
//			}
		}
	}

	// This function handles messages sending.
	private void sendMessage(String msg) throws IOException {
		if (this.login != null) {
			this.outputStream.write(msg.getBytes());
		}
	}

	// This function loops through all the users and sends them a message.
	private void sendGlobalMessage(String msg, String exclude) throws IOException {
		List<ServerWorker> workerList = server.getWorkerList();
		for (ServerWorker worker : workerList) {
			if (worker.getLogin() == null)
				continue;
			if (!worker.getLogin().equals(exclude)) {
				worker.sendMessage(msg);
			}

		}
	}

	private boolean isUserOnline(String userName) {
		List<ServerWorker> workerList = server.getWorkerList();
		for (ServerWorker worker : workerList) {
			if (worker.getLogin() == null)
				continue;
			if (worker.getLogin().equals(userName) && worker.isAlive() && worker.loggedIn == true) {
				return true;
			}

		}
		return false;
	}

	// This command returns all online users on the server.
	private void cmd_online() throws IOException {
		List<ServerWorker> workerList = server.getWorkerList();
		String prefix = "[SERVER] Online users: \n";
		String format;
		this.outputStream.write(prefix.getBytes());
		int userCount = 0;
		for (ServerWorker worker : workerList) { // Loop through the list of all workers.
			if (worker.getLogin() != null) { // Only logged in users count.
				final String format2 = ", ";
				if (userCount > 0 && userCount % 3 != 0)
					this.outputStream.write(format2.getBytes());
				this.outputStream.write(worker.getLogin().getBytes());
				userCount++;
				if (userCount % 3 == 0) {
					format = ".\n";
					this.outputStream.write(format.getBytes());
				}

			}
		}
		if (userCount % 3 != 0) {
			this.outputStream.write(".".getBytes()); // Formatting.
			this.outputStream.write("\n".getBytes()); // Even more formatting.
		}
	}

	// This function prints the credits for the creators.
	private void cmd_credits() throws IOException {
		final String FORMAT = "Created by " + ServerMain.CREATOR + ". \n~ Credits: \nTesters: Dani (Denis) Kogel.\n"
				+ "Version: " + ServerMain.VERSION + ".\n";
		sendMessage(FORMAT);
	}

	// This function prints all the available commands on the server.
	private void cmd_help() throws IOException {
		final String USER_CMDS = "[SERVER] Available commands: !pm, !online, !credits, !help.\n";
		String FORMAT;
		if (this.isAdmin == true) // If the user is an admin, output the administrator commands.
			FORMAT = USER_CMDS + "[SERVER] Administrator commands: !motd <on/off>, !kick <username>.\n";
		else
			FORMAT = USER_CMDS;
		sendMessage(FORMAT);
	}

	// This function alters the MOTD message displayed.
	private void cmd_aMotd(String[] userSwitch) throws IOException {
		final String STATUS = userSwitch[1];
		if (server.motdAvailable == true && (STATUS.equalsIgnoreCase("OFF") || STATUS.equalsIgnoreCase("false"))) {
			sendMessage("[SERVER] MOTD switched off.\n");
			server.motdAvailable = false;
		} else if (server.motdAvailable == false
				&& (STATUS.equalsIgnoreCase("OFF") || STATUS.equalsIgnoreCase("false"))) {
			sendMessage("[SERVER] MOTD switched on.\n");
			server.motdAvailable = true;
		} else {
			sendMessage("[SERVER] Unkown status ''" + STATUS + "''.\n");
		}

	}

	// This function kicks a user from the server.
	private void cmd_aKick(String[] kickUser) throws IOException {
		final String USER = kickUser[1];
		if (isUserOnline(USER)) {
			final String MSG = "[SERVER] You have been kicked from the server.\n";
			List<ServerWorker> workerList = server.getWorkerList();
			for (ServerWorker worker : workerList) {
				if (worker.getLogin() == null)
					continue;
				if (worker.getLogin().equals(USER)) {
					worker.sendMessage(MSG);
					server.removeWorker(worker);
					worker.clientSocket.close();
					sendGlobalMessage("[SERVER] " + USER + " has been kicked from the server.\n", null);
					break;
				}

			}
		} else {
			sendMessage("[SERVER] Couldn't find a user by the name " + USER + ".\n");
		}

	}

}
