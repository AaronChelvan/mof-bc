import java.io.IOException;
import java.util.ArrayList;

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

		block.setBlockId(node.get("blockId").asText().getBytes());
		block.setPrevBlockId(node.get("prevBlockId").asText().getBytes());
		ArrayList<Transaction> transactionList = null;
		try {
			transactionList = Util.deserialize(node.get("transactions").binaryValue());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		block.setTransactions(transactionList);
		
		/*
		JsonNode transactions = node.get("transactions");
		for (JsonNode n : transactions) {
			Transaction t = new Transaction();
			t.setTid(n.get("tid").asText().getBytes());
			t.setPrevTid(n.get("prevTid").asText().getBytes());
			t.setGv(n.get("gv").asText().getBytes());
			t.setTimestamp(n.get("timestamp").asText());
			if (n.get("type").asText().equals("standard")) {
				t.setType(TransactionType.Standard);
			} else if (n.get("type").asText().equals("remove")) {
				t.setType(TransactionType.Remove);
			} else if (n.get("type").asText().equals("summary")) {
				t.setType(TransactionType.Summary);
			} else {
				System.out.println("Error");
				System.exit(0);
			}
			
			HashMap<String, byte[]> data = new HashMap<String, byte[]>();
			try {
				data = Util.deserialize(n.get("data").asText().getBytes());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			t.setData(data);
			
			block.addTransaction(t);
	    }
	    */
		return block;
	}
}
