import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

	
	private ArrayList<ConnectionHandler> connections;
	private ServerSocket server ;
	private boolean done;
	private ExecutorService pool;
	
	public Server() {
		System.out.println("server created");
		try {
			server = new ServerSocket(50001);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		connections = new ArrayList<ConnectionHandler>();
		done = false;
	}
	
	@Override
	public void run() {
		try {
			
			pool = Executors.newCachedThreadPool();
			while(!done) {
				Socket client = server.accept();
				ConnectionHandler handler = new ConnectionHandler(client);
				connections.add(handler);
				pool.execute(handler);
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
			shutdown();
		}
		
	}

	
	public void broadcast(String message) {
		for (ConnectionHandler ch : connections) {
			if(ch != null)
				ch.sendMessage(message);
		}
	}
	
	public void shutdown() {
		try {
			done = true;
			pool.shutdown();
			if (!server.isClosed()) {
				System.out.println("Server is closing");
				server.close();
			}
			for(ConnectionHandler ch : connections) {
				ch.shutdown();
			}
		}
		catch(IOException e){
			// ignore
		}
	}
	
	class ConnectionHandler implements Runnable{
		
		private Socket client;
		private BufferedReader in;
		private PrintWriter out;
		private String nickname;
		
		public ConnectionHandler(Socket client) {
			this.client = client;
			try {
	            out = new PrintWriter(client.getOutputStream(), true);
	        } catch (IOException e) {
	            //ignore
	        }

		}
		
		@Override
		public void run() {
			try {
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				//out = new PrintWriter(client.getOutputStream(), true);
				out.println("Please enter a nickname: ");
				nickname = in.readLine();
				System.out.println(nickname + " Connected");
				broadcast(nickname + " joined the chat!");
				String message;
				while((message=in.readLine())!=null) {
					if(message.startsWith("/nick ")) {
						String[] messageSplit = message.split(" ",2);
						if(messageSplit.length == 2) {
							broadcast(nickname + "renamed themselves to " + messageSplit[1]);
							System.out.println(nickname + " renamed themselves to " + messageSplit[1]);
							nickname = messageSplit[1];
							out.println("Successfully changed nickname to " + nickname);
						}
						else {
							out.println("No nickname provided!");
						}
					}
					else if(message.startsWith("/quit")) {
						broadcast(nickname + " left the chat");
						shutdown();
					}
					else {
						broadcast(nickname + ": " + message);
					}
				}
			}
			catch(IOException e) {
				e.printStackTrace();
				shutdown();
			}
		}
		
		public void sendMessage(String message) {
			out.println(message);
		}
		
		public void shutdown() {
			try {
				in.close();
				out.close();
				if(!client.isClosed()) {
					client.close();
				}
			}
			catch(IOException e){
				//ignore
			}
		}
	}
	
	public static void main(String[] args) {
	    Server server = new Server();
	    Thread serverThread = new Thread(server);
	    serverThread.start();
	    
	    System.out.println("server running successfully");
	    //server.shutdown();
	    //System.out.println("server closed");
	}

}
