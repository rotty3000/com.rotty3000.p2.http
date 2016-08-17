/**
 * Copyright 2016 Raymond Augé
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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