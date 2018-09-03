# Change Log

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
