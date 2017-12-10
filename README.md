# CameraView

[![](https://jitpack.io/v/markusfisch/CameraView.svg)](https://jitpack.io/#markusfisch/CameraView)

Camera view for Android. Supports orientation changes, fits preview image
into available view space and works with Gingerbread (minSDK 9) or better
(since it still uses the deprecated Camera API).

How to include
--------------

### Gradle

Add the JitPack repository in your root build.gradle at the end of
repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

Then add the dependency in your app/build.gradle:

	dependencies {
		compile 'com.github.markusfisch:CameraView:1.3.3'
	}

### As subproject

If you prefer your project to be self-reliant and completely modifiable,
just copy the `cameraview` folder into your project root and add it as a
subproject to `settings.gradle`:

	include ':app', ':cameraview'

And to the dependencies block of your `app/build.gradle`:

	dependencies {
		compile project(':cameraview')
	}

Then remove the Android Maven plug-in from `cameraview/build.gradle`:

	apply plugin: 'com.github.dcendents.android-maven'

Because `cameraview/build.gradle` uses variables to manage version
numbers of Android's dependencies, you need to define them in you root
`build.gradle` (or replace the variables with literals):

	buildscript {
		ext.tools_version = '2.3.3'
		ext.support_version = '25.3.1'
		...
	}

How to use
----------

Add it to a layout:

	<de.markusfisch.android.cameraview.widget.CameraView
		xmlns:android="http://schemas.android.com/apk/res/android"
		android:id="@+id/camera_view"
		android:layout_width="match_parent"
		android:layout_height="match_parent"/>

Or create it in java:

	import de.markusfisch.android.cameraview.widget.CameraView;

	CameraView cameraView = new CameraView(context);

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
		public void onCameraStarted(Camera camera) {
			// set a preview listener
		}

		@Override
		public void onCameraStopping(Camera camera) {
			// clean up
		}
	});

Demo
----

This is a demo app you may use to see and try if this widget is what
you're searching for. Either import it into Android Studio or, if you're
not on that thing from Redmond, just type make to build, install and run.

License
-------

This widget is so basic, it should be Public Domain. And it is.
