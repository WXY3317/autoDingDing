#!/usr/bin/env bash
#
# AutoDing 编译脚本
# 自动装配 JDK / Android SDK 环境变量，并调用 Gradle Wrapper 打包。
#
# 用法：
#   ./build.sh                 # 默认打 Debug 包 (assembleDebug)
#   ./build.sh assembleRelease # 打 Release 包（使用 app/build.gradle 中的签名配置）
#   ./build.sh clean assembleDebug
#   ./build.sh --install       # 若 SDK 缺失，先自动执行 setup-build-env.sh 再编译
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 允许通过环境变量覆盖，默认使用 setup-build-env.sh 的安装位置
export JAVA_HOME="${JAVA_HOME:-$HOME/opt/android-build/jdk-17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
# 将 build-tools 加入 PATH，方便使用 apksigner / aapt / zipalign 等工具
for _bt in "$ANDROID_HOME"/build-tools/*/; do
  if [ -d "$_bt" ]; then
    export PATH="${_bt%/}:$PATH"
  fi
done

# 可选：--install 时先安装缺失的编译环境
if [ "${1:-}" = "--install" ]; then
  shift
  bash "$SCRIPT_DIR/setup-build-env.sh"
fi

# 环境自检
need_setup=0
[ -x "$JAVA_HOME/bin/java" ] || { echo "!! 未找到 JDK：$JAVA_HOME"; need_setup=1; }
[ -d "$ANDROID_HOME/platforms" ] || { echo "!! 未找到 Android SDK：$ANDROID_HOME"; need_setup=1; }
if [ "$need_setup" -eq 1 ]; then
  echo ">> 请先执行： ./build.sh --install   （或 bash setup-build-env.sh）"
  exit 1
fi

# 生成/更新 local.properties 指向 SDK
echo "sdk.dir=$ANDROID_HOME" > local.properties

# 默认任务
if [ "$#" -eq 0 ]; then
  set -- assembleDebug
fi

echo "==================================================================="
echo " JAVA_HOME    = $JAVA_HOME"
echo " ANDROID_HOME = $ANDROID_HOME"
echo " java         = $("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"
echo " task(s)      = $*"
echo "==================================================================="

chmod +x ./gradlew 2>/dev/null || true
./gradlew "$@" --no-daemon

# Gradle 在输入未变化时复用 APK；打印产物信息，避免误拿其他目录的旧包。
show_debug=0
show_release=0
for build_task in "$@"; do
  case "$build_task" in
    assembleDebug|:app:assembleDebug) show_debug=1 ;;
    assembleRelease|:app:assembleRelease) show_release=1 ;;
    assemble|:app:assemble|build|:app:build) show_debug=1; show_release=1 ;;
  esac
done
for build_variant in debug release; do
  if { [ "$build_variant" = debug ] && [ "$show_debug" -eq 1 ]; } ||
     { [ "$build_variant" = release ] && [ "$show_release" -eq 1 ]; }; then
    apk_path="$SCRIPT_DIR/app/build/outputs/apk/$build_variant/app-$build_variant.apk"
    if [ -f "$apk_path" ]; then
      echo ">> APK: $apk_path"
      ls -lh --time-style=long-iso "$apk_path"
      sha256sum "$apk_path"
      echo ">> 若任务显示 UP-TO-DATE，表示复用已有产物；版本号以 app/build.gradle 为准。"
    fi
  fi
done
