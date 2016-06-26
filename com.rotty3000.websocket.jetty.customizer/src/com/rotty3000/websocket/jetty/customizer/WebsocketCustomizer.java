package com.rotty3000.websocket.jetty.customizer;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import org.eclipse.equinox.http.jetty.JettyCustomizer;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.JsrSession;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class WebsocketCustomizer extends JettyCustomizer
	implements ServiceTrackerCustomizer<Endpoint, ServerEndpointConfigWrapper> {

	public WebsocketCustomizer() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		bundleContext = bundle.getBundleContext();

		Filter filter;

		try {
			filter = bundleContext.createFilter(
				"(&(objectClass=" + Endpoint.class.getName() + ")(" +
					Constants.SERVICE_SCOPE + "=" + Constants.SCOPE_PROTOTYPE +
						"))");
		}
		catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}

		serviceTracker = new ServiceTracker<>(bundleContext, filter, this);
	}

	@Override
	public Object customizeContext(
		Object context, Dictionary<String, ?> settings) {

		try {
			configureContext =
				WebSocketServerContainerInitializer.configureContext(
					(ServletContextHandler)context);
		}
		catch (ServletException se) {
			throw new RuntimeException(se);
		}

		serviceTracker.open();

		return context;
	}

	@Override
	public ServerEndpointConfigWrapper addingService(
		ServiceReference<Endpoint> reference) {

		String path = (String)reference.getProperty(
			"org.osgi.http.websocket.endpoint.path");

		final ServiceObjects<Endpoint> serviceObjects =
			bundleContext.getServiceObjects(reference);

		ServerEndpointConfigWrapper serverEndpointConfig = keyedWrappers.get(
			path);

		boolean isNew = false;

		if (serverEndpointConfig == null) {
			serverEndpointConfig = new ServerEndpointConfigWrapper(path);

			isNew = true;
		}

		serverEndpointConfig.setConfigurator(
			reference, new ServiceObjectsConfigurator(serviceObjects));

		if (isNew) {
			try {
				configureContext.addEndpoint(serverEndpointConfig);
			}
			catch (Exception e) {
				e.printStackTrace();

				return null;
			}

			keyedWrappers.put(path, serverEndpointConfig);
		}

		return serverEndpointConfig;
	}

	@Override
	public void modifiedService(
		ServiceReference<Endpoint> reference,
		ServerEndpointConfigWrapper serverEndpointConfig) {

		// do nothing
	}

	@Override
	public void removedService(
		ServiceReference<Endpoint> reference,
		ServerEndpointConfigWrapper serverEndpointConfig) {

		ServiceObjectsConfigurator configurator =
			serverEndpointConfig.removeConfigurator(reference);

		configurator.close();
	}

	private final BundleContext bundleContext;
	private ServerContainer configureContext;
	private Map<String, ServerEndpointConfigWrapper> keyedWrappers =
		new ConcurrentHashMap<>();
	private ServiceTracker<Endpoint, ServerEndpointConfigWrapper> serviceTracker;

}