package io.rtdi.bigdata.connectors.pipeline.kafkadirect;

import java.util.List;
import java.util.Properties;

import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import io.rtdi.bigdata.connector.pipeline.foundation.MicroServiceTransformation;
import io.rtdi.bigdata.connector.pipeline.foundation.ServiceSession;
import io.rtdi.bigdata.connector.pipeline.foundation.TopicName;
import io.rtdi.bigdata.connector.pipeline.foundation.avro.JexlGenericData.JexlRecord;
import io.rtdi.bigdata.connector.pipeline.foundation.exceptions.PropertiesException;
import io.rtdi.bigdata.connector.properties.ServiceProperties;

public class ServiceSessionKafkaDirect extends ServiceSession {
	private KafkaStreams stream;
	private KafkaAPIdirect api;
	private ServiceProperties properties;

	public ServiceSessionKafkaDirect(ServiceProperties properties, KafkaAPIdirect api) {
		this.api = api;
		this.properties = properties;
	}

	@Override
	public void start() throws PropertiesException {
		if (properties.getSchemaTransformations() != null && properties.getSchemaTransformations().size() > 0) {
			Properties streamsConfiguration = new Properties();
			streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "rtdi.io microservice." + properties.getName());
		    streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, properties.getName());
		    streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, api.getAPIProperties().getKafkaBootstrapServers());
		    streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass().getName());
		    streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, GenericAvroSerde.class);
		    streamsConfiguration.put(GenericAvroSerde.SCHEMA_PROVIDER_CONFIG, api);
		    KafkaConnectionProperties connectionprops = api.getAPIProperties();
			if (connectionprops.getKafkaAPIKey() != null && connectionprops.getKafkaAPIKey().length() > 0) {
				/*
				 * 	security.protocol=SASL_SSL
				 *	sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule   required username="{{ CLUSTER_API_KEY }}"   password="{{ CLUSTER_API_SECRET }}";
				 *	ssl.endpoint.identification.algorithm=https
				 *	sasl.mechanism=PLAIN
				 */
				streamsConfiguration.put("security.protocol", "SASL_SSL");
				streamsConfiguration.put(SaslConfigs.SASL_JAAS_CONFIG, 
						"org.apache.kafka.common.security.plain.PlainLoginModule   required username=\"" +
								connectionprops.getKafkaAPIKey() + "\"   password=\"" + 
								connectionprops.getKafkaAPISecret() + "\";");
				streamsConfiguration.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, SslConfigs.DEFAULT_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM);
				streamsConfiguration.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
			}    	
	
		    TopicName source = new TopicName(properties.getSourceTopic());
		    TopicName target = new TopicName(properties.getTargetTopic());
		    api.getTopicOrCreate(target.getName(), 1, (short) 1); // make sure the target topic exists
		    StreamsBuilder builder = new StreamsBuilder();
		    KStream<byte[], JexlRecord> input = builder.stream(source.getName());
	
		    KStream<byte[], JexlRecord> microservice = input.transformValues(() -> new ValueMapperMicroService(properties.getSchemaTransformations()));
	
			microservice.to(target.getName());
	
		    stream = new KafkaStreams(builder.build(), streamsConfiguration);
	
			stream.start();
		} else {
			throw new PropertiesException("No microservices configured for this service", (Exception) null, null, properties.getName());
		}
	}

	@Override
	public void stop() {
		if (stream != null) {
			stream.close();
		}
	}

	@Override
	public long getRowsProcessed() {
		if (properties.getSchemaTransformations() != null) {
			long rowcount = 0L;
			for (List<? extends MicroServiceTransformation> transformations : properties.getSchemaTransformations().values()) {
				if (transformations != null && transformations.size() > 0) {
					rowcount += transformations.get(0).getRowProcessed();
				}
			}
			return rowcount;
		} else {
			return 0;
		}
	}

	@Override
	public Long getLastRowProcessed() {
		if (properties.getSchemaTransformations() != null) {
			long maxdate = 0L;
			for (List<? extends MicroServiceTransformation> transformations : properties.getSchemaTransformations().values()) {
				if (transformations != null && transformations.size() > 0) {
					long d = transformations.get(0).getRowProcessed();
					if (d > maxdate) {
						maxdate = d;
					}
				}
			}
			return maxdate;
		} else {
			return null;
		}
	}

}
