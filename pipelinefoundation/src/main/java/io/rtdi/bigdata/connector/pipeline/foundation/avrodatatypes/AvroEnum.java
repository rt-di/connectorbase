package io.rtdi.bigdata.connector.pipeline.foundation.avrodatatypes;

import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes.LogicalTypeFactory;
import org.apache.avro.Schema.Type;
import org.apache.avro.Schema;

/**
 * Wrapper around the Avro Type.ENUM data type
 *
 */
public class AvroEnum extends LogicalType implements IAvroPrimitive {
	public static final Factory factory = new Factory();
	public static final String NAME = "ENUM";
	private static AvroEnum element = new AvroEnum();

	public static Schema getSchema(Schema schema) {
		return create().addToSchema(schema);
	}

	public AvroEnum() {
		super(NAME);
	}

	public static AvroEnum create() {
		return element;
	}

	@Override
	public Schema addToSchema(Schema schema) {
		return super.addToSchema(schema);
	}

	@Override
	public void validate(Schema schema) {
		super.validate(schema);
		// validate the type
		if (schema.getType() != Schema.Type.ENUM) {
			throw new IllegalArgumentException("Logical type must be backed by a ENUM");
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		return true;
	}

	@Override
	public int hashCode() {
		return 1;
	}
	
	@Override
	public String toString() {
		return NAME;
	}

	@Override
	public Object convertToInternal(Object value) {
		return value;
	}

	public static class Factory implements LogicalTypeFactory {
		
		public Factory() {
		}

		@Override
		public LogicalType fromSchema(Schema schema) {
			return AvroEnum.create();
		}

	}

	@Override
	public void toString(StringBuffer b, Object value) {
		if (value != null) {
			b.append('\"');
			b.append(value.toString());
			b.append('\"');
		}
	}

	@Override
	public Type getBackingType() {
		return Type.ENUM;
	}

	@Override
	public Schema getDatatypeSchema() {
		return null;
	}

	@Override
	public AvroType getAvroType() {
		return AvroType.AVROENUM;
	}

}
