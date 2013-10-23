package at.fhv.audioracer.server.proxy;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.fhv.audioracer.communication.player.IPlayerClient;
import at.fhv.audioracer.communication.player.IPlayerClientManager;
import at.fhv.audioracer.server.PlayerManager;

import com.esotericsoftware.kryonet.Connection;

/**
 * Created each time new Device connected. Communication with one Device only.
 * 
 * @author edi
 */
public class PlayerCommunicationProxy extends Connection implements IPlayerClientManager, IPlayerClient {
	
	UUID _plrId;
	
	private static final Logger _logger = LoggerFactory.getLogger(PlayerCommunicationProxy.class);
	
	// Remote PushObject for player communication
	private IPlayerClient _playerClient;
	
	public void setPlayerClient(IPlayerClient playerClient) {
		_playerClient = playerClient;
	}
	
	public PlayerCommunicationProxy() {
		// TODO: not work with uuid, use Integer
		_plrId = UUID.randomUUID();
	}
	
	@Override
	public int connect(String playerName) {
		return PlayerManager.addPlayer(_plrId, playerName);
	}
	
	@Override
	public void disconnect() {
		PlayerManager.removePlayer(_plrId);
	}
	
	@Override
	public void updateVelocity(float speed, float direction) {
		
	}
	
	@Override
	public boolean selectCar(int carId) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public byte[] getCarImage(int carId) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void setPlayerReady() {
		try {
			PlayerManager.getPlayer(_plrId).setReady(true);
		} catch (NullPointerException e) {
			_logger.error("Player with id: {} does not exist!", _plrId);
			_playerClient.invalidCommand();
		}
	}
	
	@Override
	public void updateGameState(int playerId, int coinsLeft, int time) {
		_playerClient.updateGameState(playerId, coinsLeft, time);
	}
	
	@Override
	public void playerConnected(int playerId, String playerName) {
		_playerClient.playerConnected(playerId, playerName);
	}
	
	@Override
	public void playerDisconnected(int playerId) {
		_playerClient.playerDisconnected(playerId);
	}
	
	@Override
	public void updateCheckpointDirection(float directionX, float directionY) {
		_playerClient.updateCheckpointDirection(directionX, directionY);
	}
	
	@Override
	public void updateFreeCars(int[] carIds) {
		_playerClient.updateFreeCars(carIds);
	}
	
	@Override
	public void gameOver() {
		_playerClient.gameOver();
	}
	
	@Override
	public void invalidCommand() {
		_playerClient.invalidCommand();
	}
}