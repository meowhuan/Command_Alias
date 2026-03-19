#!/usr/bin/env sh

APP_HOME=$(cd "${APP_HOME:-.}" && pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar:$APP_HOME/gradle/wrapper/gradle-wrapper-shared.jar:$APP_HOME/gradle/wrapper/gradle-cli.jar"

if [ -n "$JAVA_HOME" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  JAVA_CMD=java
fi

exec "$JAVA_CMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
