# CameraView

Camera view for Android. Supports orientation changes, fits preview image
into available view space and works with Gingerbread (minSDK 9) or better
(since it still uses the deprecated Camera API).

## How to include

### Android Archive

Just download the latest `aar` from
[Releases](https://github.com/markusfisch/CameraView/releases) and put it
into `app/libs` in your app.

Then make sure your `app/build.gradle` contains the following line in the
`dependencies` block:

	dependencies {
		implementation fileTree(dir: 'libs', include: '*')
		...
	}

## How to use

Add it to a layout:

	<de.markusfisch.android.cameraview.widget.CameraView
		xmlns:android="http://schemas.android.com/apk/res/android"
		android:id="@+id/camera_view"
		android:layout_width="match_parent"
		android:layout_height="match_parent"/>

Or create it in java:

	import de.markusfisch.android.cameraview.widget.CameraView;

	CameraView cameraView = new CameraView(context);

If your app supports *orientation changes*, please also enable the built-in
orientation listener:

	cameraView.setUseOrientationListener(true);

This will take care of landscape to reverse landscape rotations (and vice
versa). Without this, Android will re-use the Activity without calling a
life cycle method what will result in an upside down camera preview.

Run `CameraView.openAsync()`/`.close()` in `onResume()`/`onPause()` of
your activity or fragment:

	@Override
	public void onResume() {
		super.onResume();
		cameraView.openAsync(CameraView.findCameraId(
				Camera.CameraInfo.CAMERA_FACING_BACK));
	}

	@Override
	public void onPause() {
		super.onPause();
		cameraView.close();
	}

To set custom camera parameters or a preview listener to get the camera
frame, set an OnCameraListener:

	cameraView.setOnCameraListener(new OnCameraListener {
		@Override
		public void onConfigureParameters(Camera.Parameters parameters) {
			// set additional camera parameters here
		}

		@Override
		public void onCameraError(Camera camera) {
			// handle camera errors
		}

		@Override
		public void onCameraReady(Camera camera) {
			// set a preview listener
		}

		@Override
		public void onPreviewStarted(Camera camera) {
			// start processing camera data
		}

		@Override
		public void onCameraStopping(Camera camera) {
			// clean up
		}
	});

## Demo

This is a demo app you may use to see and try if this widget is what
you're searching for. Either import it into Android Studio or, if you're
not on that thing from Redmond, just type make to build, install and run.

## License

This widget is so basic, it should be Public Domain. And it is.
