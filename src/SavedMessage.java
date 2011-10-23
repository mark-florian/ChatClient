public class SavedMessage {
	
	private String name;
	private String IP;
	private int port;
	private String message;
	
	public SavedMessage(String n, String ip, int p, String m) {
		name = n;
		IP = ip;
		port = p;
		message = m;
	}
	
	public String getName() {
		return name;
	}
	public String getIP() {
		return IP;
	}
	public int getPort() {
		return port;
	}
	public String getMessage() {
		return message;
	}
}
