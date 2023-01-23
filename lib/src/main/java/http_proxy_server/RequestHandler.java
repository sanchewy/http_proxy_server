package http_proxy_server;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

// RequestHandler is thread that process requests of one client connection
public class RequestHandler extends Thread {

	Socket clientSocket;
	OutputStream outToClient;
	byte[] request = new byte[1024];
	private ProxyServer server;
	private static Logger logger = Logger.getLogger(RequestHandler.class);
	private static int threadCount = 1; // Used to trigger sleep when demoing multithreading

	public RequestHandler(Socket clientSocket, ProxyServer proxyServer) {
		this.clientSocket = clientSocket;
		this.server = proxyServer;
		try {
			outToClient = clientSocket.getOutputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
//      // Used to trigger sleep when demoing multithreading
//		threadCount++;
//		if (threadCount % 2 == 0) {
//			try {
//				logger.info("Thread even, delaying thread to demo multithreading: " + threadCount);
//				Thread.sleep(20000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		
		/**
		 * To do
		 * Process the requests from a client. In particular, 
		 * (1) Check the request type, only process GET request and ignore others
		 * (2) Write log.
		 * (3) If the url of GET request has been cached, respond with cached content
		 * (4) Otherwise, call method proxyServertoClient to process the GET request
		 *
		*/
		try(BufferedReader socketReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
			String fullInput = "";
			String inputLine;
			while (!(inputLine = socketReader.readLine()).equals("")) { // Curl request end with blank string not null
		    	fullInput += inputLine + " ";
		    }
			String requestNoHeaders = reformatRequest(fullInput);
			server.writeLog(String.format("%s %s", clientSocket.getRemoteSocketAddress(), hostAndResource(requestNoHeaders)));
			logger.debug(String.format("Reformated request '%s'", removeHostLeaveResource(fullInput)));
			if (fullInput.startsWith("GET")) {
				String cachedFileName;
				if ((cachedFileName = server.getCache(requestNoHeaders)) != null) {
					logger.debug("Returning cached response from previous identical response!");
					sendCachedInfoToClient(cachedFileName);
				} else {
					proxyServertoClient(requestNoHeaders);
				}
			} else {
				logger.error("This proxy only supports GET HTTP requests");
			}
		} catch (IOException e) {
			logger.error("Error reading from client request: ", e);
		}
	}

	private void proxyServertoClient(String clientRequest) {

		FileOutputStream fileWriter = null;
		Socket toWebServerSocket = null;
		InputStream inFromServer;
		OutputStream outToServer;
			
		/**
		 * To do
		 * (1) Create a socket to connect to the web server (default port 80)
		 * (2) Send client's request (clientRequest) to the web server, you may want to use flush() after writing.
		 * (3) Use a while loop to read all responses from web server and send back to client
		 * (4) Write the web server's response to a cache file, put the request URL and cache file name to the cache Map
		 * (5) close file, and sockets.
		*/
		
		try {
			// Create external socket connection to requested host
			String host = new URL(extractUrl(clientRequest)).getHost();
			int port = 80;
			logger.debug(String.format("Creating socket with host: '%s' and port: '%d'", host, port));
			toWebServerSocket = new Socket(host, port);
			outToServer = toWebServerSocket.getOutputStream();
			inFromServer = toWebServerSocket.getInputStream();
			PrintWriter serverWriter = new PrintWriter(outToServer, true);
			BufferedReader serverReader = new BufferedReader(new InputStreamReader(inFromServer));
			
			// Format request and send to host server, printf auto-flushes, \r\n for mac newline to work with http
			String methodResourceHttp = removeHostLeaveResource(clientRequest);
			logger.debug(String.format("Sending request to external webserver: '%s'", methodResourceHttp));
			serverWriter.printf("%s\r\n", methodResourceHttp);
			serverWriter.printf("Host: %s\r\n", host);
			serverWriter.printf("\r\n");
			
//			// Sleep waiting for response (only needed during debugging of incoming connection)
//			while(inFromServer.available() == 0) {
//				Thread.sleep(100);
//				logger.debug("Sleeping");
//			}
			
			// Create client writer and cache writer to copy server response into
			PrintWriter clientWriter = new PrintWriter(outToClient, true);
			String cacheFileName = "cached/" + generateRandomFileName() + ".dat";
			BufferedOutputStream cacheBufferOut = null;
			try {
				cacheBufferOut = new BufferedOutputStream(new FileOutputStream(cacheFileName));
			} catch (FileNotFoundException e) {
				logger.error("Error creating cache file: ", e);
			}
			
			// Process server response (copy into client writer and cache writer)
			String inputLine;
			while ((inputLine = serverReader.readLine()) != null) {
		    	clientWriter.printf("%s\r\n", inputLine);
		    	cacheBufferOut.write(String.format("%s\r\n", inputLine).getBytes());
		    }
			logger.debug("Read in server response.");
			
			// Add cache map record to file
			cacheBufferOut.flush();
			cacheBufferOut.close();
			server.putCache(clientRequest, cacheFileName);
			logger.debug("Cached server response in file: " + cacheFileName);
			
			// Return response to client and close all resources.
			clientWriter.flush();
		} catch (IOException e) {
			logger.error("Error processing server response: ", e);
		}
	}
	
	private String reformatRequest(String request) {
		String regex = "^(.*HTTP\\/1\\.1).*$";
		Matcher m = Pattern.compile(regex).matcher(request);
		m.find();
		return m.group(1);
	}
	
	private String extractUrl(String request) {
		String regex = "^GET (http:\\/\\/[^\\/]+)\\/.*$";
		Matcher m = Pattern.compile(regex).matcher(request);
		m.find();
		return m.group(1);
	}
	
	private String removeHostLeaveResource(String request) {
		return request.replaceAll("http:\\/\\/[^\\/]+", "");
	}
	
	private String hostAndResource(String request) {
		String regex = "^GET http:\\/\\/(.*) HTTP\\/1\\.1.*$";
		Matcher m = Pattern.compile(regex).matcher(request);
		m.find();
		return m.group(1);
	}
	
	// Sends the cached content stored in the cache file to the client
	private void sendCachedInfoToClient(String fileName) {
		try {
			byte[] bytes = Files.readAllBytes(Paths.get(fileName));
			outToClient.write(bytes);
			outToClient.flush();
			if (clientSocket != null) {
				clientSocket.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Generates a random file name  
	public String generateRandomFileName() {
		String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
		SecureRandom RANDOM = new SecureRandom();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10; ++i) {
			sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}
}