package http_proxy_server;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


// RequestHandler is thread that process requests of one client connection
public class RequestHandler extends Thread {

	Socket clientSocket;
	InputStream inFromClient;
	OutputStream outToClient;
	byte[] request = new byte[1024];
	private ProxyServer server;
	private static Logger logger = Logger.getLogger(RequestHandler.class);

	public RequestHandler(Socket clientSocket, ProxyServer proxyServer) {
		this.clientSocket = clientSocket;
		this.server = proxyServer;
		try {
			clientSocket.setSoTimeout(2000);
			inFromClient = clientSocket.getInputStream();
			outToClient = clientSocket.getOutputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	@Override
	public void run() {
		/**
		 * To do
		 * Process the requests from a client. In particular, 
		 * (1) Check the request type, only process GET request and ignore others
		 * (2) Write log.
		 * (3) If the url of GET request has been cached, respond with cached content
		 * (4) Otherwise, call method proxyServertoClient to process the GET request
		 *
		*/
		try {
			BufferedReader socketReader = new BufferedReader(new InputStreamReader(inFromClient));
			String fullInput = "";
			String inputLine;
			while (!(inputLine = socketReader.readLine()).equals("")) {
		    	fullInput += inputLine;
		    }
			logger.debug(String.format("Read client request: '%s'", fullInput));
			logger.debug(String.format("Reformated request '%s'", reformatRequest(fullInput)));
		    proxyServertoClient(reformatRequest(fullInput));
		} catch (IOException e) {
			logger.error(e);
		}
	}

	
	private void proxyServertoClient(String clientRequest) {

		FileOutputStream fileWriter = null;
		Socket toWebServerSocket = null;
		InputStream inFromServer;
		OutputStream outToServer;
		
		// Create Buffered output stream to write to cached copy of file
		String fileName = "cached/" + generateRandomFileName() + ".dat";
		BufferedOutputStream bout;
		try {
			fileWriter = new FileOutputStream(fileName);
			bout = new BufferedOutputStream(fileWriter);
		} catch (FileNotFoundException e) {
			logger.error(e);
		}
		
		// to handle binary content, byte is used
		byte[] serverReply = new byte[4096];
			
		/**
		 * To do
		 * (1) Create a socket to connect to the web server (default port 80)
		 * (2) Send client's request (clientRequest) to the web server, you may want to use flush() after writing.
		 * (3) Use a while loop to read all responses from web server and send back to client
		 * (4) Write the web server's response to a cache file, put the request URL and cache file name to the cache Map
		 * (5) close file, and sockets.
		*/
		
		try {
			URL url = new URL(extractUrl(clientRequest));
			int port = 80;
			logger.debug(String.format("Creating socket with host: '%s' and port: '%d'", url.getHost(), port));
			toWebServerSocket = new Socket(url.getHost(), port);
			outToServer = toWebServerSocket.getOutputStream();
			inFromServer = toWebServerSocket.getInputStream();
			PrintWriter out = new PrintWriter(outToServer, true);
			BufferedReader in = new BufferedReader(new InputStreamReader(inFromServer));
			clientRequest = removeHostLeaveResource(clientRequest);
			logger.debug(String.format("Sending request to external webserver: '%s'", clientRequest));
			out.print(clientRequest + "\r\n");
			out.print("Host: " + url.getHost() +  "\r\n");
			out.print("User-Agent: Simple Http Client\r\n");
			out.print("Accept: text/html\r\n");
			out.print("Accept-Language: en-US\r\n");
			out.print("Connection: close\r\n");
			out.print("\r\n");
			out.flush();
			String fullInput = "";
			String inputLine;
			while(inFromServer.available() == 0) {
				Thread.sleep(100);
				logger.debug("Sleeping");
			}
			while (!(inputLine = in.readLine()).equals("")) {
				logger.debug(String.format("ResponseLine: '%s'", inputLine));
		    	fullInput += inputLine;
		    }
			logger.debug(String.format("Read server response: '%s'", fullInput));
		} catch (IOException | InterruptedException e) {
			logger.error(e);
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
	
	// Sends the cached content stored in the cache file to the client
	private void sendCachedInfoToClient(String fileName) {
		try {
			byte[] bytes = Files.readAllBytes(Paths.get(fileName));
			outToClient.write(bytes);
			outToClient.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
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