import java.io.IOException;

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
				} else if (t.getType() == TransactionType.Remove) {
					jsonGenerator.writeStringField("type", "remove");
				} else {
					jsonGenerator.writeStringField("type", "summary");
				}
			}
			if (t.getData() != null) {
				// TODO - serialize the individual items in the "data" hashmap
				jsonGenerator.writeBinaryField("data", Util.serialize(t.getData()));
			}

			jsonGenerator.writeEndObject();
		}
		jsonGenerator.writeEndArray();
		jsonGenerator.writeEndObject();
	}
}
