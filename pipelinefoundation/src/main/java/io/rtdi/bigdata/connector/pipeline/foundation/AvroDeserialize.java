package io.rtdi.bigdata.connector.pipeline.foundation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;

import com.github.benmanes.caffeine.cache.Cache;

import io.rtdi.bigdata.connector.pipeline.foundation.avro.JexlGenericData.JexlRecord;
import io.rtdi.bigdata.connector.pipeline.foundation.avro.JexlGenericDatumReader;
import io.rtdi.bigdata.connector.pipeline.foundation.avrodatatypes.LogicalDataTypesRegistry;
import io.rtdi.bigdata.connector.pipeline.foundation.exceptions.PipelineRuntimeException;
import io.rtdi.bigdata.connector.pipeline.foundation.utils.IOUtils;

/**
 * This is class does deserialize Avro records. It uses the same format as Kafka itself, hence
 * even if data is serialized by others, e.g. Kafka Connect, it can be consumed.
 *
 */
public class AvroDeserialize {

	private static final DecoderFactory decoderFactory = DecoderFactory.get();
	
	static {
		LogicalDataTypesRegistry.registerAll();
	}

	/**
	 * Avoid using this version as it requests the schema from the server always, no caching.
	 * 
	 * @param data as binary
	 * @param registry used to get the schema from
	 * @return the JexlRecord, an extended Avro GenericRecord
	 * @throws IOException in case the byte array cannot be converted to a record
	 */
	public static JexlRecord deserialize(byte[] data, ISchemaRegistrySource registry) throws IOException {
		return deserialize(data, registry, null);
	}

	/**
	 * In order to avoid double caching the contract is that the PipelineAbstract does not cache schemas and other objects. 
	 * Hence this version of the method would read the schema from the registry for every call. 
	 * Therefore a cache can be provided and then the method checks the cache and adds schemas to the cache for reuse.
	 * The alternative is of course that the ISchemaRegistrySource itself does cache the schemas already, like it does for the ConsumerSession.
	 * 
	 * @param data with the binary Avro representation
	 * @param registry is the provider of the schema
	 * @param schemacache An optional map used as a cache between calls
	 * @return AvroRecord in Jexl abstraction
	 * @throws IOException In case anything went wrong
	 */
	public static JexlRecord deserialize(byte[] data, ISchemaRegistrySource registry, Cache<Integer, Schema> schemacache) throws IOException {
		if (data != null) {
			ByteBuffer schemaidb = ByteBuffer.allocate(Integer.BYTES);
			int schemaid = -1;
			try (ByteArrayInputStream in = new ByteArrayInputStream(data); ) {
				int b = in.read();
				if (b != IOUtils.MAGIC_BYTE) {
					throw new PipelineRuntimeException("Not a valid row frame");
				} else {
					in.read(schemaidb.array());
					schemaid = schemaidb.getInt();
					
					Schema schema = null;
					if (schemacache != null) {
						schema = schemacache.getIfPresent(schemaid);
					}
					if (schema == null) {
						schema = registry.getSchema(schemaid);
						if (schema == null) {
							throw new PipelineRuntimeException("Schema " + schemaid + " not found");
						} else if (schemacache != null) {
							schemacache.put(schemaid, schema);
						}
					}
					BinaryDecoder decoder = decoderFactory.directBinaryDecoder(in, null);
					DatumReader<JexlRecord> reader = new JexlGenericDatumReader<>(schema);
					JexlRecord rec = reader.read(null, decoder);
					rec.setSchemaId(schemaid);
					return rec;
				}
			}
		} else {
			return null;
		}
	}
}
