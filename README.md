# 小助手（AutoDing）

用于定时或远程打开钉钉，并通过邮件反馈运行状态、截图和识别到的打卡通知。远程指令通过极光推送传递，邮件由接收指令的手机直接发送。

仓库：<https://github.com/WXY3317/autoDingDing>

当前应用名称为 **小助手**，安装包名为 `xcom.abc.autoding`。源码命名空间仍为 `com.pengxh.autodingding`，与安装包名用途不同，无需一起修改。

> 打开钉钉不等于打卡成功。实际结果取决于钉钉登录状态、考勤规则、极速打卡设置、网络及手机运行状态，请以钉钉记录为准。

## 功能

| 功能 | 行为 |
| --- | --- |
| 定时打开 | 按应用内设置的时间打开钉钉 |
| 推送检查 | 唤醒目标手机、打开小助手并发送运行检查邮件 |
| 获取电量状态 | 发送电量、充电状态、机型等信息邮件，不主动亮屏 |
| 推送截屏 | 打开小助手，等待约 2 秒，截取小助手自身页面并通过邮件发送 |
| 推送打开钉钉 | 在目标手机打开钉钉 |
| 打卡通知监听 | 识别钉钉通知中包含“考勤打卡”的内容，记录并发送邮件 |



## 编译与安装

### 环境

当前项目使用：

- JDK 17
- Gradle Wrapper 8.6
- Android Gradle Plugin 8.4.2
- Kotlin 1.9.0
- Android SDK Platform 33、Build Tools 34.0.0
- 最低 Android 6.0（API 23），targetSdk 33

Linux x86_64 可使用环境安装脚本。需要联网下载组件，并预先具备 `bash`、`curl`、`tar`，以及 `bsdtar`、`unzip` 或 `python3` 中的一种解压工具。

```bash
git clone git@github.com:WXY3317/autoDingDing.git
cd autoDingDing
bash setup-build-env.sh
```

脚本默认安装目录：

```text
JDK:         ~/opt/android-build/jdk-17
Android SDK: ~/Android/Sdk
```

`setup-build-env.sh` 负责安装环境；`build.sh` 设置环境变量并调用项目的 Gradle Wrapper。可通过 `JAVA_HOME`、`ANDROID_HOME` 指定已有环境，安装脚本另支持 `BUILD_ROOT`。

### 配置极光推送

在极光后台创建 Android 应用，应用选择极光推送，登记包名 `xcom.abc.autoding`，然后在项目根目录创建本地配置：

```bash
cp jpush.local.properties.example jpush.local.properties
```

填写自己的凭据：

```properties
JPUSH_APPKEY=your_app_key
JPUSH_MASTER_SECRET=your_master_secret
```

构建也支持同名环境变量，优先于本地文件；缺少凭据时会报错。修改安装包名时，还需同步极光后台登记的包名和 Manifest 中的推送权限、category 等引用。

### 可选：预填邮箱

普通构建可不提供邮箱配置，安装后在应用内填写即可。私用构建可在项目根目录创建 `mail.local.properties`：

```properties
sender.email=your_account@163.com
sender.auth=your_smtp_authorization_code
receiver.email=your_receiver@example.com
```

省略 `receiver.email` 时，收件邮箱默认与发件邮箱相同。默认值会在启动时填充手机上尚未配置的字段；已有非空配置会保留。因此，更新本地授权码并覆盖安装后，若手机保存的是旧值，仍需在应用内替换并保存。

`jpush.local.properties`、`mail.local.properties`、`local.properties` 和 APK 构建产物均已加入 Git 忽略规则。不要把真实凭据写进源码、README 或示例配置。**这些凭据会进入相应 APK，Git 忽略不代表 APK 中的凭据不可提取，请勿公开分发带有私人授权码的安装包。**

### 打包

```bash
./build.sh                            # 默认只生成 Debug 包
./build.sh assembleRelease            # 生成 Release 包
./build.sh assembleDebug assembleRelease # 同时生成两个包
```

环境尚未安装时，也可使用 `./build.sh --install assembleRelease`。依赖已完整缓存后，可附加 `--offline` 离线构建。

| 类型 | 输出位置 |
| --- | --- |
| Debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Release | `app/build/outputs/apk/release/app-release.apk` |

**默认执行 `./build.sh` 不会更新 Release APK。** 安装前请确认文件类型和更新时间，避免把旧 Release 包当成新包安装。

构建完成后，`build.sh` 会打印 APK 的绝对路径、文件时间和 SHA-256。若任务全部显示 `UP-TO-DATE`，说明输入没有变化，Gradle 复用已有 APK，文件时间不会更新；版本号也不会自动递增，由 `app/build.gradle` 中的 `versionCode` 和 `versionName` 决定。

需要强制重新执行打包任务时，可使用：

```bash
./build.sh assembleRelease --rerun-tasks
```

签名配置位于 `app/build.gradle`。自行分发时应使用自己的签名；覆盖更新同一应用需要保持签名一致。


## 双手机使用

- **A 手机**：随身携带，用于发送指令。
- **B 手机**：安装钉钉，接收指令并执行操作。

1. A、B 安装同一套极光配置构建的小助手，并保持联网。
2. B 打开小助手，在“设置”页下方找到 Registration ID，点击复制。
3. A 进入“设置 → 发送推送”，填入 **B 的 Registration ID**。只粘贴一次，不能包含空格、换行或多个 ID。
4. B 先保持亮屏、打开小助手，A 点击“推送打开钉钉指令”，确认钉钉能打开。
5. 再分别测试 B 退到桌面、熄屏后的效果。

首次启动时，极光注册异步完成，Registration ID 可能暂时为空；等待联网注册后重新进入页面，必要时重启应用。卸载重装后 ID 可能变化，应重新核对。

“推送平台已接受指令”仅代表平台接受请求，不代表 B 已执行成功。

### B 手机设置

按手机实际提供的选项允许自启动、后台运行、后台弹出界面和锁屏显示，并检查电池优化限制。通知监听用于识别打卡通知；辅助操作需要对应无障碍权限。

应用可请求亮屏和解除非安全锁屏，但无法自动绕过密码、图案等身份验证。不同品牌和系统版本的后台限制不同，需在目标手机实测。

已由使用者验证：荣耀 90 Pro、MagicOS 9、无锁屏密码且允许相关后台权限时，可以接收指令并从熄屏状态打开钉钉。此结果不代表所有设备都能稳定执行。

使用钉钉极速打卡时，还需确保已登录、考勤时段及位置等条件满足要求。

## 邮箱通知

在 **B 的“设置 → 邮箱设置”** 中填写：

- 发件邮箱：自己的 163 邮箱完整地址。
- 授权码：该发件邮箱开启 SMTP 服务后生成的授权码，**不是邮箱登录密码**。
- 收件邮箱：接收通知的邮箱，可与发件邮箱相同，也可使用其他邮箱服务。

点击“保存”，再点击“发送测试邮件”，确认收件箱或垃圾箱中能收到测试邮件。

当前发件服务器固定为 `smtp.163.com`，端口 `465`，使用 SSL。B 手机直接通过 SMTP 发信；极光仅负责传递远程指令，GitHub 不参与邮件发送。

测试邮件、推送检查、电量查询、截屏附件和识别到的打卡通知均可能触发邮件。截图邮件当前主题为“打卡记录”，附件是小助手页面截图。打卡成功邮件依赖通知监听及代码能识别的钉钉通知，未收到邮件不能直接判定打卡失败。
