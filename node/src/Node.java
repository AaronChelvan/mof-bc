import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class Node {
	
	public static void main(String[] args) throws Exception {
		System.out.println("I am a Node");
		Socket clientSocket = new Socket("localhost", 8000);
		OutputStream output = clientSocket.getOutputStream();
		PrintWriter writer = new PrintWriter(output, true);
		writer.println("This is a message from the node");
		clientSocket.close();
	}
	
}