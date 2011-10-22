import java.util.*;

public class Table {
	
	private ArrayList<ClientObject> clients;
	
	public Table() {
		clients = new ArrayList<ClientObject>();
	}
	
	public void addClient(ClientObject c) {
		clients.add(c);
	}
	
	public void removeClient(int i) {
		clients.remove(i);
	}
	
	public ArrayList<ClientObject> getClients() {
		return clients;
	}
	
	public void printTable() {
		for(ClientObject c : clients)
			System.out.printf("%s\t%s\t%s\t%s\n", c.getName(), c.getIP(), c.getPort(), c.getActive());
	}
	
	public ClientObject[] getArray() {
		return clients.toArray(new ClientObject[clients.size()]);
	}
	
	public void clear() {
		clients.clear();
	}
	
	public void replaceClients(ArrayList<ClientObject> list) {
		clients = list;
	}
}