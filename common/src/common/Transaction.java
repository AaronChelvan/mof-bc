package common;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
public class Transaction {
	
	private String timestamp; // Timestamp of creation
	private String tid; // Transaction ID
	private String prev_tid; // Previous transaction ID
	private String input;
	private String output;
	private String signature;
	private String pub_key;
	
	public Transaction() {
		timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		tid = UUID.randomUUID().toString();
		prev_tid = UUID.randomUUID().toString();
		input = "";
		output = "";
		signature = "";
		pub_key = "";
	}
	
	
}
