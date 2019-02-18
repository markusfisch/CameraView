package de.markusfisch.android.cameraview.widget;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraView extends FrameLayout {
	public interface OnCameraListener {
		void onConfigureParameters(Camera.Parameters parameters);
		void onCameraError();
		void onCameraReady(Camera camera);
		void onPreviewStarted(Camera camera);
		void onCameraStopping(Camera camera);
	}

	public final Rect previewRect = new Rect();
	private boolean useOrientationListener = false;
	private Camera cam;
	private int viewWidth;
	private int viewHeight;
	private int frameWidth;
	private int frameHeight;
	private int frameOrientation;
	private OnCameraListener cameraListener;
	private OrientationEventListener orientationListener;
	private HandlerThread cameraCallbackThread;

	public static int findCameraId(int facing) {
		for (int i = 0, l = Camera.getNumberOfCameras(); i < l; ++i) {
			Camera.CameraInfo info = new Camera.CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == facing) {
				return i;
			}
		}
		return -1;
	}

	public static int getRelativeCameraOrientation(
			Context context,
			int cameraId) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int orientation = info.orientation;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			orientation -= 180;
		}
		return (orientation - getDeviceRotation(context) + 360) % 360;
	}

	public static int getDeviceRotation(Context context) {
		switch (((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay()
				.getRotation()) {
			case Surface.ROTATION_90:
				return 90;
			case Surface.ROTATION_180:
				return 180;
			case Surface.ROTATION_270:
				return 270;
			case Surface.ROTATION_0:
			default:
				return 0;
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public static boolean setAutoFocus(Camera.Parameters parameters) {
		// best for taking pictures, API >= ICE_CREAM_SANDWICH
		String continuousPicture =
				Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
		// less aggressive than CONTINUOUS_PICTURE, API >= GINGERBREAD
		String continuousVideo =
				Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
		// last resort
		String autoFocus = Camera.Parameters.FOCUS_MODE_AUTO;

		// prefer feature detection instead of checking BUILD.VERSION
		List<String> focusModes = parameters.getSupportedFocusModes();

		if (focusModes.contains(continuousPicture)) {
			parameters.setFocusMode(continuousPicture);
		} else if (focusModes.contains(continuousVideo)) {
			parameters.setFocusMode(continuousVideo);
		} else if (focusModes.contains(autoFocus)) {
			parameters.setFocusMode(autoFocus);
		} else {
			return false;
		}

		return true;
	}

	// overriding `View.performClick()` wouldn't make any sense here
	@SuppressLint("ClickableViewAccessibility")
	public void setTapToFocus() {
		final Runnable runnable = new Runnable() {
			@Override
			public void run() {
				setFocusArea(null);
			}
		};
		setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Camera camera = getCamera();
				if (camera != null && event.getActionMasked() ==
						MotionEvent.ACTION_UP) {
					camera.cancelAutoFocus();
					setFocusArea(calculateFocusRect(
							event.getX(),
							event.getY(),
							100));
					camera.autoFocus(new Camera.AutoFocusCallback() {
						@Override
						public void onAutoFocus(boolean success,
								Camera camera) {
							removeCallbacks(runnable);
							postDelayed(runnable, 3000);
						}
					});
					v.performClick();
				}
				return true;
			}
		});
	}

	public static Camera.Size findBestPreviewSize(
			List<Camera.Size> sizes,
			int width,
			int height) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) width / height;
		double minDiff = Double.MAX_VALUE;
		double minDiffAspect = Double.MAX_VALUE;
		Camera.Size bestSize = null;
		Camera.Size bestSizeAspect = null;

		for (Camera.Size size : sizes) {
			double diff = (double) Math.abs(size.height - height) +
					Math.abs(size.width - width);

			if (diff < minDiff) {
				bestSize = size;
				minDiff = diff;
			}

			double ratio = (double) size.width / size.height;

			if (Math.abs(ratio - targetRatio) < ASPECT_TOLERANCE &&
					diff < minDiffAspect) {
				bestSizeAspect = size;
				minDiffAspect = diff;
			}
		}

		return bestSizeAspect != null ? bestSizeAspect : bestSize;
	}

	public CameraView(Context context) {
		super(context);
	}

	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CameraView(
			Context context,
			AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void setUseOrientationListener(boolean use) {
		useOrientationListener = use;
	}

	public void openAsync(final int cameraId) {
		if (cameraCallbackThread != null) {
			return;
		}

		cameraCallbackThread = new HandlerThread(
				"CameraCallbackHandlerThread");
		cameraCallbackThread.start();
		Handler callbackThreadHandler = new Handler(
				cameraCallbackThread.getLooper());
		callbackThreadHandler.post(new Runnable() {
			@Override
			public void run() {
				try {
					// Use the cameraCallbackThread to open the camera
					// All camera callbacks will be invoked from this thread
					// See https://developer.android.com/reference/android/hardware/Camera
					cam = Camera.open(cameraId);
					final Context context = getContext();

					if (context == null) {
						// view is not resumed any more
						postClose();
						return;
					}

					if (useOrientationListener) {
						enableOrientationListener(context, cameraId);
					}
					frameOrientation = getRelativeCameraOrientation(
							context,
							cameraId);
					if (viewWidth > 0) {
						postAddPreview(context);
					}
				} catch (RuntimeException e) {
					if (cameraListener != null) {
						cameraListener.onCameraError();
					}
				}
			}
		});
	}

	private void postClose() {
		post(new Runnable() {
			@Override
			public void run() {
				close();
			}
		});
	}

	public void close() {
		if (cam != null) {
			cam.stopPreview();
			cam.setPreviewCallback(null);
			cam.release();
			cam = null;
		}
		if (orientationListener != null) {
			orientationListener.disable();
			orientationListener = null;
		}
		if (cameraCallbackThread != null) {
			// terminate cameraCallbackThread, discard all pending messages
			cameraCallbackThread.quit();
			try {
				cameraCallbackThread.join();
			} catch (InterruptedException ignore) {
			}
			cameraCallbackThread = null;
		}
		if (cam != null && cameraListener != null) {
			cameraListener.onCameraStopping(cam);
		}
		removeAllViews();
	}

	public void setOnCameraListener(OnCameraListener listener) {
		cameraListener = listener;
	}

	public Camera getCamera() {
		return cam;
	}

	public int getFrameWidth() {
		return frameWidth;
	}

	public int getFrameHeight() {
		return frameHeight;
	}

	public int getFrameOrientation() {
		return frameOrientation;
	}

	public Rect calculateFocusRect(float x, float y, int radius) {
		int cx = Math.round(2000f / viewWidth * x - 1000f);
		int cy = Math.round(2000f / viewHeight * y - 1000f);
		return new Rect(
				Math.max(-1000, cx - radius),
				Math.max(-1000, cy - radius),
				Math.min(1000, cx + radius),
				Math.min(1000, cy + radius));
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public boolean setFocusArea(Rect area) {
		if (cam == null || Build.VERSION.SDK_INT <
				Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return false;
		}
		try {
			Camera.Parameters parameters = cam.getParameters();
			if (parameters.getMaxNumFocusAreas() > 0) {
				if (area != null) {
					List<Camera.Area> focusAreas =
							new ArrayList<Camera.Area>();
					focusAreas.add(new Camera.Area(area, 1000));
					parameters.setFocusAreas(focusAreas);
					parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
				} else {
					parameters.setFocusAreas(null);
					CameraView.setAutoFocus(parameters);
				}
			}
			cam.setParameters(parameters);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	@Override
	protected void onLayout(
			boolean changed,
			int left,
			int top,
			int right,
			int bottom) {
		if (!changed) {
			return;
		}
		viewWidth = right - left;
		viewHeight = bottom - top;
		if (cam != null && getChildCount() == 0) {
			Context context = getContext();
			if (context == null) {
				return;
			}
			addPreview(context);
		}
	}

	private void enableOrientationListener(Context context,
			final int cameraId) {
		final Display defaultDisplay = ((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		final int defaultOrientation = defaultDisplay.getRotation();
		orientationListener = new OrientationEventListener(context,
				SensorManager.SENSOR_DELAY_NORMAL) {
			@Override
			public void onOrientationChanged(int orientation) {
				if (Math.abs(defaultOrientation -
						defaultDisplay.getRotation()) == 2) {
					close();
					openAsync(cameraId);
				}
			}
		};
		orientationListener.enable();
	}

	private void postAddPreview(final Context context) {
		post(new Runnable() {
			@Override
			public void run() {
				addPreview(context);
			}
		});
	}

	private void addPreview(Context context) {
		boolean transpose;
		try {
			transpose = setCameraParameters();
		} catch (RuntimeException e) {
			if (cameraListener != null) {
				cameraListener.onCameraError();
			}
			return;
		}
		int childWidth;
		int childHeight;
		if (transpose) {
			childWidth = frameHeight;
			childHeight = frameWidth;
		} else {
			childWidth = frameWidth;
			childHeight = frameHeight;
		}
		addSurfaceView(context, childWidth, childHeight);
		if (cameraListener != null) {
			cameraListener.onCameraReady(cam);
		}
	}

	private boolean setCameraParameters() throws RuntimeException {
		boolean transpose = frameOrientation == 90 || frameOrientation == 270;
		Camera.Parameters parameters = cam.getParameters();
		parameters.setRotation(frameOrientation);
		setPreviewSize(parameters, transpose);
		if (cameraListener != null) {
			cameraListener.onConfigureParameters(parameters);
		}
		Camera.Size size = parameters.getPreviewSize();
		if (size != null) {
			frameWidth = size.width;
			frameHeight = size.height;
		}
		cam.setParameters(parameters);
		cam.setDisplayOrientation(frameOrientation);
		return transpose;
	}

	private void setPreviewSize(
			Camera.Parameters parameters,
			boolean transpose) {
		if (transpose) {
			frameWidth = viewHeight;
			frameHeight = viewWidth;
		} else {
			frameWidth = viewWidth;
			frameHeight = viewHeight;
		}
		Camera.Size size = findBestPreviewSize(
				// will always return at least one item
				parameters.getSupportedPreviewSizes(),
				frameWidth,
				frameHeight);
		parameters.setPreviewSize(size.width, size.height);
	}

	private void addSurfaceView(
			Context context,
			int surfaceWidth,
			int surfaceHeight) {
		SurfaceView surfaceView = new SurfaceView(context);
		SurfaceHolder holder = surfaceView.getHolder();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		holder.setKeepScreenOn(true);
		holder.addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				// wait until the surface has dimensions
			}

			@Override
			public void surfaceChanged(
					SurfaceHolder holder,
					int format,
					int width,
					int height) {
				if (cam == null) {
					return;
				}
				try {
					cam.setPreviewDisplay(holder);
				} catch (IOException e) {
					return;
				}
				cam.startPreview();
				if (cameraListener != null) {
					cameraListener.onPreviewStarted(cam);
				}
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				close();
			}
		});
		addView(surfaceView);
		setChildLayout(
				viewWidth,
				viewHeight,
				surfaceView,
				surfaceWidth,
				surfaceHeight,
				previewRect);
	}

	private static void setChildLayout(
			int width,
			int height,
			View child,
			int childWidth,
			int childHeight,
			Rect childRect) {
		int widthByHeight = width * childHeight;
		int heightByWidth = height * childWidth;
		boolean dontScaleBeyondScreen = Build.VERSION.SDK_INT <
				Build.VERSION_CODES.ICE_CREAM_SANDWICH;

		if (dontScaleBeyondScreen ?
				// center within parent view
				widthByHeight > heightByWidth :
				// scale to cover parent view
				widthByHeight < heightByWidth) {
			childWidth = childWidth * height / childHeight;
			childHeight = height;
		} else {
			childHeight = childHeight * width / childWidth;
			childWidth = width;
		}

		int l = (width - childWidth) >> 1;
		int t = dontScaleBeyondScreen ?
				(height - childHeight) >> 1 :
				0;

		childRect.set(
				l,
				t,
				l + childWidth,
				t + childHeight);

		child.layout(
				childRect.left,
				childRect.top,
				childRect.right,
				childRect.bottom);
	}
}
