# Build & Release Workflow

> **[English](build.md)**
> **[简体中文](build.zh-cn.md)**

## Overview

The `Build & Release` workflow runs on every branch push and can also be started manually with `workflow_dispatch`. Build and release jobs are selected by case-insensitive keywords in the latest commit message.

## Commit Keywords

| Latest commit message contains | Check job | Build JAR | Upload artifact | GitHub Release |
|---|:---:|:---:|:---:|:---:|
| No recognized keyword | Yes | No | No | No |
| `build action` | Yes | Yes | Yes | No |
| `build release` | Yes | Yes | Yes | Yes |
| Both `build action` and `build release` | Yes | Yes | Yes | Yes |
| Manual `workflow_dispatch` | Yes | Yes | Yes | No |

Keyword matching is case-insensitive, so `Build Action` and `BUILD RELEASE` also work. If both keywords are present, `build release` takes precedence.

For push events, only `github.event.head_commit.message` is parsed. A keyword in an earlier commit from the same push does not trigger a build unless the latest commit also contains it.

## Examples

```bash
# Regular commit: check job only
git commit -m "docs: update configuration guide"

# Build and upload the JAR as a workflow artifact
git commit -m "fix: use server player lifecycle events (build action)"

# Build, upload the artifact, and create a GitHub Release
git commit -m "release: LCon 1.4.0 (build release)"

# Re-run a build without changing files
git commit --allow-empty -m "ci: retry Forge build (build action)"
```

## Pipeline

```text
push / workflow_dispatch
        |
        v
check commit message and read version
        |
        +-- no keyword ------> stop after check
        |
        +-- build action ----> build JAR -> upload artifact
        |
        +-- build release ---> build JAR -> upload artifact -> create release
```

The build job uses Temurin JDK 17 and runs:

```bash
./gradlew --no-daemon build
```

## Version And Artifacts

The workflow reads these values from `gradle.properties`:

| Property | Example | Purpose |
|---|---|---|
| `minecraft_version` | `1.20.1` | Minecraft version in the release tag and artifact name |
| `mod_version` | `1.4.0` | LCon version in the release tag and artifact name |

With the examples above, the workflow produces:

```text
Tag:      v1.20.1-1.4.0
Artifact: lcon-v1.20.1-1.4.0.jar
```

Versions containing `-alpha` create an alpha prerelease type, versions containing `-beta` create a beta prerelease type, and all other versions use the normal release type in generated release notes.

## Release Warning

`build release` deletes any existing GitHub Release and tag with the same generated version before creating the new Release. Confirm `minecraft_version` and `mod_version` before pushing a release commit.

The workflow needs `contents: write` permission to create or replace releases. Regular `build action` runs do not create tags or releases.
