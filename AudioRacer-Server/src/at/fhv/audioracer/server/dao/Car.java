package at.fhv.audioracer.server.dao;

import java.awt.image.BufferedImage;

import at.fhv.audioracer.core.util.Direction;
import at.fhv.audioracer.core.util.Position;
import at.fhv.audioracer.server.proxy.CarCommunicationProxy;

public class Car extends at.fhv.audioracer.core.model.Car {
	
	/**
	 * Player associated with this car
	 */
	private Player _player;
	
	/**
	 * server - car communication
	 */
	private CarCommunicationProxy _proxy;
	
	public Car(int cardId) {
		super(cardId);
	}
	
	public Car(int carId, Position position, Direction direction, BufferedImage image) {
		super(carId, position, direction, image);
	}
	
	public Player getPlayer() {
		return _player;
	}
	
	public void setPlayer(Player player) {
		_player = player;
	}
	
	public CarCommunicationProxy getProxy() {
		return _proxy;
	}
	
	public void setProxy(CarCommunicationProxy proxy) {
		_proxy = proxy;
	}
}
