package com.rotty3000.websocket.jetty.customizer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.websocket.CloseReason;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;

import org.osgi.framework.ServiceReference;

public class ServerEndpointConfigWrapper implements ServerEndpointConfig {

	public ServerEndpointConfigWrapper(String path) {
		delegate = ServerEndpointConfig.Builder.create(
			Endpoint.class, path).build();
	}

	public Configurator getConfigurator() {
		Entry<ServiceReference<Endpoint>, Configurator> entry =
			endpoints.firstEntry();

		if (entry == null) {
			return NULL;
		}

		return entry.getValue();
	}

	public List<Class<? extends Decoder>> getDecoders() {
		return delegate.getDecoders();
	}

	public Class<?> getEndpointClass() {
		return delegate.getEndpointClass();
	}

	public List<Class<? extends Encoder>> getEncoders() {
		return delegate.getEncoders();
	}

	public List<Extension> getExtensions() {
		return delegate.getExtensions();
	}

	public String getPath() {
		return delegate.getPath();
	}

	public Map<String, Object> getUserProperties() {
		return delegate.getUserProperties();
	}

	public List<String> getSubprotocols() {
		return delegate.getSubprotocols();
	}

	public void removeConfigurator(ServiceReference<Endpoint> reference) {
		endpoints.remove(reference);
	}

	public void setConfigurator(
		ServiceReference<Endpoint> reference, Configurator configurator) {

		endpoints.put(reference, configurator);
	}

	private final Configurator NULL = new ServerEndpointConfig.Configurator() {

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getEndpointInstance(
			Class<T> endpointClass) {

			return (T)new NullEndpoint();
		}

	};

	private ServerEndpointConfig delegate;
	private ConcurrentSkipListMap<ServiceReference<Endpoint>, Configurator>
		endpoints = new ConcurrentSkipListMap<>();

	final class NullEndpoint extends Endpoint {

		@Override
		public void onOpen(Session session, EndpointConfig config) {
			try {
				session.close(
					new CloseReason(
						CloseReason.CloseCodes.GOING_AWAY,
						"Service has gone away"));
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}