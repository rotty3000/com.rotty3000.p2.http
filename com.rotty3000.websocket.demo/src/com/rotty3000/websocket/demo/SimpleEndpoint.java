package com.rotty3000.websocket.demo;

import java.io.IOException;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(
	property = {
		"org.osgi.http.websocket.endpoint.path=/simple"
	},
	scope = ServiceScope.PROTOTYPE,
	service = Endpoint.class
)
public class SimpleEndpoint extends Endpoint {

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		final Basic remote = session.getBasicRemote();

		session.addMessageHandler(
			new MessageHandler.Whole<String>() {

				public void onMessage(String text) {
					try {
						remote.sendText("Got your message (" + text + "). Thanks !");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}
		);
	}

}