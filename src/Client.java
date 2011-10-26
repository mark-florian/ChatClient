import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;

public class Client extends Thread {
	
	// Variables
	private static String serverIP = null;
	private static int serverPort;
	private static String clientName = null;
	private static int clientPort;
	protected static Table table = new Table();
	
	private static InetAddress serverAddress;
	private static InetAddress clientAddress;
	private static String clientInfo = null;
	
	public static boolean ACK = false;
	
	public Client() throws IOException {
	}
	
	public Client(String s) throws IOException {
		super(s);
	}
	
	public static void startClient(String name, String sIP, String sPort, String cPort) throws IOException {
		serverIP = sIP;
		serverPort = Integer.parseInt(sPort);
		clientName = name;
		clientPort = Integer.parseInt(cPort);
		
		serverAddress = InetAddress.getByName(serverIP);
		clientAddress = InetAddress.getLocalHost();
		clientInfo = name + "|" + clientAddress.getHostAddress() + "|" + cPort;
//		InetAddress address = InetAddress.getByName(serverIP);
//		InetAddress clientIP = InetAddress.getLocalHost();		// Use for passing clientIP
		
		/* Build message for registration - this always happens first */
		StringBuilder sb = new StringBuilder();
		sb.append("r$");	// This tells the server that its a registration request
		sb.append(name + "|");
		sb.append(clientAddress.getHostAddress() + "|");
		sb.append(clientPort);
		sb.append("$null|null|0$noMessage");	// This is done to normalize headers
		String clientInfo = sb.toString();

		byte[] buffer = clientInfo.getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
		
		DatagramSocket socket = new DatagramSocket();
		socket.send(packet);
		socket.close();
		
		System.out.print(">>> [Welcome, You are registered.]\n>>> ");
		
		/* Start two threads for send and recieve */
		Thread t1 = new Thread(new Client(), "STDIN");
		Thread t2 = new Thread(new Client(), "Listen");
		Thread t3 = new Thread(new Client(), "Broadcast");
		
		t1.start();
		t2.start();
		t3.start();
	}
	
	public void run() {
		/* Receiving Thread */
		if(Thread.currentThread().getName().equals("Listen")) {
//			System.out.println("ThreadIn");
			
			try {
				DatagramSocket socket = new DatagramSocket(clientPort);
				
				while(true) {
					byte[] buffer = new byte[256];
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet); // This thread is waiting to receive
					System.out.print(">>> ");
					
					/* Extract data */
					String[] incoming = parsePacket(new String(packet.getData()), "$");
					String[] fromClient = parsePacket(incoming[1], "|");
					String[] toClient = parsePacket(incoming[2], "|");
					
					String category = incoming[0].trim();
					String fromName = fromClient[0].trim();
					String fromIP = fromClient[1].trim();
					int fromPort = Integer.parseInt(fromClient[2]);
					String toName = toClient[0].trim();
					String toIP = toClient[1].trim();
					int toPort = Integer.parseInt(toClient[2]);
					String message = incoming[3].trim();
					
//					/* These are either streaming or saved messages */
//					String rec = new String(packet.getData(), 0, packet.getLength());
//					String[] message = parseInput(rec, "$");
					
					if(category.equals("saved")) {
						System.out.print("[You have messages]\n>>> ");
						System.out.print(message);
//						for(int i=1; i<message.length; i++)
//							System.out.println(">>> " + message[i]);
					}
					else if(category.equals("ACK")) {
						ACK = true;
//						System.out.println("ACKMOTHERFUCKER!!!");
					}
					else if(category.equals("clientMessage")){
						/* Get fromClient info */
//						System.out.println("HeaderInfo: " + message[0]);
						
//						String[] fromClient = parseInput(message[0], "|");
////						for(String s : fromClient)
////							System.out.println(s);
//						String fromName = fromClient[0];
//						String fromIP = fromClient[1];
//						int fromPort = Integer.parseInt(fromClient[2]);
//						InetAddress fromAddress = InetAddress.getByName(fromIP);
						
						/* Print out message */
//						for(int i=0; i<message.length; i++)
//							System.out.print(message[i]);
						System.out.print(message + "\n>>> ");
						
						/* Send ACK */
						byte[] ack = "ACK$null|null|0$null|null|0$noMessage".getBytes();
						DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, InetAddress.getByName(fromIP), fromPort);
						DatagramSocket ackSocket = new DatagramSocket();
						ackSocket.send(ackPacket);
						ackSocket.close();
					}
//					System.out.print(">>> ");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/* Sending Thread */
		else if(Thread.currentThread().getName().equals("STDIN")) {
			
//			System.out.println("ThreadOut");
			
			Scanner scanner = new Scanner(System.in);
			while(true) {
				String in = scanner.nextLine();
				System.out.print(">>> ");
				
				/* Build data used throughout */
				String[] input = parsePacket(in, " ");
				String category = input[0];
				
				if(category.equals("send")) {
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
								sb.append("s$");	// Indicates to save message
								sb.append(clientInfo + "$");	// From
								sb.append(name + "|" + address.getHostAddress() + "|" + port + "$");	// To
								for(int i=2; i<input.length; i++)
									sb.append(input[i] + " ");
								String message = sb.toString();
								byte[] buffer = message.getBytes();
								
								DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
								DatagramSocket socket = new DatagramSocket();
								socket.send(packet);
								socket.close();
								
								/* Wait for ACK */
								ACK = false;
								long startTime = System.currentTimeMillis();
								int failures = 0;
								while(failures < 5) {
									while(System.currentTimeMillis()-startTime < 500 && ACK == false);
									if(ACK == true)
										break;
									System.out.print("[Server not responding]\n>>> ");
									startTime = System.currentTimeMillis();
									failures++;
								}
								
								if(ACK == false)
									System.out.print("[Server is offline, message was lost.]\n>>> ");
								else
									System.out.print("[Messages received by the server and saved]\n>>> ");
							}
							else {
								/* Send to client */
								StringBuilder sb = new StringBuilder();
								
								/* Header */
								sb.append("clientMessage$");
								sb.append(clientInfo + "$");	// From
								sb.append(name + "|" + address.getHostAddress() + "|" + port + "$");	// To
								
								/* Build message */
								sb.append(clientName + ": ");
								for(int i=2; i< input.length; i++)
									sb.append(input[i] + " ");
								String message = sb.toString();
								byte[] buffer = message.getBytes();
								
								/* Send packet to user */
								DatagramSocket socket = new DatagramSocket();
								DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
								socket.send(packet);
								
								/* Wait for ACK */
								ACK = false;
								long startTime = System.currentTimeMillis();
								while(System.currentTimeMillis()-startTime < 500 && ACK == false);
								
								if(ACK == false) {
									/* Prepend 's' to packet */
									message = "s$" + clientInfo + "$null|null|0$" + message;
									buffer = message.getBytes();
									DatagramPacket serverPacket = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
									socket.send(serverPacket);
									socket.close();
									
									System.out.printf("[No ACK from %s, message sent to server.]\n>>> ", name);
									System.out.printf("[Message received by the server and saved]\n>>> ");
								}
								else
									System.out.printf("[Message received by %s.]\n>>> ", name);
								socket.close();
							}
//							System.out.print(">>> ");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				else if(category.equals("reg")) {
					if(input.length != 2)
						System.out.println("Usage: reg <nickname>");
					else {
						// Make sure the user is already registered and inactive
						boolean activateClient = false;
						ClientObject[] clients = table.getArray();
						String name = null;
						for(ClientObject c : clients)
							if(c.getName().equals(input[1])) {
								if(c.getActive()) {
									System.out.println("User is already registered and active");
									break;
								}
								name = c.getName();
								activateClient = true;
							}
						if(!activateClient) {
							System.out.printf("User %s does not exist\n", input[1]);
						}
						else {
							// Send request from here
							StringBuilder sb = new StringBuilder();
							sb.append("rereg$");
							sb.append(name + "|null|0$null|null|0$noMessage");
							String message = sb.toString();
							byte[] buffer = message.getBytes();
							
							try {
								DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
								DatagramSocket socket = new DatagramSocket();
								socket.send(packet);
								socket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
				else if(input[0].equals("dereg")) {
					// Send server dereg request
					if(input.length != 2)
						System.out.println("Usage: dereg <nick-name>"); //TODO make sure user is current uesr
					else {
						try {
							String message = "dereg$" + input[1] + "|null|0$null|null|0$noMessage";
							byte[] buffer = message.getBytes();
							
							DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
							DatagramSocket socket = new DatagramSocket();
							socket.send(packet);
							socket.close();
							
							/* Wait for ACK */
							ACK = false;
							long startTime = System.currentTimeMillis();
							int failures = 0;
							while(failures < 5) {
								while(System.currentTimeMillis()-startTime < 500 && ACK == false);
								if(ACK == true)
									break;
								System.out.print("[Server not responding]\n>>> ");
								startTime = System.currentTimeMillis();
								failures++;
							}
							
							if(ACK == false)
								System.out.print("[Exiting]\n>>> ");
							System.out.print("[You are offline. Bye.]\n>>> ");
							
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				else {
					System.out.printf("[Error: invalid request]\n>>> ");
				}
			}
		}
		
		/* Listening for broadcasts */
		else if(Thread.currentThread().getName().equals("Broadcast")) {
//			System.out.println("listening on bcast");
			
			try {
				DatagramSocket socket = new DatagramSocket(clientPort+1);
				
				while(true) {
//					System.out.print(">>> ");
					byte[] buffer = new byte[256];
					
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet); // This thread is waiting to receive
					
					/* Replace table */
					table.clear();
					String rec = new String(packet.getData(), 0, packet.getLength());
					String[] clientStrings = parsePacket(rec, "$");
					for(String c : clientStrings) {
						String[] fields = parsePacket(c, "|");
						table.addClient(new ClientObject(fields[0], fields[1], Integer.parseInt(fields[2]), Boolean.parseBoolean(fields[3])));
					}
//					table.printTable();
					System.out.print("[Client table updated]\n>>> ");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static String[] parsePacket(String s, String delim) {
//		System.out.println("parseInput:" + s);
		ArrayList<String> al = new ArrayList<String>();
		
		StringTokenizer st = new StringTokenizer(s, delim);
		while(st.hasMoreTokens())
			al.add(st.nextToken().trim());
		
		return al.toArray(new String[al.size()]);
	}
	
	private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

}