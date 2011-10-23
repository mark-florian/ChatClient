import java.util.*;
import java.io.*;
import java.net.*;

public class Client extends Thread {
	
	// Variables
	private static int listenPort;
	private static String serverIP = null;
	private static int serverPort;
	protected static Table table = new Table();
	
	
	public Client() throws IOException {
	}
	
	public Client(String s) throws IOException {
		super(s);
	}
	
	public static void startClient(String name, String sIP, String sPort, String clientPort) throws IOException {
		serverIP = sIP;
		serverPort = Integer.parseInt(sPort);
		
		DatagramSocket socket = new DatagramSocket();
		InetAddress address = InetAddress.getByName(serverIP);
		InetAddress clientIP = InetAddress.getLocalHost();		// Use for passing clientIP
		
		listenPort = Integer.parseInt(clientPort);
		
		/* Build message for registration - this always happens first */
		StringBuilder sb = new StringBuilder();
		sb.append("r ");	// This tells the server that its a registration request
		sb.append(name);
		sb.append(" ");
		sb.append(clientIP.getHostAddress());
		sb.append(" ");
		sb.append(listenPort);
		String clientInfo = sb.toString();

		byte[] buffer = clientInfo.getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, serverPort);
		socket.send(packet);
		
		/* Start two threads for send and recieve */
		Thread t1 = new Thread(new Client(), "ThreadOut");
		Thread t2 = new Thread(new Client(), "ThreadIn");
		Thread t3 = new Thread(new Client(), "Broadcast");
		
		t1.start();
		t2.start();
		t3.start();
	}
	
	public void run() {
		/* Receiving Thread */
		if(Thread.currentThread().getName().equals("ThreadIn")) {
			System.out.println("ThreadIn");
			
			try {
				DatagramSocket socket = new DatagramSocket(listenPort);
				
				while(true) {
					System.out.print(">>> ");
					byte[] buffer = new byte[256];
					
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet); // This thread is waiting to receive
					
					/* Once received */
					String rec = new String(packet.getData(), 0, packet.getLength());
					System.out.println();
					System.out.println(rec);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/* Sending Thread */
		else if(Thread.currentThread().getName().equals("ThreadOut")) {
			
			System.out.println("ThreadOut");
			
			Scanner scanner = new Scanner(System.in);
			while(true) {
				String in = scanner.nextLine();
				
				// Build the packet to send
				String[] input = parseInput(in, " ");
				if(input[0].equals("send")) {
					if(input.length < 3)
						System.out.println("Usage: send <nick-name> <message>\n>>> ");
					else {
						boolean inactiveClient = false;
						try {
							/* Find client in table */
							InetAddress address = null;
							int port = 0;
							String name = null;
							ClientObject[] clients = table.getArray();
							for(ClientObject c : clients)
								if(c.getName().equals(input[1])) {
									name = c.getName();
									address = InetAddress.getByName(c.getIP());
									port = c.getPort();
									if(!c.getActive())
										inactiveClient = true;
									break;
								}
							if(address == null)
								System.out.printf("User %s is not a registered user\n", input[1]);
							else if(inactiveClient) {
								/* Build message for server */
								StringBuilder sb = new StringBuilder();
								sb.append("s ");	// Indicates to save message
								sb.append(name);
								sb.append(" ");
								sb.append(address.getHostAddress());
								sb.append(" ");
								sb.append(port);
								sb.append(" ");
								for(int i=2; i<input.length; i++)
									sb.append(input[i] + " ");
								String message = sb.toString();
								byte[] buffer = message.getBytes();
								
								DatagramSocket socket = new DatagramSocket();
								address = InetAddress.getByName(serverIP);
								DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, serverPort);
								socket.send(packet);
								socket.close();
							}
							else {
								DatagramSocket socket = new DatagramSocket();
								
								/* Build message */
								StringBuilder sb = new StringBuilder();
								for(int i=2; i< input.length; i++)
									sb.append(input[i] + " ");
								String message = sb.toString();
								byte[] buffer = message.getBytes();
								
								/* Send packet to user */
								DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
								socket.send(packet);
								socket.close();
								
								//TODO still have to account for offline users
							}
							System.out.print(">>> ");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				else if(input[0].equals("dereg")) {
					// Send server dereg request
					if(input.length != 2)
						System.out.println("Usage: dereg <nick-name>"); //TODO make sure user is current uesr
					else {
						try {
							DatagramSocket socket = new DatagramSocket();
							InetAddress address = InetAddress.getByName(serverIP);
							
							byte[] buffer = in.trim().getBytes();
							
							DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, serverPort);
							socket.send(packet);
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				else {
					System.out.println("Error: invalid request");
				}
			}
		}
		
		/* Listening for broadcasts */
		else if(Thread.currentThread().getName().equals("Broadcast")) {
			System.out.println("listening on bcast");
			
			try {
				DatagramSocket socket = new DatagramSocket(listenPort+1);
				
				while(true) {
					System.out.print(">>> ");
					byte[] buffer = new byte[256];
					
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet); // This thread is waiting to receive
					System.out.println("Rec packate");
					/* Replace table */
					table.clear();
					String rec = new String(packet.getData(), 0, packet.getLength());
					String[] clientStrings = parseInput(rec, "$");
					for(String c : clientStrings) {
						String[] fields = parseInput(c, "|");
						table.addClient(new ClientObject(fields[0], fields[1], fields[2], Boolean.parseBoolean(fields[3])));
					}
					table.printTable();
					System.out.println(">>> [Client table updated]");
					
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static String[] parseInput(String s, String delim) {
		System.out.println("parseInput:" + s);
		ArrayList<String> al = new ArrayList<String>();
		
		StringTokenizer st = new StringTokenizer(s, delim);
		while(st.hasMoreTokens())
			al.add(st.nextToken().trim());
		
		return al.toArray(new String[al.size()]);
	}

}