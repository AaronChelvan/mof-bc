import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class Node {
	
	public static void main(String[] args) {
		System.out.println("I am a Node");
		while (true) {
			try {
				Socket clientSocket = new Socket("miner1", 8000);
				OutputStream output = clientSocket.getOutputStream();
				PrintWriter writer = new PrintWriter(output, true);
				writer.println("This is a message from the node");
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