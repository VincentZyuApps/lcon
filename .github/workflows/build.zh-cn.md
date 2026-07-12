# 构建与发布工作流

> **[English](build.md)**
> **[简体中文](build.zh-cn.md)**

## 概述

`Build & Release` 工作流会在任意分支收到 push 时运行，也可以通过 `workflow_dispatch` 手动启动。是否继续构建或发布，由最新一条 commit 信息中的关键词决定，关键词匹配不区分大小写。

## Commit 关键词

| 最新 commit 信息包含 | 检查任务 | 构建 JAR | 上传 Artifact | GitHub Release |
|---|:---:|:---:|:---:|:---:|
| 没有可识别关键词 | 是 | 否 | 否 | 否 |
| `build action` | 是 | 是 | 是 | 否 |
| `build release` | 是 | 是 | 是 | 是 |
| 同时包含 `build action` 和 `build release` | 是 | 是 | 是 | 是 |
| 手动运行 `workflow_dispatch` | 是 | 是 | 是 | 否 |

关键词不区分大小写，因此 `Build Action` 和 `BUILD RELEASE` 同样有效。两个关键词同时存在时，`build release` 优先。

对于 push 事件，工作流只解析 `github.event.head_commit.message`，也就是这次 push 的最新一条 commit 信息。若关键词只存在于同一次 push 中更早的 commit，构建不会被触发。

## 使用示例

```bash
# 普通提交：只运行检查任务
git commit -m "docs: update configuration guide"

# 构建 JAR，并作为工作流 Artifact 上传
git commit -m "fix: use server player lifecycle events (build action)"

# 构建、上传 Artifact，并创建 GitHub Release
git commit -m "release: LCon 1.4.0 (build release)"

# 不修改文件，重新触发一次构建
git commit --allow-empty -m "ci: retry Forge build (build action)"
```

## 流水线

```text
push / workflow_dispatch
        |
        v
检查 commit 信息并读取版本号
        |
        +-- 无关键词 ---------> 检查结束后停止
        |
        +-- build action -----> 构建 JAR -> 上传 Artifact
        |
        +-- build release ----> 构建 JAR -> 上传 Artifact -> 创建 Release
```

构建任务使用 Temurin JDK 17，并运行以下命令：

```bash
./gradlew --no-daemon build
```

## 版本号与产物

工作流从 `gradle.properties` 读取以下配置：

| 配置项 | 示例 | 用途 |
|---|---|---|
| `minecraft_version` | `1.20.1` | 用于 Release 标签和产物名称中的 Minecraft 版本 |
| `mod_version` | `1.4.0` | 用于 Release 标签和产物名称中的 LCon 版本 |

使用以上示例时，工作流会生成：

```text
标签：v1.20.1-1.4.0
产物：lcon-v1.20.1-1.4.0.jar
```

版本号包含 `-alpha` 时，生成的发布说明会标记为 alpha 类型；包含 `-beta` 时会标记为 beta 类型；其他版本使用普通 release 类型。

## 发布注意事项

`build release` 会先删除相同版本号的现有 GitHub Release 和标签，再创建新的 Release。推送发布 commit 前，需要确认 `minecraft_version` 和 `mod_version` 正确。

工作流需要 `contents: write` 权限才能创建或替换 Release。普通的 `build action` 不会创建标签或 Release。
