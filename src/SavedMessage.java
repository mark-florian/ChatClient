public class SavedMessage {
	
	private String name;
	private String IP;
	private int port;
	private String message;
	private String from;
	private String time;
	
	public SavedMessage(String n, String ip, int p, String m, String f, String t) {
		name = n;
		IP = ip;
		port = p;
		message = m;
		from = f;
		time = t;
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
	public String getFrom() {
		return from;
	}
	public String getTime() {
		return time;
	}
}
