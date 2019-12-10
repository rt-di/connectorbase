package io.rtdi.bigdata.connector.connectorframework.exceptions;

public class ConnectorTemporaryException extends ConnectorRuntimeException {

	private static final long serialVersionUID = 8184972522002210263L;

	public ConnectorTemporaryException(String message, Throwable cause, String hint, String causingobject) {
		super(message, cause, hint, causingobject);
	}

}
