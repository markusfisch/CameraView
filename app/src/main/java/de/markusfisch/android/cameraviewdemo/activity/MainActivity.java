package de.markusfisch.android.cameraviewdemo.activity;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import de.markusfisch.android.cameraview.widget.CameraView;
import de.markusfisch.android.cameraviewdemo.R;

public class MainActivity extends Activity {
	private static final int REQUEST_CAMERA = 1;

	private static boolean frontFacing = false;

	private CameraView cameraView;

	@Override
	public void onRequestPermissionsResult(
			int requestCode,
			String[] permissions,
			int[] grantResults) {
		if (requestCode == REQUEST_CAMERA &&
				grantResults.length > 0 &&
				grantResults[0] != PackageManager.PERMISSION_GRANTED) {
			Toast.makeText(
					this,
					R.string.error_camera,
					Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		checkPermissions();

		cameraView = new CameraView(this);
		cameraView.setUseOrientationListener(true);
		cameraView.setOnClickListener(v -> invertCamera());

		setContentView(cameraView);
	}

	@Override
	public void onResume() {
		super.onResume();
		openCameraView();
	}

	@Override
	public void onPause() {
		super.onPause();
		closeCameraView();
	}

	private void checkPermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			String permission = android.Manifest.permission.CAMERA;
			if (checkSelfPermission(permission) !=
					PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[]{permission}, REQUEST_CAMERA);
			}
		}
	}

	private void openCameraView() {
		cameraView.openAsync(CameraView.findCameraId(getFacing()));
	}

	private void closeCameraView() {
		cameraView.close();
	}

	private void invertCamera() {
		frontFacing ^= true;
		closeCameraView();
		openCameraView();
	}

	private int getFacing() {
		return frontFacing ?
				Camera.CameraInfo.CAMERA_FACING_FRONT :
				Camera.CameraInfo.CAMERA_FACING_BACK;
	}
}
