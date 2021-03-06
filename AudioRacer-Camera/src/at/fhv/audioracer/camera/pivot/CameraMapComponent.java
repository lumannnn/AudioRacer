package at.fhv.audioracer.camera.pivot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.naming.OperationNotSupportedException;

import org.apache.pivot.wtk.Mouse;
import org.apache.pivot.wtk.Mouse.Button;
import org.apache.pivot.wtk.Mouse.ScrollType;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;

import at.fhv.audioracer.camera.OpenCVCamera;
import at.fhv.audioracer.camera.OpenCVCameraListener;
import at.fhv.audioracer.core.model.Map;
import at.fhv.audioracer.core.util.Position;
import at.fhv.audioracer.ui.pivot.MapComponent;

public class CameraMapComponent extends MapComponent implements OpenCVCameraListener {
	private OpenCVCamera _camera;
	
	private boolean _drawCheesboard;
	
	private boolean _positioning;
	private int _zoom;
	private int _positionX;
	private int _positionY;
	
	private Position _lastMousePosition; // TODO: use a position which uses integers as values
	
	private boolean _selecting;
	
	private boolean _cameraImagePosCalculated;
	private int _cameraImagePosX;
	private int _cameraImagePosY;
	private int _cameraImageWidth;
	private int _cameraImageHeight;
	private float _cameraImageWidthScale;
	private float _cameraImageHeightScale;
	
	private int _gameAreaX1;
	private int _gameAreaX2;
	private int _gameAreaY1;
	private int _gameAreaY2;
	
	private final CameraMapComponentSkin _skin;
	
	public CameraMapComponent() {
		_skin = new CameraMapComponentSkin(this);
		setSkin(_skin);
		
		_positionX = 0;
		_positionY = 0;
		_zoom = 50;
		_camera = new OpenCVCamera(_positionX, _positionY, _zoom);
		_camera.getListenerList().add(this);
		CameraApplication.getInstance().setCamera(_camera);
		
		_drawCheesboard = false;
	}
	
	public BufferedImage getCameraImage() {
		Mat frame = _camera.getFrame();
		if (frame == null) {
			return null;
		} else {
			if (_drawCheesboard) {
				MatOfPoint2f corners = new MatOfPoint2f();
				if (Calib3d.findChessboardCorners(frame, _camera.getPatternSize(), corners,
						Calib3d.CALIB_CB_FAST_CHECK)) {
					Calib3d.drawChessboardCorners(frame, _camera.getPatternSize(), corners, true);
				}
			}
			
			BufferedImage img = matToBufferedImage(frame);
			frame.release();
			if (!_cameraImagePosCalculated) {
				refreshCameraImagePos(img.getWidth(), img.getHeight());
			}
			if (_selecting) {
				drawGameArea(img);
			}
			return img;
		}
	}
	
	public int getCameraImagePosX() {
		return _cameraImagePosX;
	}
	
	public int getCameraImagePosY() {
		return _cameraImagePosY;
	}
	
	public int getCameraImageWidth() {
		return _cameraImageWidth;
	}
	
	public int getCameraImageHeight() {
		return _cameraImageHeight;
	}
	
	public boolean isDrawCheesboard() {
		return _drawCheesboard;
	}
	
	public void setDrawCheesboard(boolean drawCheesboard) {
		_drawCheesboard = drawCheesboard;
	}
	
	private void refreshCameraImagePos(int cameraWidth, int cameraHeight) {
		int height = getHeight();
		int width = getWidth();
		
		float oversizeX = (float) cameraWidth / (float) width;
		float oversizeY = (float) cameraHeight / (float) height;
		
		if (oversizeX > oversizeY) {
			_cameraImageWidth = width;
			_cameraImageHeight = (int) (((float) width / (float) cameraWidth) * (float) cameraHeight);
			_cameraImagePosX = 0;
			_cameraImagePosY = (height - _cameraImageHeight) / 2;
		} else {
			_cameraImageWidth = (int) (((float) height / (float) cameraHeight) * (float) cameraWidth);
			_cameraImageHeight = height;
			_cameraImagePosX = (width - _cameraImageWidth) / 2;
			_cameraImagePosY = 0;
		}
		
		_cameraImageWidthScale = (float) cameraWidth / (float) _cameraImageWidth;
		_cameraImageHeightScale = (float) cameraHeight / (float) _cameraImageHeight;
		
		_cameraImagePosCalculated = true;
	}
	
	public void selectCamera(int id) {
		_camera.openCamera(id);
	}
	
	public void startCalibration() {
		_positioning = false;
		_camera.startCalibration();
	}
	
	public boolean endCalibration() {
		return _camera.endCalibration();
	}
	
	public boolean startPositioning() {
		if (_camera.beginPositioning()) {
			_positioning = true;
			return true;
		} else {
			return false;
		}
	}
	
	public void startSelectGameArea() {
		_camera.endPositioning();
		_positioning = false;
		_selecting = true;
	}
	
	@Override
	public void onNewFrame() {
		repaint();
	}
	
	@Override
	protected void layout() {
		_cameraImagePosCalculated = false;
		
		super.layout();
	}
	
	@Override
	protected boolean mouseDown(Button button, int xArgument, int yArgument) {
		if (_positioning && button == Button.LEFT) {
			_lastMousePosition = new Position(xArgument, yArgument);
			return true;
		} else if (_selecting && button == Button.LEFT) {
			_gameAreaX1 = getRealImageCoordinateX(xArgument);
			_gameAreaY1 = getRealImageCoordinateY(yArgument);
			_gameAreaX2 = _gameAreaX1;
			_gameAreaY2 = _gameAreaY1;
			return true;
		} else {
			return super.mouseDown(button, xArgument, yArgument);
		}
	}
	
	@Override
	protected boolean mouseMove(int xArgument, int yArgument) {
		if (_positioning && _lastMousePosition != null) {
			if (!Mouse.isPressed(Button.LEFT)) {
				_lastMousePosition = null;
			} else {
				_positionX += (xArgument - _lastMousePosition.getPosX());
				_positionY += (yArgument - _lastMousePosition.getPosY());
				_camera.setPosition(_positionX, _positionY);
				
				_lastMousePosition.setPosX(xArgument);
				_lastMousePosition.setPosY(yArgument);
			}
			
			return true;
		} else if (_selecting && Mouse.isPressed(Button.LEFT)) {
			_gameAreaX2 = getRealImageCoordinateX(xArgument);
			_gameAreaY2 = getRealImageCoordinateY(yArgument);
			return true;
		} else {
			return super.mouseMove(xArgument, yArgument);
		}
	}
	
	@Override
	protected boolean mouseUp(Button button, int xArgument, int yArgument) {
		if (_positioning && button == Button.LEFT) {
			_lastMousePosition = null;
			return true;
		} else {
			return super.mouseUp(button, xArgument, yArgument);
		}
	}
	
	@Override
	protected boolean mouseWheel(ScrollType scrollType, int scrollAmount, int wheelRotation,
			int xArgument, int yArgument) {
		
		if (_positioning) {
			_zoom -= wheelRotation;
			_camera.setZoom(_zoom);
			return true;
		} else {
			return super.mouseWheel(scrollType, scrollAmount, wheelRotation, xArgument, yArgument);
		}
	}
	
	public void rotate() {
		_camera.rotate();
	}
	
	public void setRotation(int degree) {
		_camera.setRotation(degree);
	}
	
	public boolean gameAreaSelected() {
		int width = Math.abs(_gameAreaX1 - _gameAreaX2);
		int height = Math.abs(_gameAreaY1 - _gameAreaY2);
		
		if (width == 0 || height == 0) {
			return false;
		}
		
		Map map = new Map(width, height);
		map.getMapListenerList().add(CameraApplication.getInstance());
		CameraApplication.getInstance().configureMap(map);
		_selecting = false;
		
		int offsetX = Math.min(_gameAreaX1, _gameAreaX2);
		int offsetY = Math.min(_gameAreaY1, _gameAreaY2);
		_camera.setMap(map, offsetX, offsetY);
		
		_skin.setMapOffset(offsetX, offsetY);
		try {
			setMap(map);
		} catch (OperationNotSupportedException e) {
			// this function can be called just once
		}
		
		return true;
	}
	
	private int getRealImageCoordinateX(int x) {
		return (int) ((x - _cameraImagePosX) * _cameraImageWidthScale);
	}
	
	private int getRealImageCoordinateY(int y) {
		return (int) ((y - _cameraImagePosY) * _cameraImageHeightScale);
	}
	
	private void drawGameArea(BufferedImage img) {
		int x = Math.min(_gameAreaX1, _gameAreaX2);
		int y = Math.min(_gameAreaY1, _gameAreaY2);
		int width = Math.abs(_gameAreaX1 - _gameAreaX2);
		int height = Math.abs(_gameAreaY1 - _gameAreaY2);
		
		Graphics g = img.getGraphics();
		g.setColor(Color.RED);
		g.drawRect(x, y, width, height);
	}
	
	/**
	 * Converts/writes a Mat into a BufferedImage.
	 * 
	 * @param matrix
	 *            Mat of type CV_8UC3 or CV_8UC1
	 * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY
	 */
	private BufferedImage matToBufferedImage(Mat matrix) {
		int cols = matrix.cols();
		int rows = matrix.rows();
		int elemSize = (int) matrix.elemSize();
		byte[] data = new byte[cols * rows * elemSize];
		int type;
		matrix.get(0, 0, data);
		switch (matrix.channels()) {
			case 1:
				type = BufferedImage.TYPE_BYTE_GRAY;
				break;
			case 3:
				type = BufferedImage.TYPE_3BYTE_BGR;
				// bgr to rgb
				byte b;
				for (int i = 0; i < data.length; i = i + 3) {
					b = data[i];
					data[i] = data[i + 2];
					data[i + 2] = b;
				}
				break;
			default:
				return null;
		}
		BufferedImage image2 = new BufferedImage(cols, rows, type);
		image2.getRaster().setDataElements(0, 0, cols, rows, data);
		return image2;
	}
	
	public int[] getHueValues(int x, int y) {
		x = getRealImageCoordinateX(x);
		y = getRealImageCoordinateY(y);
		
		Mat frame = _camera.getFrame();
		Mat pixel = frame.col(x).row(y);
		Mat pixelHue = new Mat();
		Imgproc.cvtColor(pixel, pixelHue, Imgproc.COLOR_BGR2HSV);
		
		byte[] tmp = new byte[3];
		pixelHue.get(0, 0, tmp);
		
		frame.release();
		
		int[] data = new int[3];
		data[0] = tmp[0] & 0xFF; // get the unsigned value of the byte
		data[1] = tmp[1] & 0xFF;
		data[2] = tmp[2] & 0xFF;
		
		return data;
	}
	
	public void updateHueRange(int colorLower, int colorUpper, int saturationLower,
			int saturationUpper, int valueLower, int valueUpper) {
		_camera.updateHueRange(colorLower, colorUpper, saturationLower, saturationUpper,
				valueLower, valueUpper);
		
	}
	
	public boolean calibrationStep() {
		return _camera.calibrationStep();
	}
	
	public void storeCalibration() throws IOException {
		_camera.storeCalibration();
	}
	
	public boolean loadCalibration() throws ClassNotFoundException, IOException {
		return _camera.loadCalibration();
	}
	
	/**
	 * Called when the HUE range values are configured, so that the direction color (which is used on all cars) is configured
	 */
	public void directionHueConfigured() {
		
		_camera.directionHueConfigured();
		
	}
	
	/**
	 * Called when the HUE range values are configured, so that it is possible to exactly detect one car.
	 * 
	 * @return true when exactly one car could be detected, otherwise false
	 */
	public boolean carConfigured() {
		return _camera.nextCar();
	}
	
	public void allCarsDetected() {
		_camera.allCarsDetected();
	}
	
	public boolean loadGameArea() {
		int[] gameArea = _camera.loadGameArea();
		if (gameArea == null || gameArea.length != 4) {
			return false;
		}
		
		_gameAreaX1 = gameArea[0];
		_gameAreaY1 = gameArea[1];
		_gameAreaX2 = gameArea[2];
		_gameAreaY2 = gameArea[3];
		
		if (gameAreaSelected()) {
			_drawCheesboard = false;
			return true;
		} else {
			return false;
		}
	}
	
	public void storeGameArea() {
		_camera.storeGameArea(new int[] { _gameAreaX1, _gameAreaY1, _gameAreaX2, _gameAreaY2 });
	}
}
