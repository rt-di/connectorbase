package io.rtdi.bigdata.connector.pipeline.foundation;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.rtdi.bigdata.connector.pipeline.foundation.exceptions.PipelineCallerException;
import io.rtdi.bigdata.connector.pipeline.foundation.exceptions.PropertiesException;
import io.rtdi.bigdata.connector.pipeline.foundation.utils.GlobalSettings;
import io.rtdi.bigdata.connector.pipeline.foundation.utils.IOUtils;
import io.rtdi.bigdata.connector.properties.ConsumerProperties;
import io.rtdi.bigdata.connector.properties.PipelineConnectionProperties;
import io.rtdi.bigdata.connector.properties.ProducerProperties;

/**
 * The main API definition. Every concrete implementation of a transaction log service is based on this. <br>
 * Each instance can be used for a single tenant only.
 *
 * @param <S> PipelineConnectionProperties
 * @param <T> TopicHandler
 * @param <P> ProducerSession
 * @param <C> ConsumerSession
 */
public abstract class PipelineAbstract<
				S extends PipelineConnectionProperties, 
				T extends TopicHandler, 
				P extends ProducerSession<T>, 
				C extends ConsumerSession<T>> implements Closeable, IPipelineAPI<S, T, P, C> {

	public static final String ALL_SCHEMAS = "-----";
	public static final String AVRO_FIELD_PRODUCERNAME = "ProducerName";
	public static final String AVRO_FIELD_PRODUCER_INSTANCE_NO = "ProducerNumber";

	protected File webinfdir;
	protected Logger logger = LogManager.getLogger(this.getClass().getName());
	protected GlobalSettings settings;
	private static final SchemaRegistryName PRODUCER_TRANSACTIONS_SCHEMA_NAME_ = SchemaRegistryName.create("ProducerTransactions");
	private static final SchemaRegistryName PRODUCER_METADATA_SCHEMA_NAME_ = SchemaRegistryName.create("ProducerMetadata");
	private static final SchemaRegistryName CONSUMER_METADATA_SCHEMA_NAME_ = SchemaRegistryName.create("ConsumerMetadata");
	private static final SchemaRegistryName SERVICE_METADATA_SCHEMA_NAME_ = SchemaRegistryName.create("ServiceMetadata");
	private static final TopicName SCHEMA_TOPIC_NAME_ = TopicName.create("_schemas");
	private static final TopicName PRODUCER_TRANSACTION_TOPIC_NAME_ = TopicName.create("_producertransactions");
	private static final TopicName PRODUCER_METADATA_TOPIC_NAME_ = TopicName.create("ProducerMetadata");
	private static final TopicName CONSUMER_METADATA_TOPIC_NAME_ = TopicName.create("ConsumerMetadata");
	private static final TopicName SERVICE_METADATA_TOPIC_NAME_ = TopicName.create("ServiceMetadata");

	public PipelineAbstract() {
		super();
	}
	
	@Override
	public void setGlobalSettings(GlobalSettings settings) {
		this.settings = settings;
	}
	
	public GlobalSettings getGlobalSettings() {
		return settings;
	}
	
	public TopicName getSchemaRegistryTopicName() {
		if (settings != null && settings.getSchemaRegistryTopicName() != null) {
			return settings.getSchemaRegistryTopicName();
		} else {
			return SCHEMA_TOPIC_NAME_;
		}
	}

	public TopicName getTransactionsTopicName() {
		if (settings != null && settings.getTransactionsTopicName() != null) {
			return settings.getTransactionsTopicName();
		} else {
			return PRODUCER_TRANSACTION_TOPIC_NAME_;
		}
	}
	
	public TopicName getProducerMetadataTopicName() {
		if (settings != null && settings.getProducerMetadataTopicName() != null) {
			return settings.getProducerMetadataTopicName();
		} else {
			return PRODUCER_METADATA_TOPIC_NAME_;
		}
	}
	
	public TopicName getConsumerMetadataTopicName() {
		if (settings != null && settings.getConsumerMetadataTopicName() != null) {
			return settings.getConsumerMetadataTopicName();
		} else {
			return CONSUMER_METADATA_TOPIC_NAME_;
		}
	}
	
	public TopicName getServiceMetadataTopicName() {
		if (settings != null && settings.getServiceMetadataTopicName() != null) {
			return settings.getServiceMetadataTopicName();
		} else {
			return SERVICE_METADATA_TOPIC_NAME_;
		}
	}
	
	public SchemaRegistryName getTransactionsSchemaName() {
		if (settings != null && settings.getTransactionsSchemaName() != null) {
			return settings.getTransactionsSchemaName();
		} else {
			return PRODUCER_TRANSACTIONS_SCHEMA_NAME_;
		}
	}

	public SchemaRegistryName getProducerMetadataSchemaName() {
		if (settings != null && settings.getProducerMetadataSchemaName() != null) {
			return settings.getProducerMetadataSchemaName();
		} else {
			return PRODUCER_METADATA_SCHEMA_NAME_;
		}
	}

	public SchemaRegistryName getConsumerMetadataSchemaName() {
		if (settings != null && settings.getConsumerMetadataSchemaName() != null) {
			return settings.getConsumerMetadataSchemaName();
		} else {
			return CONSUMER_METADATA_SCHEMA_NAME_;
		}
	}

	public SchemaRegistryName getServiceMetadataSchemaName() {
		if (settings != null && settings.getServiceMetadataSchemaName() != null) {
			return settings.getServiceMetadataSchemaName();
		} else {
			return SERVICE_METADATA_SCHEMA_NAME_;
		}
	}

	@Override
	public boolean hasConnectionProperties() {
		Path p = webinfdir.toPath().resolve(this.getAPIName() + ".json");
		File f = p.toFile();
		return f.canRead();
	}

	public abstract void setConnectionProperties(S props);

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#createNewProducerSession(io.rtdi.bigdata.connector.properties.ProducerProperties)
	 */
	@Override
	public P createNewProducerSession(ProducerProperties properties) throws PropertiesException {
		if (properties == null) {
			throw new PipelineCallerException("ProducerSession requires a ProducerProperties object to get its name");
		} else {
			return createProducerSession(properties);
		}
	}
		
	/**
	 * Create a new ProducerSession based on the provided properties. <br>
	 * This method should not throw exceptions as it creates the object only.
	 * 
	 * @param properties Producer specific properties or null
	 * @return A new ProducerSession to be used for connecting against the server and producing records
	 * @throws PropertiesException if something wrong with the properties
	 */
	protected abstract P createProducerSession(ProducerProperties properties) throws PropertiesException;
	
	
	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#createNewConsumerSession(io.rtdi.bigdata.connector.properties.ConsumerProperties)
	 */
	@Override
	public C createNewConsumerSession(ConsumerProperties properties) throws PropertiesException {
		if (properties == null) {
			throw new PropertiesException("ProducerSession requires a ProducerProperties object to get its name");
		} else {
			return createConsumerSession(properties);
		}
	}

	/**
	 * This factory method creates a new ConsumerSession object. <br>
	 * It should also add the concrete topics it does listen on. <br>
	 * This method should not throw exceptions as it creates the object only.
	 * 
	 * @param properties Mandatory parameter as it includes the topics to listen on
	 * @return A new ConsumerSession
	 * @throws PropertiesException if something goes wrong
	 */
	protected abstract C createConsumerSession(ConsumerProperties properties) throws PropertiesException;
	
	
	@Override
	public String getHostName() {
		return IOUtils.getHostname();
	}

	@Override
	public void setWEBINFDir(File webinfdir) {
		this.webinfdir = webinfdir;
	}
	
	public synchronized T getTopicOrCreate(TopicName topic, int partitioncount, short replicationfactor, Map<String, String> configs) throws PropertiesException {
		T t = getTopic(topic);
		if (t == null) {
			t = topicCreate(topic, replicationfactor, replicationfactor, configs);
		} 
		return t;
	}
	
	public T getTopicOrCreate(TopicName topic, int partitioncount, short replicationfactor) throws PropertiesException {
		return getTopicOrCreate(topic, partitioncount, replicationfactor, null);
	}

	@Override
	public T topicCreate(TopicName topic, int partitioncount, short replicationfactor)
			throws PropertiesException {
		return topicCreate(topic, partitioncount, replicationfactor, null);
	}

	@Override
	public void reloadConnectionProperties() throws IOException {
		close();
		loadConnectionProperties();
		open();
	}

}
