package com.rotty3000.websocket.demo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

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

	enum Commands {
		START, STOP
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		final Basic remote = session.getBasicRemote();

		session.addMessageHandler(
			new MessageHandler.Whole<String>() {

				public void onMessage(String text) {
					if (Commands.START.toString().equals(text)) {
						logReader = new LogReader(remote);

						try {
							remote.sendText("Here come the logs...");

							logReader.start();
						}
						catch (Exception e) {
							try {
								remote.sendText("[ERROR] " + e.getMessage());
							}
							catch (IOException ioe) {
								ioe.printStackTrace();
							}
						}
					}
					else if (Commands.STOP.toString().equals(text) &&
							 (logReader != null)) {

						logReader.stop();

						logReader = null;
					}
				}

			}
		);
	}

	private LogReader logReader;

	public class LogReader {

		public LogReader(Basic remote) {
			thread = new Thread(() -> {
				try (FileInputStream in = new FileInputStream("/var/log/syslog");) {
					FileChannel fc = in.getChannel();
					ByteBuffer bb = ByteBuffer.allocate(1024);
					StringBuilder lineCurrent = new StringBuilder();

					LinkedList<String> fifo = new LinkedList<String>();

					int result;
					boolean reachedFirstEOF = false;

					while(!stop && (result = fc.read(bb)) >= -1) {
						if (result == -1) {
							if (!reachedFirstEOF) {
								for (String line : fifo) {
									remote.sendText(line);
								}
							}
							reachedFirstEOF = true;
							Thread.sleep(200);
							continue;
						}
					    bb.flip();
					    while(bb.hasRemaining()) {
					    	char c = (char)bb.get();
					    	if ((c != '\n') && (c != '\r')) {
					    		lineCurrent.append(c);
					    	}
					    	else {
					    		if (!reachedFirstEOF) {
					    			fifo.add(lineCurrent.toString());
					    			if (fifo.size() == 10) {
					    				fifo.removeFirst();
					    			}
					    		}
					    		else {
					    			remote.sendText(lineCurrent.toString());
					    		}
					    		lineCurrent = new StringBuilder();
					    	}
					    }
					    bb.clear();
					}
				}
				catch (Exception e) {

				}
			});
		}

		public void start() throws Exception {
			thread.start();
		}

		public void stop() {
			stop = true;
		}

		private final Thread thread;
		private boolean stop = false;

	}

}