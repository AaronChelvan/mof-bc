import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Transaction implements Serializable {

	private static final long serialVersionUID = 1L;
	private String timestamp; // Timestamp of creation
	private String tid; // Transaction ID
	private String prevTid; // Previous transaction ID
	private String input;
	private String output;
	private String signature;
	private String pubKey;
	
	public Transaction() {
		timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		tid = "";
		prevTid = "";
		input = "";
		output = "";
		signature = "";
		pubKey = "";
	}
	
	// Compute the transaction ID by hashing the contents of the transaction
	public void setTid() {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		md.update(timestamp.getBytes());
		md.update(prevTid.getBytes());
		md.update(input.getBytes());
		md.update(output.getBytes());
		md.update(signature.getBytes());
		md.update(pubKey.getBytes());
		tid = new String(md.digest());
	}
	
	public void setPrevTid(String prevTid) {
		this.prevTid = prevTid;
	}
	
	
}
