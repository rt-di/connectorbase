package io.rtdi.bigdata.connector.connectorframework.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.rtdi.bigdata.connector.connectorframework.IConnectorFactoryService;
import io.rtdi.bigdata.connector.connectorframework.Service;
import io.rtdi.bigdata.connector.connectorframework.exceptions.ConnectorCallerException;
import io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI;
import io.rtdi.bigdata.connector.pipeline.foundation.MicroServiceTransformation;
import io.rtdi.bigdata.connector.pipeline.foundation.enums.ControllerExitType;
import io.rtdi.bigdata.connector.pipeline.foundation.exceptions.PropertiesException;
import io.rtdi.bigdata.connector.properties.ServiceProperties;

public class ServiceController extends Controller<Controller<?>> {

	private ServiceProperties serviceprops;
	private ConnectorController connector;
	private File servicedir = null;
	private Service service = null;

	public ServiceController(ServiceProperties serviceprops, ConnectorController connector) throws PropertiesException {
		super(serviceprops.getName());
		this.serviceprops = serviceprops;
		this.connector = connector;
	}

	public ServiceController(File servicedir, ConnectorController connector) {
		super(servicedir.getName());
		this.servicedir  = servicedir;
		this.connector = connector;
	}

	@Override
	protected void stopControllerImpl(ControllerExitType exittype) {
		if (service != null) {
			service.stop();
		}
	}

	@Override
	protected void startControllerImpl() throws IOException {
		if (serviceprops.isValid()) {
			service = getConnectorFactory().createService(this);
			service.start();
			updateLandscape();
		} else {
			logger.info("Service not started as the properties are incomplete");
			this.disableController();
			throw new PropertiesException("Service is not configured completely");
		}
	}
	
	private IConnectorFactoryService getConnectorFactory() {
		return (IConnectorFactoryService) connector.getConnectorFactory();
	}

	public ServiceProperties getServiceProperties() {
		return serviceprops;
	}

	@Override
	protected String getControllerType() {
		return "ServiceController";
	}

	public ConnectorController getConnectorController() {
		return connector;
	}

	public long getRowsProcessed() {
		if (service != null) {
			return service.getRowsProcessed();
		} else {
			return 0;
		}
	}
	
	public IPipelineAPI<?, ?, ?, ?> getPipelineAPI() {
		return connector.getPipelineAPI();
	}
	
	public void readConfigs() throws PropertiesException {
		if (servicedir == null) {
			throw new PropertiesException("servicedirectory is not set");
		} else if (!servicedir.exists()) {
			throw new PropertiesException("servicedirectory \"" + servicedir.getAbsolutePath() + "\" does not exist");
		} else if (!servicedir.isDirectory()) {
			throw new PropertiesException("servicedirectory \"" + servicedir.getAbsolutePath() + "\"is no directory");
		}
		
		logger.info("reading configs for service \"" + getName() + "\"");
		
		IConnectorFactoryService connectorfactory = getConnectorFactory();
		serviceprops = connectorfactory.createServiceProperties(servicedir.getName());
		serviceprops.read(servicedir);
	}

	public void setServiceProperties(ServiceProperties props) {
		this.serviceprops = props;
	}

	public File getDirectory() {
		return servicedir;
	}

	public Long getLastProcessed() {
		if (service != null) {
			return service.getLastProcessed();
		} else {
			return null;
		}
	}

	public Map<String, List<? extends MicroServiceTransformation>> getSchemaTransformations() {
		return serviceprops.getSchemaTransformations();
	}

	public MicroServiceTransformation getMicroserviceOrFail(String schemaname, String microservicename) throws ConnectorCallerException {
		MicroServiceTransformation m = getMicroservice(schemaname, microservicename);
		if (m == null) {
			throw new ConnectorCallerException("No Microservice with this name configured", null, "getMicroservice() was called for a non-existing name", microservicename);
		} else {
			return m;
		}
	}

	public MicroServiceTransformation getMicroservice(String schemaname, String microservicename) throws ConnectorCallerException {
		if (serviceprops != null && serviceprops.getSchemaTransformations() != null) {
			List<? extends MicroServiceTransformation> l = serviceprops.getMicroServices(schemaname);
			if (l != null) {
				for ( MicroServiceTransformation m : l) {
					if (m.getName().equals(microservicename)) {
						return m;
					}
				}
			}
		}
		return null;
	}

	@Override
	protected void updateLandscape() {
		if (service != null) {
			service.updateLandscape();
		}
	}

	@Override
	protected void updateSchemaCache() {
		if (service != null) {
			service.updateSchemaCache();
		}
	}

	public Set<String> getSchemas() {
		Map<String, List<? extends MicroServiceTransformation>> m = serviceprops.getSchemaTransformations();
		if (m != null) {
			return m.keySet();
		} else {
			return null;
		}
	}

}
