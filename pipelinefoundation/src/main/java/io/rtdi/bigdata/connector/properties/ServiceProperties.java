package io.rtdi.bigdata.connector.properties;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import io.rtdi.bigdata.connector.pipeline.foundation.MicroServiceTransformation;
import io.rtdi.bigdata.connector.pipeline.foundation.entity.ServiceConfigEntity;
import io.rtdi.bigdata.connector.pipeline.foundation.entity.ServiceConfigEntity.ServiceStep;
import io.rtdi.bigdata.connector.pipeline.foundation.exceptions.PropertiesException;
import io.rtdi.bigdata.connector.pipeline.foundation.utils.IOUtils;
import io.rtdi.bigdata.connector.properties.atomic.IProperty;
import io.rtdi.bigdata.connector.properties.atomic.PropertyGroup;
import io.rtdi.bigdata.connector.properties.atomic.PropertyRoot;

/**
 * The ServiceProperties is the class holding all service specific settings.
 *
 */
public abstract class ServiceProperties {

	private static final String SOURCE = "service.source";
	private static final String TARGET = "service.target";
	protected PropertyRoot properties;
	private Set<MicroServiceTransformation> schematransformations = new TreeSet<>();
	private boolean isvalid = false;

	public ServiceProperties(String name) throws PropertiesException {
		super();
		properties = new PropertyRoot(name);
		properties.addTopicSelector(SOURCE, "Source topic name", "Topic name the service consumes", null, null, true);
		properties.addTopicSelector(TARGET, "Target topic name", "Topic name the service writes the results into", null, null, true);
	}
	
	public ServiceProperties(File dir, String name) throws IOException {
		this(name);
		read(dir);
	}

	public void addMicroService(MicroServiceTransformation service) {
		schematransformations.add(service);
	}

	/**
	 * @return The name of the Producer
	 */
	public String getName() {
		return properties.getName();
	}
	 
	/**
	 * @return The list of all properties
	 * @see PropertyGroup#getValues()
	 */
	public List<IProperty> getValue() {
		return properties.getValues();
	}
	
	/**
	 * Copy all values of the provided PropertyGroup into this object's values.
	 * 
	 * @param pg PropertyRoot to take the values from
	 * @throws PropertiesException if one of the data types does not match
	 * 
	 * @see PropertyRoot#parseValue(PropertyRoot, boolean)
	 */
	public void setValue(PropertyRoot pg) throws PropertiesException {
		properties.parseValue(pg, false);
	}

	/**
	 * @return The backing PropertyGroup with all the values
	 */
	public PropertyRoot getPropertyGroup() {
		return properties;
	}

	public PropertyRoot getPropertyGroupNoPasswords() throws PropertiesException {
		PropertyRoot clone = new PropertyRoot(properties.getName());
		clone.parseValue(properties, true);
		return clone;
	}

	@Override
	public String toString() {
		if (properties != null) {
			return properties.toString();
		} else {
			return "NULL";
		}
	}

	/**
	 * Read the individual properties from a directory. The file name is derived from the {@link #getName()}.
	 *  
	 * @param directory where the file can be found
	 * @throws IOException in case of errors
	 */
	public void read(File directory) throws IOException {
		properties.read(directory);
		for (File stepdir : directory.listFiles()) {
			if (stepdir.isDirectory()) {
				MicroServiceTransformation transformation = readMicroservice(stepdir);
				addMicroService(transformation);
				isvalid = true;
			}
		}
	}
	
	protected abstract MicroServiceTransformation readMicroservice(File dir) throws IOException;

	/**
	 * Write the current properties into a directory. The file name is derived from the {@link #getName()}.
	 *  
	 * @param directory where to write the file to
	 * @param data The requested directory structure
	 * @throws IOException In case the file tree cannot be built
	 */
	public void write(File directory, ServiceConfigEntity data) throws IOException {
		properties.write(directory);
		if (data != null) {
			Set<String> existingsteps = new HashSet<>();
			for (File stepdir : directory.listFiles()) {
				if (stepdir.isDirectory()) {
					existingsteps.add(stepdir.getName());
				}
			}
			// data provides a list of elements that might have been added or removed
			for (ServiceStep s : data.getSteps()) {
				File stepdir = new File(directory, s.getStepname());
				writeMicroservice(stepdir);
				if (existingsteps.contains(s.getStepname())) {
					// Step dir exists and is contained in the data 
					existingsteps.remove(s.getStepname());
				} else {
					// Step dir does not exist so must be a new one
					stepdir.mkdir();
				}
			}
			for (String delete : existingsteps) {
				File stepdir = new File(directory, delete);
				IOUtils.deleteDirectory(stepdir);
			}
		}
	}
	
	protected abstract void writeMicroservice(File stepdir) throws IOException;

	public String getSourceTopic() {
		return properties.getStringPropertyValue(SOURCE);
	}

	public String getTargetTopic() {
		return properties.getStringPropertyValue(TARGET);
	}

	public void setSourceTopic(String value) throws PropertiesException {
		properties.setProperty(SOURCE, value);
	}

	public void setTargetTopic(String value) throws PropertiesException {
		properties.setProperty(TARGET, value);
	}

	public boolean isValid() {
		return isvalid;
	}

	public Set<MicroServiceTransformation> getMicroserviceTransformations() {
		return schematransformations;
	}
}
