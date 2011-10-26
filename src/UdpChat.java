import java.io.*;

public class UdpChat {
	public static void main(String args[]) throws IOException {
		if(args[0].equals("-s")) {
			if(args.length != 2)
				System.out.println("Usage: UdpChat -s <port number>");
			else
				try {
					int port = Integer.parseInt(args[1]);
					int check = portCheck(port);
					
					switch (check) {
					case 1:
						System.out.println("Port number must be positive");
						System.exit(1);
					case 2:
						System.out.println("Port numbers from 0 to 1023 are reserved");
						System.exit(1);
					case 3:
						System.out.println("Port number cannot exceed 65535");
						System.exit(1);
					default:
						Server.startServer(args[1]);
					}
				} catch (NumberFormatException e) {
					System.out.println("Port number must be a positive integer between 1024 and 65535");
				}
		}
		else if(args[0].equals("-c")) {
			if(args.length != 5)
				System.out.println("Usage: UdpChat -c <nick-name> <server-ip> <server-port> client-port>");
			else
				try {
					int serverPort = Integer.parseInt(args[3]);
					int clientPort = Integer.parseInt(args[4]);
					int check1 = portCheck(serverPort);
					int check2 = portCheck(clientPort);
					
					switch (check1) {
					case 1:
						System.out.println("Port number must be positive");
						System.exit(1);
					case 2:
						System.out.println("Port numbers from 0 to 1023 are reserved");
						System.exit(1);
					case 3:
						System.out.println("Port number cannot exceed 65535");
						System.exit(1);
					}
					switch (check2) {
					case 1:
						System.out.println("Port number must be positive");
						System.exit(1);
					case 2:
						System.out.println("Port numbers from 0 to 1023 are reserved");
						System.exit(1);
					case 3:
						System.out.println("Port number cannot exceed 65535");
						System.exit(1);
					}
					Client.startClient(args[1], args[2], args[3], args[4]);
					
				} catch (NumberFormatException e) {
					System.out.println("Port number must be a positive integer between 1024 and 65535");
				}
		}
		else
			System.out.println("Error: invalid input");
			
	}
	
	private static int portCheck(int port)
	{
		if(port < 0) {
			return 1;
		}
		else if(port < 1024) {
			return 2;
		}
		else if(port > 65535) {
			return 3;
		}
		return 0;
	}
}