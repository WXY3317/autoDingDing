#!/usr/bin/env bash
#
# 免 root 的 Android 编译环境安装脚本（全部安装在用户目录下）
#   - Temurin JDK 17      -> $HOME/opt/android-build/jdk-17
#   - Android SDK (cmdline-tools + platform-tools/platforms/build-tools)
#                          -> $HOME/Android/Sdk
# 幂等：已存在的组件会跳过下载/解压，可安全重复执行。
#
# 用法：  bash setup-build-env.sh
#
set -euo pipefail

BUILD_ROOT="${BUILD_ROOT:-$HOME/opt/android-build}"
DL_DIR="$BUILD_ROOT/dl"
JDK_DIR="$BUILD_ROOT/jdk-17"
SDK_ROOT="${ANDROID_HOME:-$HOME/Android/Sdk}"
JDK_MAJOR=17
CMDLINE_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
JDK_URL="https://api.adoptium.net/v3/binary/latest/${JDK_MAJOR}/ga/linux/x64/jdk/hotspot/normal/eclipse"

mkdir -p "$DL_DIR" "$SDK_ROOT/cmdline-tools"

# ---------- 1. JDK 17 ----------
if [ ! -x "$JDK_DIR/bin/java" ]; then
  if [ ! -s "$DL_DIR/jdk17.tar.gz" ]; then
    echo ">> 下载 Temurin JDK ${JDK_MAJOR} ..."
    curl -fL --retry 3 -o "$DL_DIR/jdk17.tar.gz" "$JDK_URL"
  else
    echo ">> 复用已下载的 JDK 压缩包：$DL_DIR/jdk17.tar.gz"
  fi
  echo ">> 解压 JDK ..."
  rm -rf "$BUILD_ROOT/_jdk_tmp" && mkdir -p "$BUILD_ROOT/_jdk_tmp"
  tar -xzf "$DL_DIR/jdk17.tar.gz" -C "$BUILD_ROOT/_jdk_tmp"
  rm -rf "$JDK_DIR"
  mv "$BUILD_ROOT"/_jdk_tmp/jdk-* "$JDK_DIR"
  rmdir "$BUILD_ROOT/_jdk_tmp" 2>/dev/null || true
else
  echo ">> JDK 已存在，跳过：$JDK_DIR"
fi
export JAVA_HOME="$JDK_DIR"
export PATH="$JAVA_HOME/bin:$PATH"
echo ">> java 版本： $("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"

# ---------- 2. Android command-line tools ----------
CMDLINE_BIN="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$CMDLINE_BIN" ]; then
  if [ ! -s "$DL_DIR/cmdtools.zip" ]; then
    echo ">> 下载 Android 命令行工具 ..."
    curl -fL --retry 3 -o "$DL_DIR/cmdtools.zip" "$CMDLINE_URL"
  else
    echo ">> 复用已下载的命令行工具：$DL_DIR/cmdtools.zip"
  fi
  echo ">> 解压命令行工具 ..."
  rm -rf "$SDK_ROOT/cmdline-tools/_tmp" && mkdir -p "$SDK_ROOT/cmdline-tools/_tmp"
  if command -v bsdtar >/dev/null 2>&1; then
    bsdtar -xf "$DL_DIR/cmdtools.zip" -C "$SDK_ROOT/cmdline-tools/_tmp"
  elif command -v unzip >/dev/null 2>&1; then
    unzip -q "$DL_DIR/cmdtools.zip" -d "$SDK_ROOT/cmdline-tools/_tmp"
  else
    python3 -m zipfile -e "$DL_DIR/cmdtools.zip" "$SDK_ROOT/cmdline-tools/_tmp"
  fi
  rm -rf "$SDK_ROOT/cmdline-tools/latest"
  mv "$SDK_ROOT/cmdline-tools/_tmp/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
  rmdir "$SDK_ROOT/cmdline-tools/_tmp" 2>/dev/null || true
else
  echo ">> cmdline-tools 已存在，跳过"
fi
export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

# ---------- 3. 安装 SDK 组件 ----------
echo ">> 接受 SDK license ..."
yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses >/dev/null 2>&1 || true

echo ">> 安装 platform-tools / platforms;android-33 / build-tools;34.0.0 ..."
sdkmanager --sdk_root="$ANDROID_HOME" \
  "platform-tools" \
  "platforms;android-33" \
  "build-tools;34.0.0"

echo ">> 再次接受 license 以确保就绪 ..."
yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses >/dev/null 2>&1 || true

echo ""
echo ">> 环境安装完成："
echo "   JAVA_HOME    = $JAVA_HOME"
echo "   ANDROID_HOME = $ANDROID_HOME"
echo ">> 现在可以直接执行： ./build.sh [assembleDebug|assembleRelease]"
