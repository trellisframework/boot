package net.trellisframework.stream.kafka.serde;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.util.Assert;

public class JdkSerializer implements Serializer<Object> {

	private final Converter<Object, byte[]> serializer;

	public JdkSerializer() {
		this(new SerializingConverter());
	}

	public JdkSerializer(Converter<Object, byte[]> serializer) {
		Assert.notNull(serializer, "Serializer must not be null");
		this.serializer = serializer;
	}

	@Override
	public byte[] serialize(String topic, Object data) {
		try {
			return data == null ? new byte[0] : serializer.convert(data);
		} catch (Exception ex) {
			throw new SerializationException("Cannot serialize", ex);
		}
	}
}