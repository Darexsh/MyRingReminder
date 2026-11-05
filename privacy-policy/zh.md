# 隐私政策 – MyRingReminder

Last updated: 2026-11-05

## 1. 控制者
Daniel Sichler (Darexsh)  
电子邮箱：sichler.daniel@gmail.com

## 2. 适用范围
本政策适用于通过本代码库及 APK 版本发布的 MyRingReminder Android 应用（`com.darexsh.myringreminder`）。

## 3. MyRingReminder 处理的数据
MyRingReminder 会处理周期跟踪和个性化所需的应用数据，包括：
- 周期日期和周期记录
- 经期记录（例如经期日标记、出血量、疼痛或疾病等症状标记，以及可选的开始和结束标记）
- 提醒设置和通知时间
- 用户输入的笔记
- 设计偏好（颜色、背景和语言）
- 可选的应用锁设置

## 4. 本地数据存储
MyRingReminder 会将你的数据存储在设备本地（例如通过 SharedPreferences 或应用本地存储）。

MyRingReminder 不会将你的个人周期数据或笔记上传给开发者。

如果你使用备份或恢复功能，备份数据会作为本地文件导出或导入，该文件由你通过 Android 文件选择器选取。

如果你导出经期详情 PDF，PDF 会在设备本地生成，并且只保存到你在 Android 文件选择器中选择的位置。  
当你从应用通知中打开该 PDF 时，Android 可能会将文件交给你选择的 PDF 查看器应用。在这种情况下，该查看器应用会按照自己的隐私政策处理文件。

## 5. 使用的权限
MyRingReminder 可能会请求应用功能所需的权限，例如：
- 通知和精确闹钟（周期提醒）
- 媒体或文件访问权限（选择背景图片、备份和恢复文件）
- 生物识别信息或设备凭据（可选的应用锁）

权限仅用于提供你请求的应用功能。

## 6. 分析、跟踪和崩溃报告
根据当前实现，MyRingReminder **不**包含第三方分析 SDK 或第三方崩溃报告 SDK。

## 7. 网络和外部链接
MyRingReminder 的核心功能以本地运行为主，周期跟踪不需要专用的后端 API。

为了检查更新，MyRingReminder 可能会请求本项目公开的 GitHub Releases API，以获取版本信息。

当你打开**设置 → 药品说明书**时，应用会请求本项目 GitHub 代码库中的公开文件 `leaflet-packages.json`。当你打开**应用信息 → 隐私政策**时，应用会从同一代码库请求与你选择的应用语言对应的政策文件；如果没有对应的翻译，则使用英文版本。这些请求不会包含你的周期数据、笔记或应用设置。GitHub 可能会根据其隐私政策处理常规连接信息，例如你的 IP 地址和请求标头。应用会在设备上分别保留每种语言最近一次成功获取的副本，以便后续请求失败时仍可阅读。

在你主动操作时，MyRingReminder 可以通过系统 Intent 打开外部目标，包括：
- 电子邮件（`mailto:`）
- Linktree
- Telegram
- GitHub
- Buy Me a Coffee
- 由外部药品信息服务或制造商托管的药品说明书

当你打开药品说明书时，Android 会将其 URL 传递给你选择的浏览器或文档查看器。托管服务会收到该请求；URL 可能会透露药品和说明书语言。浏览器、查看器和说明书托管方可能会根据各自的隐私政策处理常规连接信息。MyRingReminder 不会在这些请求中附加本地存储的周期数据、笔记或设置。

打开外部链接时，相关第三方服务的隐私政策适用。

### 链接中提及的第三方服务
- GitHub: https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement
- Telegram: https://telegram.org/privacy
- Linktree: https://linktr.ee/s/privacy
- Buy Me a Coffee: https://www.buymeacoffee.com/privacy-policy

## 8. 数据共享
根据当前应用实现，MyRingReminder 不会向开发者传输本地存储的周期数据、笔记或设置。

## 9. 医疗免责声明
MyRingReminder 仅用于组织和跟踪信息。

它不是医疗器械，不提供医疗诊断，也不能替代专业医疗建议。

如有医疗问题或健康方面的顾虑，请咨询合格的医疗专业人员。

## 10. 你的选择
- 你可以在应用内编辑或删除数据。
- 你可以在 Android 设置中撤销应用权限。
- 你可以选择不打开外部链接。
- 你可以卸载应用，并在 Android 设置中清除本地数据。

## 11. 政策变更
如果应用功能或法律要求发生变化，本政策可能会更新。
