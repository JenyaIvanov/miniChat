package ChatServer;

public class ServerMain {

	final static String VERSION = "0.2.1 Alpha";
	final static String CREATOR = "Jenya Ivanov - 2021";

	public static void main(String[] args) {
		System.out.printf("Welcome to miniChat by %s, version %s.\n", CREATOR, VERSION);
		final int PORT = 8818;
		Server server = new Server(PORT);
		System.out.println("~ Establishing server.");
		server.start();

	}

}
