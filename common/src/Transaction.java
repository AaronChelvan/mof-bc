import java.io.IOException;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Transaction implements Serializable {

	private static final long serialVersionUID = 1L;
	private TransactionType type;
	private byte[] data;
	private String timestamp; // Timestamp of creation
	private byte[] tid; // Transaction ID
	private byte[] prevTid; // Previous transaction ID
	private byte[] gv; // Generator Verifier
	
	public Transaction(byte[] data, byte[] gv, byte[] prevTid, TransactionType type) throws IOException {
		this.data = data;
		this.gv = gv;
		this.prevTid = prevTid;
		this.type = type;
		timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		this.setTid();
	}
	
	public byte[] getData() {
		return data;
	}
	
	public byte[] getTid() {
		return tid;
	}
	
	// Compute the transaction ID by hashing the contents of the transaction
	public void setTid() throws IOException {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		md.update(type.name().getBytes());
		md.update(data);
		md.update(timestamp.getBytes());
		md.update(prevTid);
		md.update(gv);
		tid = md.digest();
	}
	
	public byte[] getPrevTid() {
		return prevTid;
	}
	
	public void setPrevTid(byte[] prevTid) {
		this.prevTid = prevTid;
	}
	
	public TransactionType getType() {
		return type;
	}
	
	public byte[] getGv() {
		return gv;
	}

	public void clearTransaction() {
		this.type = null;
		this.timestamp = null;
		this.gv = null;
		this.data = null;
	}
}
