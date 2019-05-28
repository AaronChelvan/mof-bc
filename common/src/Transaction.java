import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Transaction implements Serializable {

	private static final long serialVersionUID = 1L;
	private String data;
	private String timestamp; // Timestamp of creation
	private String tid; // Transaction ID
	private String prevTid; // Previous transaction ID
	private String input;
	private String output;
	private String signature;
	private String pubKey;
	
	public Transaction() {
		data = UUID.randomUUID().toString(); // Generate a random string
		timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		tid = "";
		prevTid = "";
		input = "";
		output = "";
		signature = "";
		pubKey = "";
	}
	
	public String getData() {
		return data;
	}
	
	public String getTid() {
		return tid;
	}
	
	// Compute the transaction ID by hashing the contents of the transaction
	public void setTid() {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		md.update(data.getBytes());
		md.update(timestamp.getBytes());
		md.update(prevTid.getBytes());
		md.update(input.getBytes());
		md.update(output.getBytes());
		md.update(signature.getBytes());
		md.update(pubKey.getBytes());
		tid = new String(md.digest());
	}
	
	public String getPrevTid() {
		return prevTid;
	}
	
	public void setPrevTid(String prevTid) {
		this.prevTid = prevTid;
	}
}
