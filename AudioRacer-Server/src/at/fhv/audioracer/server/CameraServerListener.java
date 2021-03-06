package at.fhv.audioracer.server;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.fhv.audioracer.communication.world.message.CameraMessage;
import at.fhv.audioracer.communication.world.message.CarDetectedMessage;
import at.fhv.audioracer.communication.world.message.ConfigureMapMessage;
import at.fhv.audioracer.communication.world.message.UpdateCarMessage;
import at.fhv.audioracer.core.model.Car;
import at.fhv.audioracer.core.util.Direction;
import at.fhv.audioracer.core.util.Position;
import at.fhv.audioracer.server.game.GameModerator;
import at.fhv.audioracer.server.model.Player;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

public class CameraServerListener extends Listener {
	
	private static Logger _logger = LoggerFactory.getLogger(CameraServerListener.class);
	private GameModerator _moderator;
	
	public CameraServerListener(GameModerator moderator) {
		_moderator = moderator;
	}
	
	public void received(Connection connection, Object object) {
		if (object instanceof CameraMessage) {
			CameraMessage message = (CameraMessage) object;
			CameraConnection cameraConnection = (CameraConnection) connection;
			switch (message.messageId) {
				case UPDATE_CAR:
					UpdateCarMessage updateCarMsg = (UpdateCarMessage) message;
					
					if (!cameraConnection.isValidUpdateCarMessage(updateCarMsg.seqNr))
						break;
					
					_moderator.updateCar(updateCarMsg.carId, updateCarMsg.posX, updateCarMsg.posY,
							updateCarMsg.direction);
					break;
				case CAR_DETECTED:
					CarDetectedMessage carDetectedMsg = (CarDetectedMessage) message;
					
					// get the car image
					BufferedImage carImage = null;
					if (carDetectedMsg.image != null && carDetectedMsg.image.length > 0) {
						try {
							InputStream in = new ByteInputStream(carDetectedMsg.image,
									carDetectedMsg.image.length);
							carImage = ImageIO.read(in);
							in.close();
						} catch (IOException e) {
							_logger.error("Exception caught while reading car image!", e);
						}
						
					} else {
						_logger.error("No image received for car with id: {}", carDetectedMsg.carId);
					}
					
					if (carImage != null) {
						
						Car<Player> car = new Car<Player>(carDetectedMsg.carId, new Position(
								carDetectedMsg.posX, carDetectedMsg.posY), new Direction(
								carDetectedMsg.direction), carImage);
						_moderator.carDetected(car);
					}
					
					break;
				case CONFIGURE_MAP:
					ConfigureMapMessage configureMapMsg = (ConfigureMapMessage) message;
					_moderator.configureMap(configureMapMsg.sizeX, configureMapMsg.sizeY);
					break;
				case DETECTION_FINISHED:
					_moderator.detectionFinished();
					break;
				default:
					_logger.warn("Camera message with id: {} not known!", message.messageId);
					break;
			}
		} else if (object instanceof FrameworkMessage.KeepAlive) {
			// _logger.debug("Received Camera keepAlive ...");
		}
	}
	
	public void disconnected(Connection connection) {
		_logger.info("Camera connection gone.");
	}
}
