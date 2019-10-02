import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class CustomBlockSerializer extends StdSerializer<Block> {

	private static final long serialVersionUID = 1L;

	public CustomBlockSerializer() {
		this(null);
	}

	public CustomBlockSerializer(Class<Block> t) {
		super(t);
	}

	@Override
	public void serialize(
		Block block, JsonGenerator jsonGenerator, SerializerProvider serializer) throws IOException {
		jsonGenerator.writeStartObject();
		jsonGenerator.writeBinaryField("blockId", block.getBlockId());
		jsonGenerator.writeBinaryField("prevBlockId", block.getPrevBlockId());
		jsonGenerator.writeArrayFieldStart("transactions");
		for (Transaction t: block.getTransactions()) {
			// Extract all data from the transaction
			jsonGenerator.writeStartObject();
			jsonGenerator.writeBinaryField("tid", t.getTid());
			jsonGenerator.writeBinaryField("prevTid", t.getPrevTid());
			if (t.getGv() != null) {
				jsonGenerator.writeBinaryField("gv", t.getGv());
			}
			if (t.getTimestamp() != null) {
				jsonGenerator.writeStringField("timestamp", t.getTimestamp());
			}
			if (t.getType() != null) {
				if (t.getType() == TransactionType.Standard) {
					jsonGenerator.writeStringField("type", "standard");
					HashMap<String, byte[]> data = t.getData();
					jsonGenerator.writeBinaryField("data", data.get("data"));
				} else if (t.getType() == TransactionType.Remove) {
					jsonGenerator.writeStringField("type", "remove");
					HashMap<String, byte[]> data = t.getData();
					jsonGenerator.writeBinaryField("location", data.get("location"));
					jsonGenerator.writeBinaryField("pubKey", data.get("pubKey"));
					jsonGenerator.writeBinaryField("unsignedGv", data.get("unsignedGv"));
					jsonGenerator.writeBinaryField("sigMessage", data.get("sigMessage"));
					jsonGenerator.writeBinaryField("sig", data.get("sig"));
					
					/*transactionData.put("location", Util.serialize(tl));
					transactionData.put("pubKey", Util.serialize(publicKey));
					transactionData.put("unsignedGv", computeGv(tl.getPrevTransactionID(), false));
					byte[] sigMessage = new byte[20]; // Generate a signature message
					new Random().nextBytes(sigMessage);
					transactionData.put("sigMessage", sigMessage);
					transactionData.put("sig", Util.sign(privateKey, sigMessage));*/
				} else {
					jsonGenerator.writeStringField("type", "summary");
					// TODO - serialize the items in a summary transaction
				}
			}

			jsonGenerator.writeEndObject();
		}
		jsonGenerator.writeEndArray();
		jsonGenerator.writeEndObject();
	}
}
