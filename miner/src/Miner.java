import java.io.ObjectInputStream;
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
			ObjectInputStream in = new ObjectInputStream(connectionSocket.getInputStream());
			Transaction t = (Transaction) in.readObject();
			System.out.println(t);
			
			// Add the transaction to the block
		}
	}

}
