import java.util.*;
import java.io.*;
import java.net.*;

public class Server extends Thread {
	
	//Variables
	protected static DatagramSocket socket = null;
//	protected static MulticastSocket multi = null;
	protected static DatagramSocket bSocket = null;
	protected static int serverPort;
	protected static int multiPort;
	protected static Table table = new Table();
	private static boolean broadcastReady = false;
	
	public Server() throws IOException {
//		this("ServerSide");
	}
	
	public Server(String s) throws IOException {
		super(s);
//		socket = new DatagramSocket(7777);
//		multi = new MulticastSocket(serverPort+1);
	}

	public static void startServer(String port) throws IOException {
		serverPort = Integer.parseInt(port);
		multiPort = serverPort + 1;
		socket = new DatagramSocket(serverPort);
		bSocket = new DatagramSocket(multiPort);
		
		Thread t1 = new Thread(new Server(), "Server");
		Thread t2 = new Thread(new Server(), "Broadcast");
		
		t1.start();
		t2.start();
	}
	
	public void run() {
		/* Server Thread */
		if(Thread.currentThread().getName().equals("Server")) {
			while(true) {
				try {
					byte[] buffer = new byte[256];
					
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);
					
					String[] incoming = parsePacket(new String(packet.getData()), " ");
					
					// Determine what type of request
					if(incoming[0].equals("r")) {
						table.addClient(new ClientObject(incoming[1], incoming[2], incoming[3], true)); // Register new client
						broadcastReady = true;
						System.out.println("bcast is TRUE!");
					}
					else if(incoming[0].equals("dereg")) {
						System.out.println("dereg in the house");
						ArrayList<ClientObject> clients = table.getClients();
						for(int i=0; i<clients.size(); i++) {
							System.out.printf("incoming:<%s>list:<%s>", incoming[1], clients.get(i).getName());
							if(clients.get(i).getName().equals(incoming[1])) {
								System.out.println("condition satisfied");
								clients.get(i).setActive(false);
								break;
							}
						}
						table.replaceClients(clients);
						broadcastReady = true;
					}
					else if(incoming[0].equals("o")) {
						// Offline message
					}
					else if(incoming[0].equals("s")) {
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
						sleep(500);
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
}