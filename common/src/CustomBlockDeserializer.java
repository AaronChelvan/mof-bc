import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class CustomBlockDeserializer extends StdDeserializer<Block> {

	private static final long serialVersionUID = 1L;

	public CustomBlockDeserializer() {
		this(null);
	}

	public CustomBlockDeserializer(Class<?> vc) {
		super(vc);
	}
 
	@Override
	public Block deserialize(JsonParser parser, DeserializationContext deserializer) throws IOException {
		Block block = new Block();
		ObjectCodec codec = parser.getCodec();
		JsonNode node = codec.readTree(parser);

		block.setBlockId(node.get("blockId").binaryValue());
		block.setPrevBlockId(node.get("prevBlockId").binaryValue());
		/*ArrayList<Transaction> transactionList = null;
		try {
			transactionList = Util.deserialize(node.get("transactions").binaryValue());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		block.setTransactions(transactionList);*/
		

		JsonNode transactions = node.get("transactions");
		for (JsonNode n : transactions) {
			Transaction t = new Transaction();
			t.setTid(n.get("tid").binaryValue());
			t.setPrevTid(n.get("prevTid").binaryValue());
			if (n.get("gv") != null) {
				t.setGv(n.get("gv").binaryValue());
			}
			if (n.get("timestamp") != null) {
				t.setTimestamp(n.get("timestamp").asText());
			}
			if (n.get("type") != null) {
				if (n.get("type").asText().equals("standard")) {
					t.setType(TransactionType.Standard);
					HashMap<String, byte[]> data = new HashMap<String, byte[]>();
					data.put("data", n.get("data").binaryValue());
					t.setData(data);
				} else if (n.get("type").asText().equals("remove")) {
					t.setType(TransactionType.Remove);
					HashMap<String, byte[]> data = new HashMap<String, byte[]>();
					data.put("location", n.get("location").binaryValue());
					data.put("pubKey", n.get("pubKey").binaryValue());
					data.put("unsignedGv", n.get("unsignedGv").binaryValue());
					data.put("sigMesage", n.get("sigMessage").binaryValue());
					data.put("sig", n.get("sig").binaryValue());					
					t.setData(data);
				} else if (n.get("type").asText().equals("summary")) {
					t.setType(TransactionType.Summary);
				} else {
					System.out.println("Error");
					System.exit(0);
				}
			}
			
			block.addTransaction(t);
	    }

		return block;
	}
}
