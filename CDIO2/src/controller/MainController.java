package controller;

import java.util.regex.Pattern;


import socket.ISocketController;
import socket.ISocketObserver;
import socket.SocketInMessage;
import socket.SocketOutMessage;
import weight.IWeightInterfaceController;
import weight.IWeightInterfaceObserver;
import weight.KeyPress;
/**
 * MainController - integrating input from socket and ui. Implements ISocketObserver and IUIObserver to handle this.
 * @author Christian Budtz
 * @version 0.1 2017-01-24
 *
 */
public class MainController implements IMainController, ISocketObserver, IWeightInterfaceObserver {

	private ISocketController socketHandler;
	private IWeightInterfaceController weightController;
	private KeyState keyState = KeyState.K1;
	private Double weight = 0.0, taraWeight = 0.0;
	private String currentDisplay = "";
	private String tester = "";
	boolean counter = false;

	public MainController(ISocketController socketHandler, IWeightInterfaceController weightInterfaceController) {
		this.init(socketHandler, weightInterfaceController);
	}

	@Override
	public void init(ISocketController socketHandler, IWeightInterfaceController weightInterfaceController) {
		this.socketHandler = socketHandler;
		this.weightController=weightInterfaceController;
	}

	@Override
	public void start() {
		if (socketHandler!=null && weightController!=null){
			//Makes this controller interested in messages from the socket
			socketHandler.registerObserver(this);
			//Starts socketHandler in own thread
			new Thread(socketHandler).start();
			weightController.registerObserver(this);
			new Thread(weightController).start();
		} 
		else {
			System.err.println("No controllers injected!");
		}
	}

	//Listening for socket input
	@Override
	public void notify(SocketInMessage message) {
		switch (message.getType()) {
		case B:
			if (keyState.equals(KeyState.K4) || keyState.equals(KeyState.K3)) {
				weightController.showMessagePrimaryDisplay(String.valueOf(-taraWeight));

			}
			else
				System.out.println("ES");
			break;
		case D:
			if (keyState.equals(KeyState.K4) || keyState.equals(KeyState.K3)) {
				weightController.showMessagePrimaryDisplay(message.getMessage());
				weightController.showMessageSecondaryDisplay(message.getMessage());
				if (isDouble(message.getMessage()))
					weight = Double.parseDouble(message.getMessage());
			}
			else
				System.out.println("ES");
			break;
		case Q:
			if (keyState.equals(KeyState.K4) || keyState.equals(KeyState.K3)) {
				System.exit(0);
			}
			else
				System.out.println("ES");
			break;
		case RM204:
			break;
		case RM208:
			if (keyState.equals(KeyState.K4) || keyState.equals(KeyState.K3)) {	
				weightController.showMessageSecondaryDisplay(message.getMessage());
			}
			else 
				System.out.println("ES");
			break;
		case S:
			if (keyState.equals(KeyState.K4) || keyState.equals(KeyState.K3)) {	
				socketHandler.sendMessage(new SocketOutMessage(currentDisplay + "\n\r"));
			}
			else
				System.out.println("ES");
			break;
		case T:
			if (keyState.equals(KeyState.K4) || keyState.equals(KeyState.K3)) {
				weightController.showMessagePrimaryDisplay("");
				taraWeight = weight;
				weight = 0.0;

			}
			else
				System.out.println("ES");
			break;
		case DW:
			if (keyState.equals(KeyState.K4) || keyState.equals(KeyState.K3)) {
				weightController.showMessagePrimaryDisplay("");
				weightController.showMessageSecondaryDisplay("");
				currentDisplay = "";
				weight = 0.0;
			}
			else
				System.out.println("ES");
			break;
		case K:
			handleKMessage(message);
			break;
		case P111:
			if (keyState.equals(KeyState.K4) || keyState.equals(KeyState.K3)) {
				weightController.showMessageSecondaryDisplay(message.getMessage());
			}
			else
				System.out.println("ES");
			break;
		default:
			System.out.println(message.getMessage());
			break;
		}
	}

	private void handleKMessage(SocketInMessage message) {
		switch (message.getMessage()) {
		case "1" :
			this.keyState = KeyState.K1;
			socketHandler.sendMessage(new SocketOutMessage("ok"));
			break;
		case "2" :
			this.keyState = KeyState.K2;
			socketHandler.sendMessage(new SocketOutMessage("ok"));
			break;
		case "3" :
			this.keyState = KeyState.K3;
			socketHandler.sendMessage(new SocketOutMessage("ok"));
			break;
		case "4" :
			this.keyState = KeyState.K4;
			socketHandler.sendMessage(new SocketOutMessage("ok"));
			break;
		default:
			socketHandler.sendMessage(new SocketOutMessage("ES\n\r"));
			break;
		}
	}

	//Listening for UI input
	@Override
	public void notifyKeyPress(KeyPress keyPress) {

		switch (keyPress.getType()) {
		case SOFTBUTTON:
			break;
		case TARA:
			if (keyState.equals(KeyState.K1) || keyState.equals(KeyState.K2) ){
					//Do nothing atm...
			}
			else if (keyState.equals(KeyState.K3) || keyState.equals(KeyState.K4)) {
				if (weight > 0 && tester != "RM208") {
					taraWeight = weight;
					weight = 0.0;
					weightController.showMessagePrimaryDisplay("");
					weightController.showMessageSecondaryDisplay("Tared weight is: " + taraWeight);
					socketHandler.sendMessage(new SocketOutMessage(taraWeight.toString()));
				}
				else {
					//Do nothing...
				}
			}
			currentDisplay = "";
			break;
		case TEXT:
			int c = keyPress.getCharacter();
			if (c >= 48 && c <= 57) {
				if (currentDisplay == "") 
					currentDisplay = String.valueOf(c-48);
				else if (Pattern.matches("[a-zA-Z.?]+", currentDisplay))
					currentDisplay = String.valueOf(c-48);
				else
					currentDisplay += String.valueOf(c-48);
				weight = Double.parseDouble(currentDisplay);
				weightController.showMessagePrimaryDisplay(currentDisplay);
			}
			else if (c > 57 || c == 46) {
				if (Pattern.matches("[0-9.]+", currentDisplay) && c != 46) 
					currentDisplay = Character.toString((char) c); 
				else 
					currentDisplay += Character.toString((char) c);
				weightController.showMessagePrimaryDisplay(currentDisplay);
				weight = 0.0;
			}
			else
				System.out.println("Not a number.");
			break;
		case ZERO:
			if (keyState.equals(KeyState.K1) || keyState.equals(KeyState.K2) ){
				//Do nothing...
			}
			else if (keyState.equals(KeyState.K3) || keyState.equals(KeyState.K4)) {
				resetDisplay();
				weightController.showMessageSecondaryDisplay("");
				weight = 0.0;
			}
			break;
		case C:
			resetDisplay();
			break;
		case EXIT:
			socketHandler.sendMessage(new SocketOutMessage("Terminating weight..."));
			System.exit(0);
			break;
		case SEND:
			if (keyState.equals(KeyState.K4) || keyState.equals(KeyState.K3)) {
				socketHandler.sendMessage(new SocketOutMessage(currentDisplay + "\n\r"));
				resetDisplay();
			}
			else
				System.out.println("ES");
			break;
		}
	}

	@Override
	public void notifyWeightChange(double newWeight) {
		weight = newWeight;

		weightController.showMessagePrimaryDisplay(Double.toString(newWeight));
	}

	/**
	 * @author Sammy Masoule
	 * 
	 * @desc converts a string to a double and returns true or false whether the string has been converted or not.
	 * 
	 */
	public boolean isDouble(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**
	 * @author Niklas Broch Thielemann
	 * @description Simple method to reset display...
	 * 
	 */
	public void resetDisplay() {
		weightController.showMessagePrimaryDisplay("");
		currentDisplay = "";
	}

}
