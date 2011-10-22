import java.io.*;

public class UdpChat {
	public static void main(String args[]) throws IOException {
		if(args[0].equals("-s")) {
			Server.startServer(args[1]);
			//Server.startBroadcast();
		}
		else if(args[0].equals("-c")) {
			Client.startClient(args[1], args[2], args[3], args[4]);
		}
			
	}
}