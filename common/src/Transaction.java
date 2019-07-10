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
	private String tid; // Transaction ID
	private String prevTid; // Previous transaction ID
	private byte[] gv; // Generator Verifier
	private String pubKey;
	
	public Transaction(byte[] data, String pubKey, byte[] gv, String prevTid, TransactionType type) throws IOException {
		this.type = type;
		this.data = data;
		timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		tid = null;
		this.prevTid = prevTid;
		this.gv = gv;
		this.pubKey = pubKey;
		this.setTid();
	}
	
	public byte[] getData() {
		return data;
	}
	
	public String getTid() {
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
		md.update(prevTid.getBytes());
		md.update(gv);
		md.update(pubKey.getBytes());
		tid = new String(md.digest());
	}
	
	public String getPrevTid() {
		return prevTid;
	}
	
	public void setPrevTid(String prevTid) {
		this.prevTid = prevTid;
	}
	
	public String getPubKey() {
		return pubKey;
	}
	
	public TransactionType getType() {
		return type;
	}

	public void clearTransaction() {
		this.type = null;
		this.timestamp = null;
		this.gv = null;
		this.pubKey = null;
	}
}
