package ChatServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server extends Thread {
	// Public Config:
	public final String SERVER_NAME = "miniChat";
	public final String SERVER_MOTD = "Stay safe, wear a mask.";
	public final String LOGIN_MSG = "Welcome to " + SERVER_NAME + " version " + ServerMain.VERSION
			+ ", please use !login <name> in order to continue to the server.";
	public boolean motdAvailable = true;

	// Server fields
	private final int serverPort;
	private ArrayList<ServerWorker> workerList = new ArrayList<>();

	public Server(int port) {
		this.serverPort = port;
	}

	public List<ServerWorker> getWorkerList() {
		return workerList;
	}

	@SuppressWarnings("resource")
	@Override
	public void run() {
		try {
			System.out.printf("~ Server started, creating socket on port %d.\n", this.serverPort);
			ServerSocket serverSocket = new ServerSocket(this.serverPort);
			System.out.println("~ Socket created.");
			while (true) {
				System.out.println("Ready to accept new connections.");
				Socket clientSocket = serverSocket.accept();
				System.out.println("Accepted connection from " + clientSocket);
				ServerWorker worker = new ServerWorker(this, clientSocket);
				System.out.println("~ Creating worker thread.");
				workerList.add(worker);
				worker.start();

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void removeWorker(ServerWorker serverWorker) {
		this.workerList.remove(serverWorker);

	}
}
