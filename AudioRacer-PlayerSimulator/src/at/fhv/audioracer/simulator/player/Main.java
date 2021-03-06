package at.fhv.audioracer.simulator.player;

import java.io.IOException;
import java.net.InetAddress;

import at.fhv.audioracer.communication.player.PlayerNetwork;
import at.fhv.audioracer.communication.player.message.FreeCarsMessage;
import at.fhv.audioracer.communication.player.message.PlayerMessage;
import at.fhv.audioracer.communication.player.message.SetPlayerNameResponseMessage;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class Main {
	
	public static void main(String[] args) {
		try {
			startClient();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static Client client;
	
	private static void startClient() throws IOException {
		// test purpose
		
		client = new Client();
		client.start();
		
		PlayerNetwork.register(client);
		
		client.addListener(new Listener() {
			public void received(Connection connection, Object object) {
				if (object instanceof PlayerMessage) {
					PlayerMessage message = (PlayerMessage) object;
					switch (message.messageId) {
						case SET_PLAYER_NAME_RESPONSE:
							SetPlayerNameResponseMessage setNameRespone = (SetPlayerNameResponseMessage) message;
							System.out.println("player name set ... server responded with id: " + setNameRespone.playerId);
							break;
						case UPDATE_FREE_CARS:
							System.out.println("Free cars message incoming.");
							FreeCarsMessage freeCarsMsg = (FreeCarsMessage) message;
							if (freeCarsMsg.freeCars != null) {
								for (int i = 0; i < freeCarsMsg.freeCars.length; i++) {
									System.out.println("Car with id: " + freeCarsMsg.freeCars[i] + " is free.");
								}
							} else {
								System.out.println("Free cars are empty!");
							}
							break;
						default:
							System.out.println("Message with id: " + message.messageId + " not known!");
							break;
					}
				}
			}
		});
		client.connect(1000, InetAddress.getLoopbackAddress(), PlayerNetwork.PLAYER_SERVICE_PORT, PlayerNetwork.PLAYER_SERVICE_PORT);
		
		// Kryo Clients are running as daemons, prevent main application from exit
		try {
			while (true) {
				Thread.sleep(Long.MAX_VALUE);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
