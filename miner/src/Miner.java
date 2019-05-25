import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Miner {

	public static void main(String[] args) throws Exception {
		System.out.println("I am a Miner");
		ArrayList<Block> blockchain = new ArrayList<Block>();
		ServerSocket minerSocket = new ServerSocket(8000);
		
		while (true) {
			Socket connectionSocket = minerSocket.accept();
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		    String clientSentence = inFromClient.readLine();
		    System.out.println(clientSentence);
		}
	}

}
