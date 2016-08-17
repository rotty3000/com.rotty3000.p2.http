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

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.osgi.framework.ServiceObjects;

class WrappedEndpoint extends Endpoint {

	public WrappedEndpoint(ServiceObjects<Endpoint> serviceObjects) {
		this.serviceObjects = serviceObjects;
		this.endpoint = serviceObjects.getService();
	}

	void close() {
		closed = true;
		Iterator<Session> iterator = sessions.iterator();
		while (iterator.hasNext()) {
			Session session = iterator.next();
			iterator.remove();
			try {
				CloseReason closeReason = new CloseReason(
					CloseReason.CloseCodes.GOING_AWAY, "Service has gone away");

				session.close(closeReason);
				endpoint.onClose(session, closeReason);
				serviceObjects.ungetService(endpoint);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		if (closed) {
			return;
		}
		endpoint.onClose(session, closeReason);
		sessions.remove(session);
		serviceObjects.ungetService(endpoint);
	}

	@Override
	public void onError(Session session, Throwable throwable) {
		if (closed) {
			return;
		}
		endpoint.onError(session, throwable);
	}

	@Override
	public void onOpen(Session session, EndpointConfig endpointConfig) {
		if (closed) {
			return;
		}
		endpoint.onOpen(session, endpointConfig);
		sessions.add(session);
	}

	private volatile boolean closed = false;
	private final Endpoint endpoint;
	private final ServiceObjects<Endpoint> serviceObjects;
	private final Set<Session> sessions = new HashSet<>();

}