# Note 安卓记事本项目

这是一个基于 Android（Java）开发的本地记事本应用，支持用户注册、登录和个人笔记管理。  
应用数据使用 SQLite 本地存储，每位用户只能查看和管理自己的笔记内容。

## 功能说明

- 用户注册与登录
- 按用户隔离笔记数据
- 新建、编辑、删除笔记
- 笔记列表按创建时间倒序展示

## 技术栈

- Android SDK 30
- Java 8
- SQLite（`SQLiteOpenHelper`）
- AndroidX（AppCompat、Material、ConstraintLayout）

## 项目结构

- `app/src/main/java/com/example/myapplication/`
  - `LoginActivity`：登录页面
  - `RegisterActivity`：注册页面
  - `MainActivity`：笔记列表页面
  - `EditNoteActivity`：新增/编辑笔记页面
  - `DatabaseHelper`：SQLite 数据库操作
- `app/src/main/res/layout/`：各页面布局文件

## 运行方式

1. 使用 Android Studio 打开项目目录。
2. 等待 Gradle 同步完成。
3. 连接模拟器或真机后运行 `app` 模块。

## 构建与测试（命令行）

在项目根目录执行：

- 构建：`./gradlew assembleDebug`
- 单元测试：`./gradlew test`
- 仪器测试：`./gradlew connectedAndroidTest`