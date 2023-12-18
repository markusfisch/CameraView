# Change Log

## 1.10.0
* Add setScaleType to control camera preview layout

## 1.9.2
* Remove appcompat dependency

## 1.9.1
* Don't reset touch listener in focusTo()

## 1.9.0
* Add focusTo() to manually focus to some spot

## 1.8.4
* Catch possible RuntimeException on autoFocus()

## 1.8.3
* Terminate HandlerThread after closing camera

## 1.8.2
* Make CameraView.PreviewCallback.onPreviewFrame() run in a background thread
* Forward camera errors to OnCameraListener.onCameraError()

## 1.8.1
* Reactivate JitPack support

## 1.8.0
* Adds setTapToFocus() to enable tap to focus

## 1.7.2
* Expose findBestPreviewSize() to ease setting a custom preview size

## 1.7.1
* Allow setting a custom preview size in onConfigureParameters()

## 1.7.0
* Improve reliability of opening the camera
* Does only restart the camera for 180deg changes in orientation listener

## 1.6.0
* Add setUseOrientationListener() to fix landscape to landscape rotations

## 1.5.0
* Fix relative camera orientation for front facing cameras
* Rename OnCameraListener.onCameraStarted() to onCameraReady()

## 1.4.0
* Adds OnCameraListener.onPreviewStarted() callback

## 1.3.3
* Fixed Gradle configuration for JitPack again

## 1.3.2
* Fixed Gradle configuration for JitPack

## 1.3.1
* Fixed closing the camera immediately after openAsync()

## 1.3.0
* Helper functions to easily set a focus area (required for tap to focus)

## 1.2.1
* Fixes two formal issues static analyzers will complain about

## 1.2.0
* OnCameraListener.onCameraError() gets called when Camera.open() fails too

## 1.1.0
* CameraView.setAutoFocus() must be called manually now
* Handle camera errors in OnCameraListener.onCameraError()
