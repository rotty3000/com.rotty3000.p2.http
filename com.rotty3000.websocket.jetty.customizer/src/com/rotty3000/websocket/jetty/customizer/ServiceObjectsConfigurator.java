package com.rotty3000.websocket.jetty.customizer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.osgi.framework.ServiceObjects;

public class ServiceObjectsConfigurator
	extends ServerEndpointConfig.Configurator {

	public ServiceObjectsConfigurator(ServiceObjects<Endpoint> serviceObjects) {
		this.serviceObjects = serviceObjects;
	}

	public void close() {
		Iterator<WrappedEndpoint> iterator = endpoints.iterator();

		while (iterator.hasNext()) {
			WrappedEndpoint wrappedEndpoint = iterator.next();
			iterator.remove();
			wrappedEndpoint.close();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getEndpointInstance(Class<T> endpointClass) {
		return (T)wrapped();
	}

	private WrappedEndpoint wrapped() {
		WrappedEndpoint wrappedEndpoint = new WrappedEndpoint(serviceObjects);

		endpoints.add(wrappedEndpoint);

		return wrappedEndpoint;
	}

	private final ServiceObjects<Endpoint> serviceObjects;
	private final Set<WrappedEndpoint> endpoints = new HashSet<>();

}