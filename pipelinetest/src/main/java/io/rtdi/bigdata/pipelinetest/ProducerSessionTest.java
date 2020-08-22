package io.rtdi.bigdata.pipelinetest;

import java.io.IOException;

import io.rtdi.bigdata.connector.pipeline.foundation.IPipelineBase;
import io.rtdi.bigdata.connector.pipeline.foundation.ProducerSession;
import io.rtdi.bigdata.connector.pipeline.foundation.SchemaHandler;
import io.rtdi.bigdata.connector.pipeline.foundation.avro.JexlGenericData.JexlRecord;
import io.rtdi.bigdata.connector.pipeline.foundation.exceptions.PipelineRuntimeException;
import io.rtdi.bigdata.connector.properties.ProducerProperties;

public class ProducerSessionTest extends ProducerSession<TopicHandlerTest> {

	public ProducerSessionTest(ProducerProperties properties, IPipelineBase<?, TopicHandlerTest> api) {
		super(properties,  api);
	}

	@Override
	public void beginImpl() throws PipelineRuntimeException {
	}

	@Override
	public void commitImpl() throws PipelineRuntimeException {
	}

	@Override
	protected void abort() throws PipelineRuntimeException {
	}

	@Override
	public void open() throws PipelineRuntimeException {
	}

	@Override
	public void close() {
	}

	@Override
	protected void addRowImpl(TopicHandlerTest topic, Integer partition, SchemaHandler handler, JexlRecord keyrecord, JexlRecord valuerecord) throws PipelineRuntimeException {
		topic.addData(keyrecord, valuerecord, handler.getDetails().getKeySchemaID(), handler.getDetails().getValueSchemaID());
		logger.debug("Added a record");
	}

	@Override
	public void confirmInitialLoad(String schemaname, int producerinstance, long rowcount) {
	}

	@Override
	public void markInitialLoadStart(String schemaname, int producerinstance) throws IOException {
	}

	@Override
	public void confirmDeltaLoad(int producerinstance) throws IOException {
	}

}
