package io.rtdi.bigdata.connector.connectorframework.controller;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import io.rtdi.bigdata.connector.connectorframework.BrowsingService;
import io.rtdi.bigdata.connector.connectorframework.IConnectorFactory;
import io.rtdi.bigdata.connector.connectorframework.IConnectorFactoryConsumer;
import io.rtdi.bigdata.connector.connectorframework.IConnectorFactoryProducer;
import io.rtdi.bigdata.connector.connectorframework.exceptions.ConnectorCallerException;
import io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI;
import io.rtdi.bigdata.connector.pipeline.foundation.enums.ControllerExitType;
import io.rtdi.bigdata.connector.pipeline.foundation.exceptions.PropertiesException;
import io.rtdi.bigdata.connector.properties.ConnectionProperties;
import io.rtdi.bigdata.connector.properties.ConsumerProperties;
import io.rtdi.bigdata.connector.properties.ProducerProperties;

public class ConnectionController extends Controller<Controller<?>> {
	public static final String DIR_CONSUMERS = "consumers";
	public static final String DIR_PRODUCERS = "producers";

	private File connectiondir;
	private ConnectionProperties connectionprops = null;
	private ConnectorController connectorcontroller;
	private HashMap<String, ProducerController> producers = new HashMap<>();
	private HashMap<String, ConsumerController> consumers = new HashMap<>();
	private BrowsingService<?> browser = null;

	public ConnectionController(File connectiondir, ConnectorController connectorcontroller) {
		super(connectiondir.getName());
		this.connectiondir = connectiondir;
		this.connectorcontroller = connectorcontroller;
	}
	
	/**
	 * Read the connection settings and its children from the specified directory.
	 * The directory tree is:
	 * 
	 * connectorname/connectionname/
	 *                           /connectionname.json
	 *                           /consumers/
	 *                                     /consumername1.json
	 *                           /producers/
	 *                                     /producername1.json
	 * 
	 * @throws PropertiesException if properties are invalid or the configuration file cannot be found
	 */
	public void readConfigs() throws PropertiesException {
		if (connectiondir == null) {
			throw new PropertiesException("connectiondirectory is not set");
		} else if (!connectiondir.exists()) {
			throw new PropertiesException("connectiondirectory \"" + connectiondir.getAbsolutePath() + "\" does not exist");
		} else if (!connectiondir.isDirectory()) {
			throw new PropertiesException("connectiondirectory \"" + connectiondir.getAbsolutePath() + "\"is no directory");
		}
		
		logger.info("readingConfigs for connector \"" + getName() + "\"");
		
		IConnectorFactory<?> connectorfactory = connectorcontroller.getConnectorFactory();
		connectionprops = connectorfactory.createConnectionProperties(connectiondir.getName());
		connectionprops.read(connectiondir);

	    File consumerdirroot = new File(connectiondir, DIR_CONSUMERS);
	    if (connectorfactory instanceof IConnectorFactoryConsumer && consumerdirroot.isDirectory()) {
	    	File[] files = consumerdirroot.listFiles();
	    	if (files != null) {
	    		for (File consumerconfigfile : files) {
	    			if (consumerconfigfile.isFile() && consumerconfigfile.getName().endsWith(".json")) {
		    			String consumerconfigfilename = consumerconfigfile.getName();
		    			int pos = consumerconfigfilename.lastIndexOf('.');
		    			String consumername = consumerconfigfilename.substring(0, pos);
		    			ConsumerProperties consumerprops = ((IConnectorFactoryConsumer<?,?>) connectorfactory).createConsumerProperties(consumername);
		    			consumerprops.read(consumerdirroot);
		    			addConsumer(consumerprops);
	    			}
	    		}
	    	}
	    }
	    File producerdirroot = new File(connectiondir, DIR_PRODUCERS);
	    if (connectorfactory instanceof IConnectorFactoryProducer && producerdirroot.isDirectory()) {
	    	File[] files = producerdirroot.listFiles();
	    	if (files != null) {
	    		for (File producerfile : files) {
	    			String producerfilename = producerfile.getName();
	    			int pos = producerfilename.lastIndexOf('.');
	    			if (producerfilename.substring(pos).equalsIgnoreCase(".json")) {
		    			String producername = producerfilename.substring(0, pos);
		    			ProducerProperties producerproperties = ((IConnectorFactoryProducer<?,?>) connectorfactory).createProducerProperties(producername);
		    			producerproperties.read(producerdirroot);
		    			addProducer(producerproperties);
	    			}
	    		}
	    	}
	    }
	}
	
	public void addConsumer(ConsumerProperties consumerprops) throws PropertiesException {
		ConsumerController consumer = new ConsumerController(consumerprops, this);
		addChild(consumerprops.getName(), consumer);
		consumers.put(consumerprops.getName(), consumer);
	}

	public ProducerController addProducer(ProducerProperties producerprops) throws PropertiesException {
		ProducerController producer = new ProducerController(producerprops, this);
		producers.put(producerprops.getName(), producer);
		addChild(producerprops.getName(), producer);
		return producer;
	}

	public boolean removeProducer(ProducerController producer) {
		producers.remove(producer.getProducerProperties().getName());
		producer.disableController();
		File producerfile = new File(connectiondir, DIR_PRODUCERS + File.separatorChar + producer.getProducerProperties().getName() + ".json");
		return producerfile.delete();
	}

	public boolean removeConsumer(ConsumerController consumer) {
		consumers.remove(consumer.getConsumerProperties().getName());
		consumer.disableController();
		File consumerfile = new File(connectiondir, DIR_CONSUMERS + File.separatorChar + consumer.getConsumerProperties().getName() + ".json");
		return consumerfile.delete();
	}

	@Override
	protected void stopControllerImpl(ControllerExitType exittype) {
		super.stopChildControllers(exittype);
		if (browser != null) {
			browser.close();
			browser = null;
		}
	}

	@Override
	protected void startControllerImpl() throws IOException {
		startChildController();
	}

	@Override
	protected String getControllerType() {
		return "ConnectionController";
	}

	public ConnectionProperties getConnectionProperties() {
		return connectionprops;
	}

	public IPipelineAPI<?, ?, ?, ?> getPipelineAPI() {
		return connectorcontroller.getPipelineAPI();
	}

	public IConnectorFactory<?> getConnectorFactory() {
		return connectorcontroller.getConnectorFactory();
	}

	public HashMap<String, ProducerController> getProducers() {
		return producers;
	}

	public HashMap<String, ConsumerController> getConsumers() {
		return consumers;
	}

	public ConsumerController getConsumerOrFail(String consumername) throws ConnectorCallerException {
		ConsumerController c = consumers.get(consumername);
		if (c == null) {
			throw new ConnectorCallerException("Connector has no consumer of that name", null, "getConsumer() was called for a non-existing name", consumername);
		} else {
			return c;
		}
	}

	public ProducerController getProducerOrFail(String producername) throws ConnectorCallerException {
		ProducerController c = producers.get(producername);
		if (c == null) {
			throw new ConnectorCallerException("Connector has no producer of that name", null, "getProducer() was called for a non-existing name", producername);
		} else {
			return c;
		}
	}

	public void setConnectionProperties(ConnectionProperties props) {
		this.connectionprops = props;
	}

	public File getDirectory() {
		return connectiondir;
	}

	public ProducerController getProducer(String producername) {
		return producers.get(producername);
	}

	public ConsumerController getConsumer(String consumername) {
		return consumers.get(consumername);
	}

	public ConnectorController getConnectorController() {
		return connectorcontroller;
	}
	
	public int getProducerCount() {
		int count = 0;
		if (producers != null) {
			count += producers.size();
		}
		return count;
	}

	public int getConsumerCount() {
		int count = 0;
		if (consumers != null) {
			count += consumers.size();
		}
		return count;
	}

	public long getRowsProcessed() {
		long count = 0;
		if (producers != null) {
			for (ProducerController c : producers.values()) {
				count += c.getRowsProcessedCount();
			}
		}
		if (consumers != null) {
			for (ConsumerController c : consumers.values()) {
				count += c.getRowsProcessedCount();
			}
		}
		return count;
	}

	public Long getLastProcessed() {
		Long last = null;
		if (producers != null) {
			for (ProducerController c : producers.values()) {
				Long l = c.getLastProcessed();
				if (l != null) {
					if (last == null || last < l) {
						last = l;
					}
				}
			}
		}
		if (consumers != null) {
			for (ConsumerController c : consumers.values()) {
				Long l = c.getLastProcessed();
				if (l != null) {
					if (last == null || last < l) {
						last = l;
					}
				}
			}
		}
		return last;
	}

	@Override
	protected void updateLandscape() {
		if (producers != null) {
			for (ProducerController c : producers.values()) {
				c.updateLandscape();
			}
		}
		if (consumers != null) {
			for (ConsumerController c : consumers.values()) {
				c.updateLandscape();
			}
		}
	}

	@Override
	protected void updateSchemaCache() {
		if (producers != null) {
			for (ProducerController c : producers.values()) {
				c.updateSchemaCache();
			}
		}
		if (consumers != null) {
			for (ConsumerController c : consumers.values()) {
				c.updateSchemaCache();
			}
		}
	}
	
	public BrowsingService<?> getBrowser() throws IOException {
		if (browser == null) {
			browser = connectorcontroller.getConnectorFactory().createBrowsingService(this);
		}
		return browser;
	}

	public void validate() throws IOException {
		BrowsingService<?> b = connectorcontroller.getConnectorFactory().createBrowsingService(this);
		b.validate();
	}
}
