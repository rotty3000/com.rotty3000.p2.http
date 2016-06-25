package com.rotty3000.websocket.demo;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

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
			this.remote = remote;
		}

		public void log(String message) {
			try {
				remote.sendText(message);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void start() throws Exception {
			Runnable task = () -> {
				try (FileInputStream in = new FileInputStream("/var/log/syslog")) {
					FileChannel fc = in.getChannel();
					ByteBuffer bb = ByteBuffer.allocate(1024);
					StringBuilder lineCurrent = new StringBuilder();
					LinkedList<String> fifo = new LinkedList<String>();
					boolean reachedFirstEOF = false;
					int result;

					while(!stop) {
						result = fc.read(bb);
						if (result == -1) {
							if (!reachedFirstEOF) {
								for (String line : fifo) {
									log(line);
								}
								fifo.clear();
							}
							reachedFirstEOF = true;
							Thread.sleep(400);
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
					    			if (fifo.size() > 10) {
					    				fifo.removeFirst();
					    			}
					    		}
					    		else if (lineCurrent.length() != 0) {
					    			log(lineCurrent.toString());
					    		}
					    		lineCurrent = new StringBuilder();
					    	}
					    }
					    bb.clear();
					}
				}
				catch (Exception e) {
					log("[ERROR] " + e.getMessage());
				}
			};

			thread = new Thread(task);

			thread.start();
		}

		public void stop() {
			stop = true;
		}

		private final Basic remote;
		private Thread thread;
		private boolean stop = false;

	}

}