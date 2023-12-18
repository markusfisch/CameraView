# CameraView

Camera view for Android. Supports orientation changes, fits preview image
into available view space and works with Gingerbread (minSDK 9) or better
(since it still uses the deprecated Camera API). All in just ~500 lines of
code.

## Why the deprecated Camera API?

This library is deliberately still on API level 9 (Android 2.3).

If you're not interested in supporting old versions of Android and/or
don't want to use the deprecated Camera API on newer devices, have a look at
the [CameraX support library](https://developer.android.com/training/camerax)
(available from API level 21).

There, we finally find
[PreviewView](https://developer.android.com/reference/androidx/camera/view/PreviewView)
as part of the SDK. You can read
[here](https://developer.android.com/training/camerax/preview)
how to implement it.

## How to include

### Gradle

Add the JitPack repository in your root build.gradle at the end of
repositories:

```groovy
allprojects {
	repositories {
		…
		maven { url 'https://jitpack.io' }
	}
}
```

Then add the dependency in your app/build.gradle:

```groovy
dependencies {
	implementation 'com.github.markusfisch:CameraView:1.10.0'
}
```

### Manually

Alternatively you may just download the latest `aar` from
[Releases](https://github.com/markusfisch/CameraView/releases) and put it
into `app/libs` in your app.

Then make sure your `app/build.gradle` contains the following line in the
`dependencies` block:

```groovy
dependencies {
	implementation fileTree(dir: 'libs', include: '*')
	…
}
```

## How to use

Add it to a layout:

```xml
<de.markusfisch.android.cameraview.widget.CameraView
	xmlns:android="http://schemas.android.com/apk/res/android"
	android:id="@+id/camera_view"
	android:layout_width="match_parent"
	android:layout_height="match_parent"/>
```

Or create it in Java:

```java
import de.markusfisch.android.cameraview.widget.CameraView;

CameraView cameraView = new CameraView(context);
```

If your app supports *orientation changes*, please also enable the built-in
orientation listener:

```java
cameraView.setUseOrientationListener(true);
```

This will take care of landscape to reverse landscape rotations (and vice
versa). Without this, Android will re-use the Activity without calling a
life cycle method what will result in an upside down camera preview.

Run `CameraView.openAsync()`/`.close()` in `onResume()`/`onPause()` of
your activity or fragment:

```java
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
```

To set custom camera parameters or a preview listener to get the camera
frame, set an OnCameraListener:

```java
cameraView.setOnCameraListener(new OnCameraListener {
	@Override
	public void onConfigureParameters(Camera.Parameters parameters) {
		// set additional camera parameters here
	}

	@Override
	public void onCameraError() {
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
```

## Preview resolution

By default CameraView picks the camera preview resolution that is closest
to the size of the view on screen. If you want a lower or higher resolution,
you may use `CameraView.findBestPreviewSize()` (or a customized copy of it)
in `OnCameraListener.onConfigureParameters()` to pick another size.

For example, if you want to pick the highest possible resolution, you can
do this:

```java
cameraView.setOnCameraListener(new OnCameraListener {
	@Override
	public void onConfigureParameters(Camera.Parameters parameters) {
		Camera.Size size = findBestPreviewSize(
				parameters.getSupportedPreviewSizes(),
				cameraView.getFrameWidth() * 1000,
				cameraView.getFrameHeight() * 1000);
		parameters.setPreviewSize(size.width, size.height);
		…
	}
	…
```

`CameraView.findBestPreviewSize()` returns the preview resolution that has
the smallest absolute distance to the given dimensions *and* is as close to
the aspect ratio of those dimensions as possible.

## Preview layout

On some devices, the camera frame has a different aspect ratio than the
screen. The camera preview can either be laid so that it completely covers
the available `View` area (which is `SCALE_TYPE_CENTER_CROP`, the default),
or so that it lies completely within it (`SCALE_TYPE_CENTER_INSIDE`).

Use `CameraView.setScaleType()` with the appropriate constant:

```java
cameraView.setScaleType(CameraView.SCALE_TYPE_CENTER_INSIDE);
```

## Auto Focus

To enable Auto Focus, you should run `CameraView.setAutoFocus()` in
`OnCameraListener.onConfigureParameters()`:

```java
cameraView.setOnCameraListener(new OnCameraListener {
	@Override
	public void onConfigureParameters(Camera.Parameters parameters) {
		CameraView.setAutoFocus(parameters);
		…
	}
	…
```

`CameraView.setAutoFocus()` picks the best available Auto Focus mode for
making pictures. If you want something else, just have a look at this
method and re-implement it in the client to fit your needs.

Note that Auto Focus is not available on all devices. If your app depends
on Auto Focus, you should put a `<uses-feature/>` tag in your
`AndroidManifest.xml` to make Google Play restrict your app to devices
that sport this feature:

```xml
<uses-feature android:name="android.hardware.camera.autofocus"/>
```

## Tap to focus

To make the CameraView focus where a user taps, simply call `setTapToFocus()`
on your `cameraView` instance:

```java
cameraView.setTapToFocus();
```

This adds a `View.OnTouchListener` to the `CameraView` to process the tap.

If you want to use a custom `View.OnTouchListener`, you can call `focusTo()`
manually in your touch listener instead of using `setTapToFocus()`:

```java
if (event.getActionMasked() == MotionEvent.ACTION_UP) {
	boolean success = focusTo(cameraView, event.getX(), event.getY());
	…
}
```

If `focusTo()` returns `false`, you should stop calling it because that
means there was a `RuntimeException` that will be thrown (and catched) in
the future too.

## Scene modes

You may use a predefined scene mode to use optimized camera parameters for
a specific purpose. Consequently, setting a scene mode may override previously
set camera parameters, of course.

For example, to use `SCENE_MODE_BARCODE` (if it's available) do:

```java
cameraView.setOnCameraListener(new OnCameraListener {
	@Override
	public void onConfigureParameters(Camera.Parameters parameters) {
		List<String> modes = parameters.getSupportedSceneModes();
		if (modes != null) {
			for (String mode : modes) {
				if (Camera.Parameters.SCENE_MODE_BARCODE.equals(mode)) {
					parameters.setSceneMode(mode);
					break;
				}
			}
		}
		…
	}
	…
```

Please note, not all devices support scene modes.

## Demo

You can run the enclosed demo app to see if this widget is what you want.
Either import it into Android Studio or, if you're not on that thing from
Redmond, just type `make` to build, install and run.

Tap on the screen to switch between the front and back camera.

## License

This widget is so basic, it should be Public Domain. And it is.
