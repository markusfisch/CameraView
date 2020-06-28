PACKAGE = de.markusfisch.android.cameraviewdemo

all: debug install start

debug:
	./gradlew assembleDebug

lint:
	./gradlew lintDebug

infer: clean
	infer -- ./gradlew assembleDebug

release: lint
	./gradlew assembleRelease

aar: clean
	./gradlew :cameraview:assembleRelease

install:
	adb $(TARGET) install -r app/build/outputs/apk/debug/app-debug.apk

start:
	adb $(TARGET) shell 'am start -n $(PACKAGE)/.activity.MainActivity'

uninstall:
	adb $(TARGET) uninstall $(PACKAGE)

clean:
	./gradlew clean
