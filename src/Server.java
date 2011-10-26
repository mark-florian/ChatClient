import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.net.*;

public class Server extends Thread {
	
	protected static int serverPort;
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
		
		Thread t1 = new Thread(new Server(), "Listen");
		Thread t2 = new Thread(new Server(), "Broadcast");
		
		t1.start();
		t2.start();
	}
	
	public void run() {
		/* Listen Thread */
		if(Thread.currentThread().getName().equals("Listen")) {
			while(true) {
				try {
					byte[] buffer = new byte[256];
					
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					DatagramSocket socket = new DatagramSocket(serverPort);
					socket.receive(packet);
					socket.close();
					
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
					
					// Determine what type of request
					if(category.equals("r")) {
						table.addClient(new ClientObject(fromName, fromIP, fromPort, true)); // Register new client
						broadcastReady = true;
					}
					else if(category.equals("dereg")) {
						ArrayList<ClientObject> clients = table.getClients();
						String fIP = null;
						int fPort = 0;
						for(int i=0; i<clients.size(); i++) {
							if(clients.get(i).getName().equals(fromName)) {
								clients.get(i).setActive(false);
								fIP = clients.get(i).getIP();
								fPort = clients.get(i).getPort();
								break;
							}
						}
						table.replaceClients(clients);
						broadcastReady = true;
						
						/* Send ACK */
						byte[] ack = "ACK$null|null|0$null|null|0$noMessage".getBytes();
						DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, InetAddress.getByName(fIP), fPort);
						DatagramSocket ackSocket = new DatagramSocket();
						ackSocket.send(ackPacket);
						ackSocket.close();
					}
					else if(category.equals("rereg")) {
						ArrayList<ClientObject> clients = table.getClients();
						ArrayList<String> saved = new ArrayList<String>();
						ArrayList<SavedMessage> newList = new ArrayList<SavedMessage>();
						String ip = null;
						int port = 0;
						
						for(int i=0; i<clients.size(); i++) {
							if(clients.get(i).getName().equals(fromName)) {
								clients.get(i).setActive(true);
								ip = clients.get(i).getIP();
								port = clients.get(i).getPort();
								
								for(int j=0; j< messages.size(); j++) {
									if(messages.get(j).getName().equals(fromName))
										saved.add(messages.get(j).getFrom() + ": <" + messages.get(j).getTime() + "> " + messages.get(j).getMessage());
									else
										newList.add(messages.get(j));
								}
								break;
							}
						}
						if(saved.size() > 0) {
							StringBuilder sb = new StringBuilder();
							sb.append("saved$");
							sb.append("null|null|0$null|null|0$");
							for(String s : saved)
								sb.append(s + "\n>>> ");
							String savedMessages = sb.toString();
							byte[] buf = savedMessages.getBytes();
							
							try {
								InetAddress address = InetAddress.getByName(ip);
								DatagramPacket savedPacket = new DatagramPacket(buf, buf.length, address, port);
								DatagramSocket savedSocket = new DatagramSocket();
								savedSocket.send(savedPacket);
								savedSocket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							System.out.println(message);
						}
						
						messages = newList;
						
						table.replaceClients(clients);
						broadcastReady = true;
					}
					else if(category.equals("s")) {
						messages.add(new SavedMessage(toName, toIP, toPort, message, fromName, getDateTime()));
						
						/* Send ACK */
						byte[] ack = "ACK$null|null|0$null|null|0$noMessage".getBytes();
						DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, InetAddress.getByName(fromIP), fromPort);
						DatagramSocket ackSocket = new DatagramSocket();
						ackSocket.send(ackPacket);
						ackSocket.close();
					}
					else {
						//TODO This should never happen!
					}
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
				while(true) {
					if(broadcastReady) {
						/* Send a packet to each client */
						ClientObject[] clients = table.getArray();
						
						/* Build byte message */
						StringBuilder sb = new StringBuilder();
						sb.append("broadcast$");
						sb.append("null|null|0$null|null|0$");
						for(ClientObject c : clients) {
							sb.append(c.getName());
							sb.append("|");
							sb.append(c.getIP());
							sb.append("|");
							sb.append(c.getPort());
							sb.append("|");
							sb.append(c.getActive());
							sb.append("%");
						}
						String message = sb.toString();
						byte[] buffer = message.getBytes();
						
						/* Send to each client */
						for(ClientObject c : clients) {														
							InetAddress address = InetAddress.getByName(c.getIP());
							
							DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, c.getPort());
							DatagramSocket socket = new DatagramSocket();
							socket.send(packet);
							socket.close();
						}
						broadcastReady = false;
					}
					try {
						sleep(100);	// Check to broadcast every 50ms
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