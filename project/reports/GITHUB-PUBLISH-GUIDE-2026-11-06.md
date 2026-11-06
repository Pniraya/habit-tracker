# 将 Habit Tracker 上传到 GitHub：操作指南

> 日期：2026-11-06 · 适用项目：本地 Android 项目（`D:\AndroidStudio`，`main` @ `6876de8`），Windows + PowerShell
> 前提：你已有 GitHub 账号。
> **说明**：我没有看到你的仓库内容，凡涉及「你的仓库里有没有某个文件」的地方都标了「需确认」，请用文中的命令自己核实。文末的链接来自我的记忆，未在本次对话中联网核验，打开时请以 GitHub Docs 当前页面为准。

---

## 0. 先回答你的两个问题

**Q：项目介绍需要提供什么信息？**
GitHub 没有强制要求，只要仓库根目录有 `README.md` 就会自动渲染在仓库首页。但社区有一套通行的结构：**一句话简介 → 截图 → 功能 → 技术栈 → 如何构建运行 → 项目结构 → 许可证**。本项目建议再加上**功能状态（已实现 / 计划中）**和**隐私声明**。完整清单与模板见第 5 节。

**Q：是否存在规范？**
没有强制规范，但有三类「事实标准」：
1. **GitHub 社区标准清单**：仓库页 `Insights → Community Standards` 会检查你是否具备 README、LICENSE、CODE_OF_CONDUCT、CONTRIBUTING、SECURITY、Issue/PR 模板。这是 GitHub 官方的「健康度」参考。
2. **README 约定**：`standard-readme`（社区规范）与 `makeareadme.com`（入门指南）。
3. **配套约定**：`Keep a Changelog`（变更日志）、`Semantic Versioning`（版本号）、`Conventional Commits`（提交信息）。都是可选，按需采用。

---

## 1. 总流程一览

| 阶段 | 内容 | 必要性 | 工作量 |
|---|---|---|---|
| **A** | 上传前清点：`.gitignore`、敏感信息、邮箱、大文件、许可证、可复现性 | **必做** | 中 |
| **B** | 创建远程仓库并推送 | 必做 | 小 |
| **C** | 推送后设置：About、主题标签、分支保护、安全选项 | 建议 | 小 |
| **D** | README 与社区文件 | **必做（README）**，其余建议 | 中 |
| **E** | 可选增强：CI、Releases、签名 | 可选 | 中 |

**强烈建议的节奏：先建「私有仓库」推送并检查，确认无误后再改为公开。**

---

## 2. 阶段 A：上传前清点（必做）

### A1 决定可见范围

| 方案 | 适用 | 注意 |
|---|---|---|
| **先私有，检查后转公开（推荐）** | 绝大多数情况 | 免费账号可建无限私有仓库；转公开在 `Settings → General → Danger Zone → Change visibility` |
| 直接公开 | 已确认无敏感内容 | **一旦公开，请假定内容（含历史）已被他人抓取**。密钥泄露只能吊销，不能靠删除补救 |

### A2 `.gitignore` 与 `.gitattributes`

先确认现有文件（需确认）：
```powershell
cd D:\AndroidStudio
git status
Get-Content .gitignore
git ls-files | Select-String -Pattern 'local\.properties|\.keystore|\.jks|\.idea/|/build/|\.apk$|\.aab$'
```
最后一行如果有输出，说明这些文件**已经被跟踪**，光改 `.gitignore` 没用，需要：
```powershell
git rm -r --cached <路径>
```

**建议的 `.gitignore` 要点（Android 通用，可参考 `github/gitignore` 的 `Android.gitignore`）：**
```gitignore
# Gradle / 构建产物
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# 本机配置与密钥（必须忽略）
local.properties
*.keystore
*.jks
keystore.properties
secrets.properties

# IDE
.idea/
*.iml
captures/
.externalNativeBuild/
.cxx/

# 发布产物走 GitHub Releases，不进仓库
*.apk
*.aab

# AI 工具的本机配置（如存在，需确认）
.claude/settings.local.json
```

**两个 Android 专属的坑：**
- `gradle/wrapper/gradle-wrapper.jar` **必须提交**。别写 `*.jar` 这种过宽规则，否则别人 clone 后 `gradlew` 无法运行。
- **Room 导出的 schema JSON（通常在 `app/schemas/`）必须提交**，它是迁移测试的依据。不要被 `build/` 之类的规则误伤。

**`.gitattributes`（Windows 开发者强烈建议加）：**
```gitattributes
* text=auto
gradlew text eol=lf
*.sh text eol=lf
*.bat text eol=crlf
*.png binary
*.jar binary
```
为什么：`gradlew` 若以 CRLF 提交，在 Linux/macOS/CI 上会报 `bad interpreter`。再补一步，让 Linux 能执行它：
```powershell
git update-index --chmod=+x gradlew
```
添加 `.gitattributes` 之后，已提交文件可能出现换行符差异。若出现，请用 `git add --renormalize .` 单独做一次提交。

### A3 敏感信息与个人信息扫描

**当前文件：**
```powershell
git grep -nIE "(api[_-]?key|secret|token|passw(or)?d|storePassword|keyPassword|sk-|ghp_)" 
```

**历史记录（比当前文件更重要，因为推送的是完整历史）：**
```powershell
# 历史中是否出现过密钥/本机配置类文件
git log --all --name-only --pretty=format: | Sort-Object -Unique | Select-String -Pattern 'keystore|\.jks|local\.properties|\.env|secrets'

# 历史中是否出现过疑似密钥字符串
git log --all -p -S"password" --oneline
```
也可以用专门工具扫描（任选其一，需自行安装）：`gitleaks detect`、`trufflehog git file://.`。

**逐文件人工检查（需确认）：**

| 文件 | 检查什么 |
|---|---|
| `gradle.properties` | 是否有代理、镜像账号、签名口令、本机路径 |
| `settings.gradle.kts` / `build.gradle.kts` | 腾讯 Maven 镜像可以公开，但对海外用户和 GitHub CI 可能不友好，建议同时保留 `google()` / `mavenCentral()` 作为回退 |
| `local.properties` | 含 `sdk.dir`，必须忽略 |
| `app/src/main/AndroidManifest.xml` | `allowBackup`、权限声明（README 的隐私声明要与此一致） |
| 截图、录屏 | 是否露出真实习惯名称、通知、系统时间等个人信息 |

**如果历史里已经出现过真实密钥：** 先在对应服务**吊销该密钥**，再决定是否重写历史。仅仅从历史里抹掉并不能让已泄露的密钥重新安全。

### A4 提交者邮箱（常被忽略）

Git 提交会永久记录作者邮箱，推送到公开仓库后任何人都能看到。

```powershell
# 看历史里出现过哪些作者邮箱
git log --all --format="%an <%ae>" | Sort-Object -Unique
git config user.email
```

**建议：**
1. 在 GitHub `Settings → Emails` 勾选 **Keep my email addresses private**，并勾选 **Block command line pushes that expose my email**。
2. 本地使用 GitHub 提供的 noreply 邮箱（格式 `<数字ID>+<用户名>@users.noreply.github.com`，ID 在该设置页可见）：
```powershell
git config user.name  "<你的显示名>"
git config user.email "<ID>+<用户名>@users.noreply.github.com"
```
3. 若**旧提交**已经带了真实邮箱，有三种处理：
   - 接受并照常推送；
   - 用 `git filter-repo --mailmap` 重写历史；
   - 丢弃历史，以一次全新的初始提交推送。

   **注意：** 后两种会改变所有提交哈希，而你的文档里引用了 `6876de8` 这类哈希，重写后这些引用会失效。请权衡。

### A5 大文件与仓库体积

GitHub 对单个文件超过 50 MB 会警告，超过 100 MB 会直接拒绝推送；仓库整体建议保持在 1 GB 以内。

```powershell
git count-objects -vH
git ls-files | ForEach-Object { Get-Item -LiteralPath $_ } | Sort-Object Length -Descending | Select-Object -First 10 FullName, Length
```
如果有 APK、设计稿大图、录屏，建议移出仓库，改放 Releases 或外部链接。

### A6 哪些文件要不要公开

你的仓库带有一整套工程治理文档（`project/` 下的需求、ADR、评审、任务）。这些是项目的**亮点**，但需要逐项决定（需确认）：

| 类型 | 建议 |
|---|---|
| `REQUIREMENTS.md`、`DATABASE.md`、ADR、任务文件 | 建议公开，体现工程化。检查其中是否有本机路径（如 `D:\AndroidToolchain`）、设备型号、环境细节 |
| `project/reviews/` 评审记录 | 建议公开 |
| `ADVICE-REQUEST-*.md`、`ADVICE-RESPONSE-*.md` | 属于个人协作过程文档，其中含本机路径和设备信息。可公开、可脱敏后公开、可放入 `.gitignore`，由你决定 |
| `CLAUDE.md` / `AGENTS.md` / `.claude/` | 如果存在，`.claude/settings.local.json` 这类本机配置应忽略；`CLAUDE.md` 等项目说明是否公开由你决定（需确认） |
| 记录「AI 代写 SDK 许可文件」的内容 | 这件事本身建议先由你本人复核（见我之前回复的 B8）。许可文件属于 SDK 安装目录，不要提交进仓库 |

### A7 可复现性：别人 clone 之后能构建吗？

你的验证脚本目前带有本机假设（需确认）：`. D:\AndroidToolchain\env.ps1`、沙箱里 Gradle 测试 worker 被禁用而改用 `tools/run-unit-tests.ps1`。公开之后，别人会在**正常环境**里 clone，因此：

- README 的「构建」一节要写**通用做法**：`./gradlew assembleDebug`、`./gradlew testDebugUnitTest`（正常环境下可用）。
- 本机特有的脚本（`env.ps1`、绕过 worker 的跑测方式）单独放在「开发环境说明」一节，并写明它们是**作者本机的特殊环境**，不是通用要求。
- 把硬编码路径（如 `D:\...`）改成环境变量或相对路径，或至少在文档中明确标注。
- 在**干净目录**里验证一遍：
```powershell
cd $env:TEMP
git clone <你的仓库地址> verify-clone
cd verify-clone
./gradlew assembleDebug
```
能构建通过，才说明别人也能。

### A8 选择许可证（需要你决定）

**没有许可证 = 默认保留所有权利**：别人可以在 GitHub 上看和 fork，但没有合法复用、修改、再分发的权利。如果想让别人使用你的代码，必须选一个许可证。

| 许可证 | 特点 | 适合 |
|---|---|---|
| **MIT** | 最宽松，只要求保留版权声明 | 个人作品集、希望被随意使用 |
| **Apache-2.0** | 宽松，并含明确的专利授权，Android 生态常用 | 同上，更重视专利条款 |
| **GPL-3.0** | 衍生作品必须同样开源 | 不希望被闭源商用 |
| AGPL-3.0 | 在 GPL 基础上覆盖网络服务场景 | 本项目（单机 App）通常不必要 |
| 不选 | 所有权利保留 | 只想展示、不欢迎复用 |

选定后：仓库根目录放 `LICENSE` 文件（GitHub 创建仓库时可选模板，或到 `choosealicense.com` 复制），填入年份与版权人。

**还要确认：** 项目里用到的设计稿素材、字体、图标是否可再分发？第三方素材的许可要单独核对，不能被你的项目许可证覆盖。

---

## 3. 阶段 B：创建远程仓库并推送

### B0 账号侧准备
- 确认账号已开启 **2FA**。
- 命令行推送**不再接受账号密码**。Windows 上安装的 Git for Windows 自带 Git Credential Manager，首次推送会弹出浏览器登录授权；也可以用 `gh auth login`，或配置 SSH 密钥。
- **不要**把令牌写进远程地址或脚本里。

### B1 在网页上创建仓库
`GitHub 右上角 + → New repository`：

| 选项 | 建议 |
|---|---|
| Repository name | 小写连字符，例如 `habit-tracker-android` |
| Description | 一句话，与 README 首行一致 |
| Visibility | **Private**（检查后再转公开） |
| Add a README / .gitignore / license | **全部不要勾选**。本地已有这些文件，勾选会造成两边历史不相关，推送时冲突 |

### B2 关联并推送
```powershell
cd D:\AndroidStudio
git remote -v                      # 确认当前没有 origin
git branch --show-current          # 确认是 main
git remote add origin https://github.com/<用户名>/<仓库名>.git
git push -u origin main
```
如果装了 GitHub CLI，也可以一条命令完成创建与推送：
```powershell
gh repo create <仓库名> --private --source=. --remote=origin --push
```

### B3 推送后立刻核对
- 在网页上浏览文件树，确认没有 `local.properties`、密钥、APK、构建产物。
- 点开几个提交，看作者邮箱是否是预期的。
- 确认 README 渲染正常、图片能显示。

---

## 4. 阶段 C：推送后设置

| 位置 | 设置 | 说明 |
|---|---|---|
| 仓库首页右侧 About | Description、Topics、Website | 建议标签：`android` `kotlin` `jetpack-compose` `material3` `room` `habit-tracker` `local-first` `offline-first` |
| `Settings → General` | 默认分支 `main`；是否启用 Issues、Discussions、Wiki | 单人项目可只开 Issues |
| `Settings → Code security` | 开启 Dependabot alerts / Secret scanning | 公开仓库通常可用；私有仓库部分功能需付费方案，以设置页显示为准 |
| `Settings → Rules / Branches` | 保护 `main`（禁止强推、要求 PR 等） | 单人项目可先不做；私有仓库在免费计划下部分分支保护不可用 |
| `Insights → Community Standards` | 按清单补齐缺失文件 | 见第 5.4 节 |
| Social preview（可选） | 上传一张 1280×640 的封面图 | 分享链接时显示 |

---

## 5. 阶段 D：README 与社区文件

### 5.1 README 放在哪里
GitHub 会依次在 `.github/`、仓库根目录、`docs/` 里找 `README.md`，**放在根目录最稳妥**。

### 5.2 应该写什么：信息清单

| 信息 | 必要性 | 写什么 | 本项目可用素材 |
|---|---|---|---|
| 项目名 + 一句话简介 | **必备** | 它是什么、给谁用、核心卖点 | 本地优先的单机习惯追踪 App，无账号、无服务器 |
| 状态说明 | **必备（本项目尤其重要）** | 开发中 / 预发布；已实现 vs 计划中，不要把未做的写成已做 | 提醒（F-012/013）、备份导出（TASK-030）尚未实现 |
| 截图 / 演示 | **强烈建议** | 3~5 张主要页面，放在 `docs/images/` | 今日、新建、统计、我的、习惯详情（注意隐私） |
| 功能列表 | **必备** | 用户能做什么 | 建/改/归档/恢复/删除习惯；布尔/数值打卡与撤销；连续天数；今日进度；月热力图/周趋势；习惯详情 |
| 隐私声明 | **建议** | 数据存哪、是否联网、申请哪些权限 | 无账号、无云同步、数据仅在本机。请以 Manifest 实际权限为准（需确认），提醒上线后更新 |
| 技术栈 | **建议** | 语言、UI、存储、架构 | Kotlin、Jetpack Compose、Material 3、Room（schema v1）、minSdk 26 / targetSdk 35 |
| 架构概览 | 建议 | 一张小图或几行文字 | domain 纯规则 → repository 接口 → Room 实现 → Presenter（StateFlow）→ Compose；UseCase 收口写入规则 |
| 构建与运行 | **必备** | JDK/Android Studio 版本、命令、如何装到手机 | 见 A7，写通用做法 |
| 测试与验证 | 建议 | 怎么跑测试、`verify-all` 做了什么 | 治理检查 + 单测 + schema/DAO 验证 + 构建；沙箱限制写入「作者环境说明」 |
| 项目结构 | 建议 | 目录说明 | `app/`、`tools/`、`project/`（需求、ADR、评审、任务） |
| 文档索引 | 建议 | 指向需求、数据库、ADR | `REQUIREMENTS.md`、`DATABASE.md`、`project/` |
| 路线图 | 建议 | 接下来做什么，简短即可 | 评审、备份导出、提醒、统计周/年切换 |
| 贡献方式 | 可选 | 是否接受 PR/Issue | 不接受也要明说，避免误会 |
| 许可证 | **必备** | 许可证名称 + 链接 `LICENSE` | 见 A8 |
| 致谢 / 第三方 | 可选 | 素材、字体、开源库 | 核对设计素材许可 |
| AI 辅助说明 | 可选 | 开发中是否使用 AI 工具 | GitHub 没有要求，是否披露由你决定；披露往往利大于弊 |

**语言：** 面向国内读者可用中文；若想让更多人看到，建议中文主 README + 英文简版 `README.en.md`，并在首行互相链接。

### 5.3 README 模板（中文，可直接改）

````markdown
# Habit Tracker

> 本地优先的 Android 习惯追踪应用。无账号、无服务器，数据只在你的手机上。

English · 状态：**开发中（预发布）**

<p>
  <img src="docs/images/today.png" width="200" alt="今日页">
  <img src="docs/images/stats.png" width="200" alt="统计页">
  <img src="docs/images/detail.png" width="200" alt="习惯详情">
</p>

## 功能状态

| 功能 | 状态 |
|---|---|
| 建 / 改 / 归档 / 恢复 / 删除习惯 | ✅ 已实现 |
| 布尔 / 数值打卡与撤销 | ✅ 已实现 |
| 连续天数（当前 / 最长） | ✅ 已实现 |
| 今日进度、统计（月热力图 / 周趋势） | ✅ 已实现 |
| 习惯详情 | ✅ 已实现 |
| 备份导出 / 恢复 | 🔜 计划中 |
| 每日提醒 | 🔜 计划中 |

## 隐私

- 无账号、无服务器、无云同步，数据保存在本机 Room 数据库。
- 当前版本不联网（以 `AndroidManifest.xml` 为准）。
- 卸载或清除应用数据会丢失数据，备份导出功能完成前请留意。

## 技术栈

Kotlin · Jetpack Compose · Material 3 · Room（schema v1）· minSdk 26 / targetSdk 35 · 单 `:app` 模块

**分层：** `domain`（纯规则）→ `repository` 接口 → Room 实现 → `Presenter`（StateFlow）→ Compose 页面；写入规则由 UseCase 收口。

## 构建与运行

环境：Android Studio（版本：待填）、JDK（版本：待填）。

```bash
git clone https://github.com/<用户名>/<仓库名>.git
cd <仓库名>
./gradlew assembleDebug
./gradlew installDebug      # 需连接设备并开启 USB 调试
```

## 测试

```bash
./gradlew testDebugUnitTest   # JVM 单元测试
```
项目还带有治理与数据库校验脚本，说明见 `tools/` 与 `project/`。

## 项目结构

```
app/        Android 应用模块
tools/      验证脚本（治理检查、schema / DAO 校验等）
project/    需求、ADR、评审与任务记录
docs/       文档与截图
```

## 文档

- 需求
- 数据库设计
- 架构决策记录（ADR）

## 路线图

- [ ] 里程碑评审
- [ ] 备份导出 / 恢复
- [ ] 每日提醒
- [ ] 统计页周 / 年切换

## 贡献

（选一句）欢迎提 Issue / 暂不接受 PR。

## 许可证

MIT（按你的选择修改）
````
（路径请按你仓库的实际位置修改，例如 `REQUIREMENTS.md` 是否在 `project/` 下。）

### 5.4 其他社区文件（`Community Standards` 清单）

| 文件 | 作用 | 单人项目建议 |
|---|---|---|
| `LICENSE` | 许可证 | **必做** |
| `CONTRIBUTING.md` | 怎么提 Issue/PR、代码风格 | 如果欢迎贡献就写，几行即可 |
| `SECURITY.md` | 如何私下报告漏洞 | 建议写，一两句话 + 联系方式。别留真实邮箱，可指向 GitHub 的私有漏洞报告功能 |
| `CODE_OF_CONDUCT.md` | 行为准则 | 可选，可用 Contributor Covenant 模板 |
| `.github/ISSUE_TEMPLATE/` | Issue 模板（Bug / 功能建议） | 可选 |
| `.github/PULL_REQUEST_TEMPLATE.md` | PR 模板 | 可选，可直接复用你 `project/` 里的评审清单 |
| `CHANGELOG.md` | 版本变更 | 可选，采用 Keep a Changelog 格式 |

### 5.5 常见错误
- 把**计划中**的功能写成已实现（提醒、备份）。
- 截图里有真实个人习惯、通知栏、系统时间。
- 构建说明写成作者本机特有的路径，别人照做会失败。
- 没有 `LICENSE`，却希望别人使用代码。
- README 里贴了本机路径、设备型号等无关细节。

---

## 6. 阶段 E：可选增强

### E1 持续集成（GitHub Actions）
一个好消息：**GitHub 的 runner 没有你本机沙箱的 Gradle worker 限制**，`./gradlew test` 在 CI 上可以正常跑，正好补上你本机缺的那一环。

最小示例 `.github/workflows/android.yml`（版本号需按你的 AGP 要求调整，需确认）：
```yaml
name: Android CI
on:
  push:
    branches: [main]
  pull_request:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - uses: gradle/actions/setup-gradle@v4
      - run: chmod +x gradlew
      - run: ./gradlew assembleDebug testDebugUnitTest lintDebug
```
注意：
- 腾讯镜像从 GitHub 的海外 runner 访问可能较慢，请保留官方源作为回退。
- `tools/verify-all.ps1` 含 PowerShell 与 `D:\` 路径假设，不能直接搬到 CI，需要先参数化（A7）。
- 设备侧测试（`connectedDebugAndroidTest`）需要真机或模拟器，CI 默认不跑。

### E2 发布 APK（Releases）
- APK **不要提交进仓库**，用 `Releases` 附件发布：
```powershell
git tag -a v0.1.0-alpha -m "首个预发布版本"
git push origin v0.1.0-alpha
```
然后在网页 `Releases → Draft a new release` 选择该标签并上传 APK。
- 版本号建议遵循语义化版本（Semantic Versioning）。
- **签名：** 发布用的 keystore **绝不能**提交。若以后用 CI 签名，把它放进 GitHub Secrets。**keystore 丢失会导致无法给已安装用户升级，请单独备份。** debug 签名的 APK 只适合自己测试。

---

## 7. 公开前最终检查清单

- [ ] `.gitignore` 生效；`local.properties`、`*.keystore`、`*.jks`、APK 都不在 `git ls-files` 里
- [ ] `gradle-wrapper.jar` 和 Room schema JSON 已提交
- [ ] 当前文件与历史都扫描过密钥，没有密钥残留
- [ ] 提交作者邮箱已确认（使用 noreply 或已接受公开）
- [ ] 无超过 50 MB 的文件
- [ ] 已决定哪些过程文档公开、哪些忽略或脱敏
- [ ] 干净目录 `git clone` 后 `./gradlew assembleDebug` 通过
- [ ] `LICENSE` 已选并放入；第三方素材许可已核对
- [ ] README 含：简介、截图、**已实现 / 计划中**、隐私、构建、许可证
- [ ] README 与 Manifest 权限声明一致
- [ ] 截图不含个人信息
- [ ] 先私有仓库检查过一遍，再改为公开

---

## 8. 需要你决定的事

1. **许可证**：MIT / Apache-2.0 / GPL-3.0 / 暂不开源（A8）。
2. **提交历史**：保留原历史（哈希引用不变），还是重写 / 重新开始以隐藏旧邮箱（A4）。
3. **过程文档**：`ADVICE-*`、`CLAUDE.md` 等是否公开、脱敏或忽略（A6）。
4. **README 语言**：只中文，还是中英双语。
5. **是否披露 AI 辅助开发**。
6. **是否接受外部贡献**（决定要不要写 `CONTRIBUTING.md`）。
7. **是否现在就配 CI**，还是等 `verify-all` 参数化之后。

---

## 9. 参考资料

- GitHub：社区健康文件 —— `docs.github.com/en/communities/setting-up-your-project-for-healthy-contributions`
- GitHub：关于 README —— `docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-readmes`
- Android 的 `.gitignore` 模板 —— `github.com/github/gitignore`（`Android.gitignore`）
- 许可证选择 —— `choosealicense.com`
- README 指南 —— `makeareadme.com`；README 规范 —— `github.com/RichardLitt/standard-readme`
- 变更日志 —— `keepachangelog.com`；版本号 —— `semver.org`；提交信息 —— `conventionalcommits.org`
- 徽章 —— `shields.io`
