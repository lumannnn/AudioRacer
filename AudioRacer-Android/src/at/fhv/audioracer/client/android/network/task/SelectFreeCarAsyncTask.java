package at.fhv.audioracer.client.android.network.task;

import android.util.Log;
import at.fhv.audioracer.client.android.activity.listener.ISelectCarListener;
import at.fhv.audioracer.client.android.controller.ClientManager;
import at.fhv.audioracer.client.android.network.task.SelectFreeCarAsyncTask.SuccessMessage;
import at.fhv.audioracer.client.android.network.task.params.SelectCarParams;

public class SelectFreeCarAsyncTask extends NetworkAsyncTask<SelectCarParams, SuccessMessage> {
	
	ISelectCarListener _listener;
	
	public static class SuccessMessage {
		public boolean success;
		public int carId;
		
		public SuccessMessage(boolean success, int carId) {
			this.success = success;
			this.carId = carId;
		}
	}
	
	public SelectFreeCarAsyncTask(ISelectCarListener listener) {
		_listener = listener;
	}
	
	@Override
	protected SuccessMessage doInBackground(SelectCarParams... params) {
		Log.d("AsyncTask", this.getClass().getName());
		byte carId = params[0].carId;
		boolean success = ClientManager.getInstance().getPlayerClient().getPlayerServer().selectCar(carId);
		return new SuccessMessage(success, carId);
	}
	
	@Override
	protected void onPostExecute(SuccessMessage result) {
		Log.d("AsyncTask", "onPostExecute" + this.getClass().getName());
		_listener.notifySuccess(result);
	}
}
