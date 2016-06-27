package com.rotty3000.websocket.demo;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.Session;

import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(
	property = {
		"org.osgi.http.websocket.endpoint.path=/logger"
	},
	scope = ServiceScope.PROTOTYPE,
	service = Endpoint.class
)
public class LoggerEndpoint extends Endpoint {

	enum Commands {
		START
	}

	public static void log(Async remote, String message) {
		remote.sendText(message);
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		Async remote = session.getAsyncRemote();

		session.addMessageHandler(
			new MessageHandler.Whole<String>() {

				public void onMessage(String text) {
					try {
						JSONObject jsonObject = new JSONObject(text);

						if (!jsonObject.has("file")) {
							log(remote, "[ERROR] No log file specified!");

							return;
						}

						String fileName = jsonObject.getString("file");

						Path filePath = Paths.get(fileName);
						File file = filePath.toFile();

						if (!file.exists() || !file.canRead()) {
							log(
								remote,
								"[ERROR] The file [" + fileName +
									"] cannot be found!");

							return;
						}

						int lines = 0;
						if (jsonObject.has("lines")) {
							lines = jsonObject.getInt("lines");
						}

						log(
							remote,
							"[CON] Here come the logs for [" + fileName +
								"] with [" + lines + "] of history.");

						logReader = new LogReader(remote, file, lines);

						logReader.start();
					}
					catch (Exception e) {
						log(remote, "[ERROR] " + e.getMessage());
					}
				}

			}
		);
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		if (logReader != null) {
			logReader.stop();

			logReader = null;
		}
	}

	private LogReader logReader;

	class LogReader {

		LogReader(Async remote, File file, int lines) {
			this.remote = remote;
			this.file = file;
			this.lines = lines;
			this.string = String.format("%s#%s[%s]", name, this.hashCode(), file);
		}

		void start() throws Exception {
			Runnable task = () -> {
				try (FileInputStream in = new FileInputStream(file)) {
					FileChannel fc = in.getChannel();
					ByteBuffer bb = ByteBuffer.allocate(1024);
					StringBuilder lineCurrent = new StringBuilder();
					LinkedList<String> fifo = new LinkedList<String>();
					int result;
					boolean reachedFirstEOF = false;

					while(!stop) {
						result = fc.read(bb);
						if (result == -1) {
							if (!reachedFirstEOF && (lines > 0)) {
								for (String line : fifo) {
									log(remote, line);
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
					    		if (!reachedFirstEOF && (lines > 0)) {
					    			fifo.add(lineCurrent.toString());
					    			if (fifo.size() > lines) {
					    				fifo.removeFirst();
					    			}
					    		}
					    		else if (lineCurrent.length() != 0) {
					    			log(remote, lineCurrent.toString());
					    		}
					    		lineCurrent = new StringBuilder();
					    	}
					    }
					    bb.clear();
					}
				}
				catch (Exception e) {
					log(remote, "[ERROR] " + e.getMessage());
				}
			};

			thread = new Thread(task);

			thread.setName(this.toString());

			System.out.println(this.toString() + " starting.");

			thread.start();
		}

		void stop() {
			System.out.println(this.toString() + " stoping.");

			stop = true;
		}

		@Override
		public String toString() {
			return string;
		}

		private final File file;
		private final int lines;
		private final Async remote;
		private volatile boolean stop = false;
		private final String string;
		private Thread thread;

	}

	private static final String name = LogReader.class.getSimpleName();

}