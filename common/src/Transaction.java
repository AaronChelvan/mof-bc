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
	private String data;
	private String timestamp; // Timestamp of creation
	private String tid; // Transaction ID
	private String prevTid; // Previous transaction ID
	private String signature;
	private String pubKey;
	private ArrayList<TransactionLocation> locations;
	
	public Transaction(String data, String pubKey, TransactionType type) {
		this.type = type;
		this.data = data;
		timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		tid = "";
		prevTid = "";
		signature = "";
		this.pubKey = pubKey;
		locations = new ArrayList<TransactionLocation>();
	}
	
	public String getData() {
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
		md.update(data.getBytes());
		md.update(timestamp.getBytes());
		md.update(prevTid.getBytes());
		md.update(signature.getBytes());
		md.update(pubKey.getBytes());
		md.update(Util.serialize(locations));
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
	
	public void addLocation(TransactionLocation tl) {
		locations.add(tl);
	}
	
	public TransactionLocation getRemoveLocation() {
		return locations.get(0);
	}
	
	public ArrayList<TransactionLocation> getSummaryLocations() {
		return locations;
	}

	public void clearTransaction() {
		this.type = null;
		this.timestamp = null;
		this.signature = null;
		this.pubKey = null;
	}
}
