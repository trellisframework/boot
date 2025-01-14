package net.trellisframework.stream.kafka.serde;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.util.Assert;

public class JdkDeserializer implements Deserializer<Object> {

	private final Converter<byte[], Object> deserializer;

	public JdkDeserializer() {
		this(new DeserializingConverter());
	}

	public JdkDeserializer(Converter<byte[], Object> serializer) {
		Assert.notNull(serializer, "Deserializer must not be null");
		this.deserializer = serializer;
	}

	@Override
	public Object deserialize(String topic, byte[] data) {
		try {
			return data == null ? new byte[0] : deserializer.convert(data);
		} catch (Exception ex) {
			throw new SerializationException("Cannot deserialize", ex);
		}
	}
}