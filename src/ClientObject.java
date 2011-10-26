
public class ClientObject {
	
	private String name;
	private String ipAddress;
	private int portNumber;
	private boolean active;
	
	public ClientObject() {
		name = null;
		ipAddress = null;
		portNumber = 0;
		active = false;
	}
	
	public ClientObject(String n, String i, int p, boolean a) {
		name = n;
		ipAddress = i;
		portNumber = p;
		active = a;
	}
	
	public String getIP() {
		return ipAddress;
	}
	
	public int getPort() {
		return portNumber;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean getActive() {
		return active;
	}
	
	public void setActive(boolean a) {
		active = a;
	}
}