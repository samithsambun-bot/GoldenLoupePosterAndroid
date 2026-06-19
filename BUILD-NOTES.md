# Android Build Notes

This folder is already structured as a native Android project.

Local build command from this folder:

```powershell
$env:JAVA_HOME='C:\Program Files\Huawei\DevEco Studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT="$env:LOCALAPPDATA\Android\Sdk"
.\gradle-8.7\bin\gradle.bat assembleDebug
```

The generated APK will be:

```text
app/build/outputs/apk/debug/app-debug.apk
```

On this machine, the build is currently blocked because Google's Android package hosts are not reachable:

- `dl.google.com` is needed for Android SDK platform/build-tools.
- Google's Maven repository is needed for `com.android.application` / Android Gradle Plugin `8.5.2`.

The included GitHub Actions workflow (`.github/workflows/android-build.yml`) builds the same debug APK on GitHub-hosted runners, where those Android repositories are normally available.
