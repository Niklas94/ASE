package socket;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import socket.SocketInMessage.SocketMessageType;

public class SocketController implements ISocketController {
	Set<ISocketObserver> observers = new HashSet<ISocketObserver>();
	private BufferedReader inStream;
	private DataOutputStream outStream;
	Scanner keyb = new Scanner(System.in);

	@Override
	public void registerObserver(ISocketObserver observer) {
		observers.add(observer);
	}

	@Override
	public void unRegisterObserver(ISocketObserver observer) {
		observers.remove(observer);
	}

	@Override
	public void sendMessage(SocketOutMessage message) {
		if (outStream!=null){
			try {
				outStream.writeBytes(message.getMessage() + "\n\r"); //Vi har tidligere brugt writeChars
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		try (ServerSocket listeningSocket = new ServerSocket(Port)){ 
			while (true){
				waitForConnections(listeningSocket); 
			}
		} catch (IOException e1) {
			notifyObservers(new SocketInMessage(SocketMessageType.E, "Couldnt connect socket... recommending to restart program..."));
			e1.printStackTrace();
		}	
	}

	private void waitForConnections(ServerSocket listeningSocket) {
		try {
			Socket activeSocket = listeningSocket.accept(); //Blocking call
			inStream = new BufferedReader(new InputStreamReader(activeSocket.getInputStream()));
			outStream = new DataOutputStream(activeSocket.getOutputStream());
			String inLine;
			//.readLine is a blocking call 

			while (true){
				inLine = inStream.readLine();
				//System.out.println(inLine);
				if (inLine != null && inLine.length() < 1) break;
				if (inLine==null) break;
				switch (inLine.split(" ")[0]) {
				case "RM208": // Display a message in the secondary display and wait for response
					//						notifyObservers(new SocketInMessage(SocketMessageType.RM208, inLine.split("RM208 ") [1]));
					notifyObservers(new SocketInMessage(SocketMessageType.RM208, inLine));
					//outStream.writeChars("Invalid input. Try again.");
					break;
				case "D":// Display a message in the primary display
					notifyObservers(new SocketInMessage(SocketMessageType.D, inLine.split("D ")[1])); 			
					break;
				case "DW": //Clear primary display
					String regex = "([\\W])*\\w";
					notifyObservers(new SocketInMessage(SocketMessageType.DW, inLine.replaceAll(regex, "")));
					break;
				case "P111": //Show something in secondary display
					notifyObservers(new SocketInMessage(SocketMessageType.P111, inLine.split("P111 ")[1]));
					break;
				case "T": // Tare the weight
					notifyObservers(new SocketInMessage(SocketMessageType.T, "Taring weight..."));
					break;
				case "S": // Request the current load
					notifyObservers(new SocketInMessage(SocketMessageType.S, "Sending data..."));
					break;
				case "K":
					if (inLine.split(" ").length>1){
						notifyObservers(new SocketInMessage(SocketMessageType.K, inLine.split(" ")[1]));
					}
					break;
				case "B": // Set the load
					notifyObservers(new SocketInMessage(SocketMessageType.B, inLine)); //inLine.split(" ")[1]
					break;
				case "Q": // Quit
					notifyObservers(new SocketInMessage(SocketMessageType.Q, "Closing..."));
					break;
				default: 
					notifyObservers(new SocketInMessage(SocketMessageType.E, "Wrong command..."));
					break;
				}
			}
		} catch (IOException e) {
			notifyObservers(new SocketInMessage(SocketMessageType.E, "Error when waiting for connection..."));
			e.printStackTrace();
		}
	}

	private void notifyObservers(SocketInMessage message) {
		for (ISocketObserver socketObserver : observers) {
			socketObserver.notify(message);
		}
	}

	public static int getPort() {
		return Port;
	}

}

