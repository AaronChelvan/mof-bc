import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class Node {
	
	public static void main(String[] args) {
		System.out.println("I am a Node");
		while (true) {
			try {
				// Establish a connection to the miner
				Socket clientSocket = new Socket("miner1", 8000);
				
				// Create a transaction, serialize it, and send it
				ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
				Transaction toSend = new Transaction();
				output.writeObject(toSend);
				
				// Close the connection
				clientSocket.close();
			} catch (Exception e){
				System.out.println(e);
			}
			
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				System.out.println(e);
			}
		}
	}
	
}