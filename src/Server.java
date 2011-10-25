import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.net.*;

public class Server extends Thread {
	
	//Variables
	protected static DatagramSocket socket = null;
	protected static DatagramSocket bSocket = null;
	protected static int serverPort;
	protected static int multiPort;
	protected static Table table = new Table();
	private static boolean broadcastReady = false;
	private static ArrayList<SavedMessage> messages = new ArrayList<SavedMessage>();
	
	public Server() throws IOException {
	}
	
	public Server(String s) throws IOException {
		super(s);
	}

	public static void startServer(String port) throws IOException {
		serverPort = Integer.parseInt(port);
		multiPort = serverPort + 1;
		socket = new DatagramSocket(serverPort);
		bSocket = new DatagramSocket(multiPort);
		
		Thread t1 = new Thread(new Server(), "Listen");
		Thread t2 = new Thread(new Server(), "Broadcast");
		
		t1.start();
		t2.start();
	}
	
	public void run() {
		/* Server Thread */
		if(Thread.currentThread().getName().equals("Listen")) {
			while(true) {
				try {
					byte[] buffer = new byte[256];
					
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);
					
					/* Extract data */
					String[] incoming = parsePacket(new String(packet.getData()), "$");
					String[] fromClient = parsePacket(incoming[1], "|");
					String[] toClient = parsePacket(incoming[2], "|");
					
					String category = incoming[0];
					String fromName = fromClient[0];
					String fromIP = fromClient[1];
					String fromPort = fromClient[2];
					String toName = toClient[0];
					String toIP = toClient[1];
					int toPort = Integer.parseInt(toClient[2]);
					String message = incoming[3];
					
//					String[] incoming = parsePacket(new String(packet.getData()), "$");
					
					// Determine what type of request
					if(category.equals("r")) {
						table.addClient(new ClientObject(fromName, fromIP, fromPort, true)); // Register new client
						broadcastReady = true;
//						System.out.println("bcast is TRUE!");
					}
					else if(category.equals("dereg")) {
//						System.out.println("dereg in the house");
						ArrayList<ClientObject> clients = table.getClients();
						for(int i=0; i<clients.size(); i++) {
//							System.out.printf("incoming:<%s>list:<%s>", incoming[1], clients.get(i).getName());
							if(clients.get(i).getName().equals(incoming[1])) {
//								System.out.println("condition satisfied");
								clients.get(i).setActive(false);
								break;
							}
						}
						table.replaceClients(clients);
						broadcastReady = true;
					}
					else if(category.equals("rereg")) {
						System.out.println("rereg in the house");
						ArrayList<ClientObject> clients = table.getClients();
						ArrayList<String> saved = new ArrayList<String>();
						ArrayList<SavedMessage> newList = new ArrayList<SavedMessage>();
						String ip = null;
						int port = 0;
						
						for(int i=0; i<clients.size(); i++) {
							if(clients.get(i).getName().equals(incoming[1])) {
								clients.get(i).setActive(true);
								ip = clients.get(i).getIP();
								port = clients.get(i).getPort();
								
								//TODO here we must check for saved messages in global list
								//TODO add timestamp
								
								for(int j=0; j< messages.size(); j++) {
									if(messages.get(j).getName().equals(incoming[1]))
										saved.add(messages.get(j).getFrom() + ": <" + messages.get(j).getTime() + "> " + messages.get(j).getMessage());
									else
										newList.add(messages.get(j));
								}
								
								break;
							}
						}
						if(saved.size() > 0) {
							StringBuilder sb = new StringBuilder();
							sb.append("<saved>$");
							for(String s : saved)
								sb.append(s + "$");
//							String message = sb.toString();
							byte[] buf = message.getBytes();
							
							try {
								DatagramSocket socket = new DatagramSocket();
								InetAddress address = InetAddress.getByName(ip);
								DatagramPacket savedPacket = new DatagramPacket(buf, buf.length, address, port);
								socket.send(savedPacket);
								socket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							//TODO build new list and send it from here
							System.out.println(message);
						}
						
						messages = newList;
						
						table.replaceClients(clients);
						broadcastReady = true;
					}
					else if(category.equals("o")) {
						// Offline message
					}
					else if(category.equals("s")) {
						System.out.println("SAVE ME!");
						
//						/* Extract data */
//						String[] fromClient = parsePacket(incoming[1], "|");
//						String fromName = fromClient[0];
//						
//						String[] toClient = parsePacket(incoming[2], "|");
//						String toName = toClient[0];
//						String toIP = toClient[1];
//						int toPort = Integer.parseInt(toClient[2]);
//						
//						String message = incoming[3];
						
						
//						/* Build message */
//						StringBuilder sb = new StringBuilder();
//						for(int i=6; i<incoming.length; i++)
//							sb.append(incoming[i] + " ");
//						String m = sb.toString();
						
//						System.out.printf("incoming0:<%s>\n", incoming[0]);
//						System.out.printf("incoming1:<%s>\n", incoming[1]);
//						System.out.printf("incoming2:<%s>\n", incoming[2]);
//						System.out.printf("incoming3:<%s>\n", incoming[3]);
//						System.out.printf("incoming4:<%s>\n", incoming[4]);
//						System.out.printf("incoming5:<%s>\n", incoming[5]);
						
						messages.add(new SavedMessage(toName, toIP, toPort, message, fromName, getDateTime()));
						
						// Testing
						for(SavedMessage s : messages)
							System.out.printf("%s\t%s\t%d\t%s\t%s\t%s\n", s.getName(), s.getIP(), s.getPort(), s.getMessage(), s.getFrom(), s.getTime());
						// Save message request
					}
					else {
						//TODO This should never happen!
					}
						
//					String message = new String(packet.getData());
					System.out.println("Current Table:");
					table.printTable();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		/* Broadcast Thread */
		else if(Thread.currentThread().getName().equals("Broadcast")) {
			
			try {
//				InetAddress address = InetAddress.getByName("localhost"); //TODO dynamic
				
				while(true) {
					if(broadcastReady) {
						/* Send a packet to each client */
						ClientObject[] clients = table.getArray();
						
						/* Build byte message */
						StringBuilder sb = new StringBuilder();
						for(ClientObject c : clients) {
							sb.append(c.getName());
							sb.append("|");
							sb.append(c.getIP());
							sb.append("|");
							sb.append(c.getPort());
							sb.append("|");
							sb.append(c.getActive());
							sb.append("$");
						}
						String message = sb.toString();
						byte[] buffer = message.getBytes();
						
						/* Send to each client */
						for(ClientObject c : clients) {														
							InetAddress address = InetAddress.getByName(c.getIP());
							
							DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, c.getPort()+1);
							bSocket.send(packet);
						}
						
						broadcastReady = false;
					}
					try {
						sleep(50);	// Check to broadcast every 50ms
					} catch (InterruptedException e) {}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private String[] parsePacket(String s, String delim) {
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