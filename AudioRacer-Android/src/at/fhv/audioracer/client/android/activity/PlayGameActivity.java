package at.fhv.audioracer.client.android.activity;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import at.fhv.audioracer.client.android.R;
import at.fhv.audioracer.client.android.activity.listener.IControlMode;
import at.fhv.audioracer.client.android.controller.ClientManager;
import at.fhv.audioracer.client.android.network.task.PlayerReadyAsyncTask;
import at.fhv.audioracer.client.android.network.task.TrimSettingsAsyncTask;
import at.fhv.audioracer.client.android.network.task.params.NetworkParams;
import at.fhv.audioracer.client.android.util.SystemUiHider;
import at.fhv.audioracer.client.android.util.SystemUiHiderAndroidRacer;

/**
 * This is the main activity to play the game.<br>
 * At first, a player can choose between different 'control modes':<br>
 * <ul>
 * <li>Standard controls: 4 buttons layout: up, down, left, right</li>
 * <li>(not yet implemented) Joystick controls: use an 'on-screen-joystick' to control the car.</li>
 * <li>(not yet implemented) Motion controls: use the motion sensors to control the car.</li>
 * </ul>
 */
public class PlayGameActivity extends Activity implements IControlMode {
	
	private SystemUiHider _systemUiHider;
	
	public static enum ControlMode {
		STANDARD,
		// JOYSTICK,
		SENSOR, SETTINGS_TRIM, ;
	}
	
	protected boolean _speedUp;
	protected boolean _speedDown;
	protected boolean _steerLeft;
	protected boolean _steerRight;
	
	private View _controlsSettingsControlsView;
	private View _standardControlsView;
	private View _motionSensorControlsView;
	private View _trimSettingsView;
	
	private ImageView _msCtrlImgView;
	
	private List<ThreadControlMode> _threads;
	
	private List<View> _views;
	
	private ControlMode _chosenControlMode;
	
	private TextView _tsSpeedValueTextView;
	private TextView _tsSteeringValueTextView;
	private float _trimSpeed;
	private float _trimSteering;
	private static float TRIM_STEP = 0.1f; // increase trim settings by TRIM_STEP per sec.
	private Pressed _trimSpeedUp = new Pressed();
	private Pressed _trimSpeedDown = new Pressed();
	private Pressed _trimSteeringUp = new Pressed();
	private Pressed _trimSteeringDown = new Pressed();
	
	@Override
	public void onBackPressed() {
		if (_controlsSettingsControlsView.getVisibility() == View.VISIBLE) {
			super.onBackPressed();
		} else {
			stopAllThreads();
			hideAllViews();
			_controlsSettingsControlsView.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_play_game);
		
		/* get all views */
		// controls
		_controlsSettingsControlsView = findViewById(R.id.controls_settings);
		_standardControlsView = findViewById(R.id.standard_controls);
		_motionSensorControlsView = findViewById(R.id.motion_sensor_controls);
		// settings
		_trimSettingsView = findViewById(R.id.trim_settings_view);
		
		_views = new LinkedList<View>();
		_views.add(_controlsSettingsControlsView);
		_views.add(_standardControlsView);
		_views.add(_motionSensorControlsView);
		_views.add(_trimSettingsView);
		
		// get the 'motion sensor' image view.
		// a circle and the current 'motion position' will be drawn onto this view.
		_msCtrlImgView = (ImageView) findViewById(R.id.ms_ctrl_img_view);
		
		// set other views than 'controlsSettingsControlsView' invisible
		_standardControlsView.setVisibility(View.INVISIBLE);
		_motionSensorControlsView.setVisibility(View.INVISIBLE);
		_trimSettingsView.setVisibility(View.INVISIBLE);
		
		/* Settings */
		final Button trimSettingsButton = (Button) findViewById(R.id.trim_settings);
		trimSettingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.d("trim button", "clicked on trim button");
				_trimSettingsView.setVisibility(View.VISIBLE);
				
				_standardControlsView.setVisibility(View.INVISIBLE);
				_motionSensorControlsView.setVisibility(View.INVISIBLE);
				_controlsSettingsControlsView.setVisibility(View.INVISIBLE);
				
				startThread(ControlMode.SETTINGS_TRIM);
			}
		});
		/* End of: Settings */
		
		final Button tsSpeedUpButton = (Button) findViewById(R.id.trim_settings_speed_up);
		final Button tsSpeedDownButton = (Button) findViewById(R.id.trim_settings_speed_down);
		final Button tsSteeringUpButton = (Button) findViewById(R.id.trim_settings_steering_up);
		final Button tsSteeringDownButton = (Button) findViewById(R.id.trim_settings_steering_down);
		tsSpeedUpButton.setOnTouchListener(new PressedTouchListener(_trimSpeedUp));
		tsSpeedDownButton.setOnTouchListener(new PressedTouchListener(_trimSpeedDown));
		tsSteeringUpButton.setOnTouchListener(new PressedTouchListener(_trimSteeringUp));
		tsSteeringDownButton.setOnTouchListener(new PressedTouchListener(_trimSteeringDown));
		_tsSpeedValueTextView = (TextView) findViewById(R.id.trim_settings_speed_value);
		_tsSteeringValueTextView = (TextView) findViewById(R.id.trim_settings_steering_value);
		
		final Button tsSetTrimButton = (Button) findViewById(R.id.trim_settings_set_trim);
		tsSetTrimButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setTrim();
			}
		});
		
		// possible threads
		_threads = new LinkedList<PlayGameActivity.ThreadControlMode>();
		_threads.add(new ThreadControlMode(ControlMode.STANDARD, new StandardControlThread()));
		_threads.add(new ThreadControlMode(ControlMode.SENSOR, new MotionSensorControlThread()));
		_threads.add(new ThreadControlMode(ControlMode.SETTINGS_TRIM, new TrimSettingsThread()));
		
		/* ChooseControls */
		
		// Choose 'Standard controls'
		final Button stdCtrlButton = (Button) findViewById(R.id.std_ctrl);
		stdCtrlButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ready(ControlMode.STANDARD);
			}
		});
		// Choose 'Motion Sensor'
		final Button msCtrlButton = (Button) findViewById(R.id.ms_ctrl);
		msCtrlButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ready(ControlMode.SENSOR);
			}
		});
		
		final Button upButton = (Button) findViewById(R.id.std_ctrl_up);
		final Button downButton = (Button) findViewById(R.id.std_ctrl_down);
		final Button leftButton = (Button) findViewById(R.id.std_ctrl_left);
		final Button rightButton = (Button) findViewById(R.id.std_ctrl_right);
		upButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (MotionEvent.ACTION_DOWN == event.getAction()) {
					_speedUp = true;
				}
				if (MotionEvent.ACTION_UP == event.getAction()) {
					_speedUp = false;
				}
				Log.d("control", "speedUp " + _speedUp);
				return true;
			}
		});
		downButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (MotionEvent.ACTION_DOWN == event.getAction()) {
					_speedDown = true;
				}
				if (MotionEvent.ACTION_UP == event.getAction()) {
					_speedDown = false;
				}
				Log.d("control", "speedDown " + _speedDown);
				return true;
			}
		});
		leftButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (MotionEvent.ACTION_DOWN == event.getAction()) {
					_steerLeft = true;
				}
				if (MotionEvent.ACTION_UP == event.getAction()) {
					_steerLeft = false;
				}
				Log.d("control", "steerLeft " + _steerLeft);
				return true;
			}
		});
		rightButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (MotionEvent.ACTION_DOWN == event.getAction()) {
					_steerRight = true;
				}
				if (MotionEvent.ACTION_UP == event.getAction()) {
					_steerRight = false;
				}
				Log.d("control", "steerRight " + _steerRight);
				return true;
			}
		});
		
		/* End of: ChooseControls */
		
		// hide SystemUi
		_systemUiHider = new SystemUiHiderAndroidRacer(this, _standardControlsView, SystemUiHider.FLAG_FULLSCREEN);
		_systemUiHider.setup();
		_systemUiHider.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
			@Override
			public void onVisibilityChange(boolean visible) {
				Log.d("foo", "onVisibilityChange: visible? " + visible);
			}
		});
		
		_systemUiHider.hide();
	}
	
	private void hideAllViews() {
		for (View view : _views) {
			view.setVisibility(View.INVISIBLE);
		}
	}
	
	private void trimSpeedUp(float trimStep) {
		_trimSpeed = Math.min(1.0f, _trimSpeed + trimStep);
		setTrimValue(_tsSpeedValueTextView, _trimSpeed);
	}
	
	private void trimSpeedDown(float trimStep) {
		_trimSpeed = Math.max(-1.0f, _trimSpeed - trimStep);
		setTrimValue(_tsSpeedValueTextView, _trimSpeed);
	}
	
	private void trimSteeringUp(float trimStep) {
		_trimSteering = Math.min(1.0f, _trimSteering + trimStep);
		setTrimValue(_tsSteeringValueTextView, _trimSteering);
	}
	
	private void trimSteeringDown(float trimStep) {
		_trimSteering = Math.max(-1.0f, _trimSteering - trimStep);
		setTrimValue(_tsSteeringValueTextView, _trimSteering);
	}
	
	private void setTrim() {
		_trimSpeed = 0.0f;
		_trimSteering = 0.0f;
		setTrimValue(_tsSpeedValueTextView, _trimSpeed);
		setTrimValue(_tsSteeringValueTextView, _trimSteering);
		sendTrim();
	}
	
	/**
	 * Sets given float <code>value</code> to given <code>TextView</code>. It also takes care of the algebraic sign. If the the value is below 0, the value will
	 * be preceded by a '-'; a '+' otherwise.
	 * 
	 * @param view
	 * @param value
	 */
	private void setTrimValue(final TextView view, float value) {
		final char[] v = String.format(" %s%.2f", ((value >= 0) ? "+" : ""), value).toCharArray();
		view.post(new Runnable() {
			@Override
			public void run() {
				view.setText(v, 0, v.length);
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// enable controls again.
		if (_chosenControlMode != null) {
			setControlMode(_chosenControlMode);
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		stopAllThreads();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		stopAllThreads();
	}
	
	private void ready(ControlMode mode) {
		new PlayerReadyAsyncTask(this, mode).execute(new NetworkParams());
	}
	
	private void sendTrim() {
		new TrimSettingsAsyncTask().execute(new NetworkParams());
	}
	
	@Override
	public void setControlMode(ControlMode mode) {
		_chosenControlMode = mode;
		switch (mode) {
			case STANDARD:
				_controlsSettingsControlsView.setVisibility(View.INVISIBLE);
				_standardControlsView.setVisibility(View.VISIBLE);
				_motionSensorControlsView.setVisibility(View.INVISIBLE);
				
				startThread(mode);
				break;
			case SENSOR:
				_controlsSettingsControlsView.setVisibility(View.INVISIBLE);
				_standardControlsView.setVisibility(View.INVISIBLE);
				_motionSensorControlsView.setVisibility(View.VISIBLE);
				
				startThread(mode);
				break;
			default:
				// set control to STANDARD
				setControlMode(ControlMode.STANDARD);
		}
	}
	
	private void startThread(ControlMode mode) {
		for (ThreadControlMode tcm : _threads) {
			if (tcm.mode == mode) {
				tcm.thread.start();
				return;
			}
		}
	}
	
	private void stopAllThreads() {
		for (ThreadControlMode tcm : _threads) {
			tcm.thread.stop();
		}
	}
	
	private class Pressed {
		private boolean pressed;
	}
	
	private class PressedTouchListener implements View.OnTouchListener {
		
		private volatile Pressed pressed;
		
		public PressedTouchListener(Pressed pressed) {
			this.pressed = pressed;
		}
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (MotionEvent.ACTION_DOWN == event.getAction()) {
				pressed.pressed = true;
			}
			if (MotionEvent.ACTION_UP == event.getAction()) {
				pressed.pressed = false;
			}
			
			return true;
		}
		
	}
	
	/* Standard controls */
	
	private static final float CONTROL_SENSITY = 1.f;
	
	private float _speed;
	private float _direction;
	
	private class ThreadControlMode {
		private ControlMode mode;
		private ControlThread thread;
		
		public ThreadControlMode(ControlMode mode, ControlThread thread) {
			this.mode = mode;
			this.thread = thread;
		}
	}
	
	protected abstract class ControlThread implements Runnable {
		
		protected long _maxControlWait;
		
		protected volatile long _lastUpdate;
		private volatile Thread _thread;
		
		public ControlThread() {
			_maxControlWait = 10;
		}
		
		public void start() {
			_thread = new Thread(this);
			_thread.start();
		}
		
		public void stop() {
			_thread = null;
			reset();
		}
		
		protected abstract void reset();
		
		@Override
		public void run() {
			Thread thisThread = Thread.currentThread();
			_lastUpdate = System.currentTimeMillis();
			while (thisThread == _thread) {
				long now = System.currentTimeMillis();
				// call the 'hook'
				control();
				long wait = _maxControlWait - (System.currentTimeMillis() - _lastUpdate);
				_lastUpdate = now;
				if (wait > 0) {
					try {
						Thread.sleep(wait);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		public abstract void control();
	}
	
	protected class TrimSettingsThread extends ControlThread {
		
		public TrimSettingsThread() {
			super();
			_maxControlWait = 10;
		}
		
		@Override
		protected void reset() {
			_trimSpeedUp.pressed = false;
			_trimSpeedDown.pressed = false;
			_trimSteeringUp.pressed = false;
			_trimSteeringDown.pressed = false;
			_trimSpeed = 0.0f;
			_trimSteering = 0.0f;
		}
		
		@Override
		public void control() {
			float sensity = (((System.currentTimeMillis() - _lastUpdate) / 1000.f) * TRIM_STEP);
			if (_trimSpeedUp.pressed) {
				trimSpeedUp(sensity);
			}
			if (_trimSpeedDown.pressed) {
				trimSpeedDown(sensity);
			}
			if (_trimSteeringUp.pressed) {
				trimSteeringUp(sensity);
			}
			if (_trimSteeringDown.pressed) {
				trimSteeringDown(sensity);
			}
			
			ClientManager.getInstance().getPlayerClient().getPlayerServer().updateVelocity(_trimSpeed, _trimSteering);
		}
		
	}
	
	protected class StandardControlThread extends ControlThread {
		@Override
		public void control() {
			/* copied from AudioRacer-PlayerSimulator: package at.fhv.audioracer.simulator.player.pivot.CarControlComponent */
			float sensity = (((System.currentTimeMillis() - _lastUpdate) / 1000.f) * CONTROL_SENSITY);
			if (_speedUp) {
				_speed = Math.min(1.f, (_speed + sensity));
			} else if (_speedDown) {
				_speed = Math.max(-1.f, (_speed - sensity));
			} else if (_speed < 0) {
				_speed = Math.min(0.f, (_speed + sensity));
			} else if (_speed > 0) {
				_speed = Math.max(0.f, (_speed - sensity));
			}
			
			if (_steerLeft) {
				_direction = Math.max(-1.f, (_direction - sensity));
			} else if (_steerRight) {
				_direction = Math.min(1.f, (_direction + sensity));
			} else if (_direction < 0) {
				_direction = Math.min(0.f, (_direction + sensity));
			} else if (_direction > 0) {
				_direction = Math.max(0.f, (_direction - sensity));
			}
			/* end of: copied from AudioRacer-PlayerSimulator: package at.fhv.audioracer.simulator.player.pivot.CarControlComponent */
			
			// note that this is sent continuously
			ClientManager.getInstance().getPlayerClient().getPlayerServer().updateVelocity(_speed, _direction);
		}
		
		@Override
		protected void reset() {
			_speedUp = false;
			_speedDown = false;
			_steerLeft = false;
			_steerRight = false;
			_speed = 0.0f;
			_direction = 0.0f;
		}
		
	}
	
	/* end of: Standard controls */
	
	/* Motion sensor controls */
	protected class MotionSensorControlThread extends ControlThread {
		
		private Bitmap _bmp;
		private Paint _cPaint;
		private Paint _pPaint;
		private Canvas _cv;
		
		private Integer _minDim = null;
		
		float _x;
		float _y;
		float _z;
		
		private SensorManager _sensorManager;
		private Sensor _sensor;
		
		private SensorEventListener _sensorListener;
		
		public MotionSensorControlThread() {
			int cxy = getMinScreenDimension();
			_bmp = Bitmap.createBitmap(cxy, cxy, Bitmap.Config.ARGB_8888);
			_cv = new Canvas(_bmp);
			
			// circle paint
			_cPaint = new Paint();
			_cPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
			_cPaint.setColor(Color.WHITE);
			
			// point paint
			_pPaint = new Paint();
			_pPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
			_pPaint.setColor(Color.BLUE);
		}
		
		private int getMinScreenDimension() {
			if (_minDim == null) {
				Display display = getWindowManager().getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);
				_minDim = Math.min(size.x, size.y);
				Log.d(ACTIVITY_SERVICE, "getMinScreenDimension: " + _minDim);
			}
			return _minDim;
		}
		
		private void initSensorValues() {
			if (_sensorManager == null) {
				_sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
				_sensor = _sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
				_sensorListener = new SensorEventListener() {
					
					@Override
					public void onSensorChanged(SensorEvent event) {
						float[] values = event.values;
						_x = values[0];
						_y = values[1];
						_z = values[2];
						String xyz = String.format("%f4.5\t%f4.5\t%f4.5", _x, _y, _z);
						Log.d("sensor", xyz);
					}
					
					@Override
					public void onAccuracyChanged(Sensor sensor, int accuracy) {
						// no op
					}
					
				};
				_sensorManager.registerListener(_sensorListener, _sensor, SensorManager.SENSOR_DELAY_GAME);
			}
		}
		
		@Override
		protected void reset() {
			if (_sensorManager != null) {
				_sensorManager.unregisterListener(_sensorListener);
			}
			_sensorManager = null;
			_sensor = null;
		}
		
		@Override
		public void control() {
			initSensorValues();
			
			final int cxy = getMinScreenDimension();
			final int centerXY = cxy / 2;
			final int radius = cxy / 2;
			final int motionX = centerXY + (int) (radius * _y);
			final int motionY = centerXY + (int) (radius * _x);
			
			_msCtrlImgView.post(new Runnable() {
				@Override
				public void run() {
					
					_cv.drawCircle(centerXY, centerXY, radius, _cPaint);
					_cv.drawCircle(motionX, motionY, 5, _pPaint);
					
					_msCtrlImgView.setBackground(new BitmapDrawable(getResources(), _bmp));
				}
			});
		}
	}
	/* end of: Motion sensor controls */
	
}