package io.rtdi.bigdata.connector.connectorframework.rest;

import java.io.File;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

import io.rtdi.bigdata.connector.connectorframework.exceptions.ConnectorCallerException;
import io.rtdi.bigdata.connector.connectorframework.servlet.ServletSecurityConstants;

@Path("/")
public class LoggingService {

	@Context
    private Configuration configuration;

	@Context 
	private ServletContext servletContext;

	@GET
	@Path("/logginglevel")
    @Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(ServletSecurityConstants.ROLE_CONFIG)
	public Response getLogLevel() {
		try {
			Level level = LogManager.getRootLogger().getLevel();
			return Response.ok(new LoggingLevel(level)).build();
		} catch (Exception e) {
			return JAXBErrorResponseBuilder.getJAXBResponse(e);
		}
	}

	@POST
	@Path("/logginglevel")
    @Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(ServletSecurityConstants.ROLE_CONFIG)
	public Response setLogLevel(LoggingLevel levelobj) {
		try {
			Level level = levelobj.value();
			Configurator.setAllLevels(LogManager.getRootLogger().getName(), level);
			return Response.ok(level.name()).build();
		} catch (Exception e) {
			return JAXBErrorResponseBuilder.getJAXBResponse(e);
		}
	}

	@GET
	@Path("/logginglevel/{levelname}")
    @Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(ServletSecurityConstants.ROLE_CONFIG)
	public Response setLogLevel2(@PathParam("levelname") String levelname) {
		try {
			try {
				Level level = Level.valueOf(levelname);
				Configurator.setAllLevels(LogManager.getRootLogger().getName(), level);
				return Response.ok(level.name()).build();
				
			} catch (IllegalArgumentException e) {
				throw new ConnectorCallerException("Cannot set the logging level", e, "Use allowed levelnames: \"" + Level.values() + "\"", null);
			}
		} catch (Exception e) {
			return JAXBErrorResponseBuilder.getJAXBResponse(e);
		}
	}

	@GET
	@Path("/debuginfo")
    @Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(ServletSecurityConstants.ROLE_CONFIG)
	public Response getDebugInfo() {
		try {
			java.nio.file.Path logpath = java.nio.file.Path.of(servletContext.getRealPath("WEB-INF"), "..", "..", "..", "logs");
			Level level = LogManager.getRootLogger().getLevel();
			return Response.ok(new DebugInfo(level, logpath.toFile())).build();
		} catch (Exception e) {
			return JAXBErrorResponseBuilder.getJAXBResponse(e);
		}
	}

	public static class DebugInfo extends LoggingLevel {
		private String webserverlog;
		private String applog;
		private static LoggingLevel[] levels = { 
				new LoggingLevel(Level.OFF), 
				new LoggingLevel(Level.FATAL),
				new LoggingLevel(Level.ERROR),
				new LoggingLevel(Level.WARN),
				new LoggingLevel(Level.INFO),
				new LoggingLevel(Level.DEBUG),
				new LoggingLevel(Level.TRACE),
				new LoggingLevel(Level.ALL)};
		
		public DebugInfo(Level level, File logdir) {
			super(level);
			File[] logfiles = logdir.listFiles();
		}
		
		public String getApplog() {
			return applog;
		}
		public String getWebserverlog() {
			return webserverlog;
		}
		
		public LoggingLevel[] getLogginglevels() {
			return levels;
		}

	}

	public static class LoggingLevel {
		private Level logginglevel;

		public LoggingLevel() {
			super();
		}

		public Level value() {
			return logginglevel;
		}

		public LoggingLevel(Level level) {
			super();
			this.logginglevel = level;
		}

		public String getLogginglevel() {
			return logginglevel.name();
		}

		public void setLogginglevel(String logginglevel) throws ConnectorCallerException {
			try {
				this.logginglevel = Level.valueOf(logginglevel);
			} catch (IllegalArgumentException e) {
				throw new ConnectorCallerException("Cannot set the logging level", e, "Use allowed levelnames: \"" + Level.values() + "\"", null);
			}
		}
		
	}
}