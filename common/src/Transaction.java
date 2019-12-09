import java.io.IOException;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class Transaction implements Serializable {

	private static final long serialVersionUID = 1L;
	private TransactionType type;
	private HashMap<String, byte[]> data;
	private String timestamp; // Timestamp of creation
	private byte[] tid; // Transaction ID
	private byte[] prevTid; // Previous transaction ID
	private byte[] gv; // Generator Verifier
	
	// Constructor
	public Transaction(HashMap<String, byte[]> data, byte[] gv, byte[] prevTid, TransactionType type) throws IOException {
		this.data = data;
		this.gv = gv;
		this.prevTid = prevTid;
		this.type = type;
		timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		this.setTid();
	}
	
	// Constructor for an empty transaction
	public Transaction() {
		this.data = null;
		this.gv = null;
		this.tid = null;
		this.prevTid = null;
		this.type = null;
		this.timestamp = null;
	}
	
	// Returns transaction data
	public HashMap<String, byte[]> getData() {
		return data;
	}
	
	// Returns the transaction ID
	public byte[] getTid() {
		return tid;
	}
	
	// Returns the timestamp
	public String getTimestamp() {
		return timestamp;
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
		md.update(Util.serialize(data));
		md.update(timestamp.getBytes());
		md.update(prevTid);
		md.update(gv);
		tid = md.digest();
	}
	
	// Set the transaction ID
	public void setTid(byte[] tid) {
		this.tid = tid;
	}
	
	// Get the previous transaction ID
	public byte[] getPrevTid() {
		return prevTid;
	}
	
	// Set the previous transaction ID
	public void setPrevTid(byte[] prevTid) {
		this.prevTid = prevTid;
	}
	
	// Get the type of the transaction
	public TransactionType getType() {
		return type;
	}
	
	// Get the Generator Verifier
	public byte[] getGv() {
		return gv;
	}
	
	// Set the Generator Verifier
	public void setGv(byte[] gv) {
		this.gv = gv;
	}
	
	// Set the timestamp
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	
	// Set the transaction type
	public void setType(TransactionType t) {
		this.type = t;
	}
	
	// Set the transaction data
	public void setData(HashMap<String, byte[]> data) {
		this.data = data;
	}

	// Delete the contents of this transaction
	public void clearTransaction() {
		this.type = null;
		this.timestamp = null;
		this.gv = null;
		this.data = null;
	}
}
