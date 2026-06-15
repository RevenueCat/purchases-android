FROM cimg/android:2025.04.1

# The base image exposes the SDK via $ANDROID_HOME but only adds the tools to
# PATH through its login profile, which isn't sourced in non-login agent shells.
# Add them explicitly so sdkmanager/adb are directly callable in agent terminals.
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"
