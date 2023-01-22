package http_proxy_server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.BasicConfigurator;



public class ProxyServer {

	//cache is a Map: the key is the URL and the value is the file name of the file that stores the cached content
	Map<String, String> cache;
	ServerSocket proxySocket;
	String logFileName = "log.txt";

	public static void main(String[] args) {
		int port = Integer.parseInt(args[0]);
		BasicConfigurator.configure();
		System.out.println("Starting server at localhost:" + port);
		new ProxyServer().startServer(port);
	}

	void startServer(int proxyPort) {
		cache = new ConcurrentHashMap<>();
		// create the directory to store cached files. 
		File cacheDir = new File("cached");
		if (!cacheDir.exists() || (cacheDir.exists() && !cacheDir.isDirectory())) {
			cacheDir.mkdirs();
		}

		/**
			 * TODO:
			 * create a serverSocket to listen on the port (proxyPort)
			 * Create a thread (RequestHandler) for each new client connection 
			 * remember to catch Exceptions!
			 *
		*/
		
		/** TODO: Multithreading for handling multiple connections
		 * while (true) {
		 *      accept a connection;
		 *	    create a thread to deal with the client;
		 *	}
		 */
		try (ServerSocket serverSocket = new ServerSocket(proxyPort)) {
			while (true) {
				try (Socket socket = serverSocket.accept()) {
					RequestHandler handler = new RequestHandler(socket, this);
					handler.start();
					handler.join();
				}	
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public String getCache(String hashcode) {
		return cache.get(hashcode);
	}

	public void putCache(String hashcode, String fileName) {
		cache.put(hashcode, fileName);
	}

	public synchronized void writeLog(String info) {
		
			/**
			 * To do
			 * write string (info) to the log file, and add the current time stamp 
			 * e.g. String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
			 *
			*/
	}

}
