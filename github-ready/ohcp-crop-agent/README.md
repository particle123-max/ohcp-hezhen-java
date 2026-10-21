# 禾诊 · 农作物缺素症识别与科学施肥建议智能体

面向 OHCP 一小时课堂编程赛的完整可运行项目。核心链路为：

> 作物症状输入 → 本地私有知识库检索 → 多候选缺素识别与置信度 → 风险预警 → 科学施肥建议 → 前端可视化展示

项目默认**不依赖大模型、不依赖数据库、不联网即可演示**。Spring AI 与 LangChain4j 已接入为可选增强能力，缺素判断和施肥事实始终来自本地 JSON 知识库。

## 1. 已实现功能

| 赛题要求 | 本项目实现 |
|---|---|
| 缺素症智能识别 | 作物/部位/症状/环境多字段输入；氮、磷、钾、镁、铁、锌、硼、钙 8 类知识；Top 3 候选及置信度 |
| 科学施肥建议 | 农艺调控、有机/生物肥、化学追施、安全与减量提醒；农户简易版与农技专业版双模板 |
| 本地知识库 RAG | 公开资料清洗后结构化为本地 JSON；症状关键词加权 + 部位/作物匹配 + 中文双字组稀疏向量余弦重排 |
| 不允许直接调用大模型出答案 | 核心 REST 链路完全本地完成；大模型仅在显式开启时润色 1～2 句思政提示 |
| SpringBoot + Spring AI | Spring Boot 3.5.16 + Spring AI 1.1.8 |
| Java 智能体框架 | LangChain4j 1.22.0，知识检索以 `@Tool` 方式开放给智能体 |
| 前后端分离 | Spring Boot REST API + 原生 HTML/CSS/JS 单页交互 |
| 课程思政自然融入 | 随诊断结果输出 1～2 句农业、粮食安全、绿色施肥相关提示，不打弹窗 |
| 创新点 | 可解释证据链、田间风险预警、农户/农技双模板、知识库热重载、现场查询统计看板 |
| 一键本地运行 | `start.bat` / `start.ps1`，或直接运行已构建 JAR |

## 2. 技术栈

- Java 17
- Spring Boot 3.5.16
- Spring AI 1.1.8（可选大模型润色）
- LangChain4j 1.22.0（知识库工具/智能体扩展）
- Maven 3.9+
- 原生 HTML + CSS + JavaScript
- 本地 JSON 知识库，无需数据库

## 3. 目录结构

```text
ohcp-crop-agent/
├─ data/
│  └─ knowledge-base.json          # 外部知识库，可改完热重载
├─ docs/
│  ├─ 5人分工与答辩稿.md
│  ├─ PPT大纲.md
│  └─ ui-smoke-result.png          # 浏览器端到端冒烟测试截图
├─ src/main/java/com/ohcp/cropagent/
│  ├─ config/
│  ├─ controller/                  # REST 接口
│  ├─ domain/                      # 请求/响应/知识条目模型
│  └─ service/
│     ├─ KnowledgeBaseService.java # JSON 加载、外部文件热重载
│     ├─ TextMatcher.java          # 本地 RAG 检索与重排
│     ├─ RiskAssessor.java         # 风险预警
│     ├─ AdviceFactory.java        # 双版本施肥建议
│     ├─ DiagnosisAgentService.java# 智能体编排
│     ├─ KnowledgeBaseTools.java   # LangChain4j @Tool
│     ├─ LlmNarrator.java          # Spring AI 可选润色
│     └─ QueryStatsService.java    # 现场统计
├─ src/main/resources/
│  ├─ knowledge-base.json          # JAR 内置兜底知识库
│  ├─ static/                      # 前端页面
│  └─ application.yml
├─ src/test/java/                  # 7 个自动化测试
├─ tools/
│  ├─ knowledge_crawler.py         # 公开资料采集/清洗/切片
│  ├─ requirements.txt
│  └─ ui_smoke_test.mjs            # 浏览器端到端冒烟测试
├─ pom.xml
├─ start.bat
└─ start.ps1
```

## 4. 快速启动

### 方式 A：直接运行已构建 JAR（推荐现场演示）

前置条件：已安装 Java 17+。

Windows 双击 `start.bat`，或在 PowerShell 中执行：

```powershell
cd ohcp-crop-agent
java -jar dist\crop-agent.jar
```

打开 <http://localhost:8080>。

### 方式 B：从源码启动

前置条件：JDK 17+、Maven 3.9+。

```powershell
cd ohcp-crop-agent
mvn spring-boot:run
```

### 方式 C：测试与打包

```powershell
mvn test
mvn -DskipTests package
java -jar target\crop-agent.jar
```

本机验证结果：`Tests run: 7, Failures: 0, Errors: 0`。详细记录见 `docs/测试报告.md`。

## 5. 演示输入

点击页面“快速样例”或手动输入：

```text
作物：水稻
部位：下部老叶
生育期：分蘖期
土壤：沙质土
环境：近期降雨较多，田间湿度偏高
症状：叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少。
```

预期输出：

- 首要候选：缺氮（N）
- 同时展示磷、钾等次候选及置信度
- 中风险预警：沙质土、降雨、关键生育期
- 农艺调控、有机肥、化学追施、安全减量四类建议
- 农户简易版 / 农技专业版切换
- 本地知识证据、公开资料来源、自然融入的思政提示

其他样例：小麦叶缘焦枯（缺钾）、玉米新叶白条（缺锌）、番茄脐腐（缺钙）、柑橘新叶脉间失绿（缺铁）。

## 6. REST API

### 6.1 诊断

`POST /api/diagnose`

```json
{
  "crop": "水稻",
  "part": "下部老叶",
  "symptoms": "叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少。",
  "growthStage": "分蘖期",
  "soilType": "沙质土",
  "environment": "近期降雨较多"
}
```

响应包含：

- `candidates[]`：多候选、置信度、命中证据、来源 ID
- `risk`：风险等级、分数、原因与建议
- `advice`：双版本施肥方案
- `retrieval`：知识库条数、命中术语、耗时、策略
- `sources[]`：资料标题与发布机构
- `civicNote`：自然融入的思政提示

### 6.2 其他接口

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/health` | 健康状态、技术栈与知识库版本 |
| GET | `/api/knowledge` | 知识库摘要 |
| POST | `/api/knowledge/reload` | 修改外部 JSON 后热重载 |
| GET | `/api/stats` | 查询总数、高频缺素、平均检索耗时 |

PowerShell 调用示例：

```powershell
$body = @{
  crop = "水稻"
  part = "下部老叶"
  symptoms = "叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少。"
  growthStage = "分蘖期"
  soilType = "沙质土"
  environment = "近期降雨较多"
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/diagnose `
  -ContentType "application/json; charset=utf-8" -Body $body
```

## 7. 知识库与爬虫

正式知识库：`data/knowledge-base.json`。系统启动时优先加载该外部文件；文件不存在时回退到 JAR 内置副本。

修改外部 JSON 后无需重启：

```powershell
Invoke-RestMethod -Method Post http://localhost:8080/api/knowledge/reload
```

采集公开资料：

```powershell
python -m pip install -r tools/requirements.txt
python tools/knowledge_crawler.py `
  --url "https://具体公开文章地址" `
  --output data/crawled_chunks.jsonl
```

脚本会：

1. 遵守 `robots.txt`，控制请求间隔；
2. 去除导航、脚本、页脚等噪声；
3. 提取正文并切分为带重叠的文本块；
4. 去重后输出 JSONL。

**注意**：爬取结果不会自动写入正式知识库。比赛提交前需人工核对来源、症状和施肥表述，再合并为结构化 JSON，避免污染知识库。

## 8. 大模型可选增强

默认关闭：

```yaml
app:
  llm:
    enabled: false
```

如需演示 Spring AI 润色能力：

```powershell
$env:OPENAI_API_KEY="你的密钥"
$env:APP_LLM_ENABLED="true"
java -jar dist\crop-agent.jar
```

约束：

- 大模型不参与缺素分类、候选排序和施肥事实生成；
- 只润色思政提示，失败时自动回退到本地文本；
- 断网、无密钥时所有核心功能不受影响。

## 9. 检索与排序说明

每条知识包含：

- 作物、部位、别名、典型症状、风险因素、标签；
- 缺素简介；
- 农艺、有机、化学、安全四类建议；
- 来源 ID、农户版与农技版文案。

检索评分由以下部分组成：

1. 典型症状命中；
2. 缺素别名命中；
3. 作物与部位匹配；
4. 标签和风险条件命中；
5. 查询与知识全文的中文双字组稀疏词频向量余弦相似度。

置信度使用绝对分数映射，而非仅做相对归一化，因此“描述含糊”时不会强行给出高置信度。

## 10. 五人分工建议（60 分钟）

| 角色 | 0–10 分钟 | 10–40 分钟 | 40–50 分钟 | 50–60 分钟 |
|---|---|---|---|---|
| A 队长/后端 | 定接口与主链路 | 智能体编排、API | 联调、修阻断问题 | 演示兜底与提交检查 |
| B 知识库/数据 | 确认数据来源与字段 | 清洗 JSON、补症状词 | 核对证据与来源 | 讲数据与 RAG |
| C 前端/交互 | 页面骨架与交互 | 表单、结果可视化 | 浏览器验收 | 现场操作演示 |
| D 测试/部署 | 写核心测试用例 | 接口与样例回归 | 打包、启动演练 | 记录测试证据 |
| E PPT/答辩 | 定叙事与思政点 | 做 PPT 与演示稿 | 计时彩排、准备追问 | 主讲答辩 |

详见 `docs/5人分工与答辩稿.md` 和 `docs/PPT大纲.md`。

## 11. 答辩演示顺序（5 分钟）

1. **需求理解（30 秒）**：农户看到黄叶后难以判断缺素，容易盲目施肥。
2. **架构方案（1 分钟）**：前端 → Spring Boot API → 本地 RAG → 风险/建议模板 → 结果回显。
3. **实现演示（1 分 30 秒）**：输入水稻缺氮 → 多候选 → 风险 → 双版本建议 → 来源与思政。
4. **创新点（1 分钟）**：可解释证据链、风险预警、外部知识库热重载、现场统计。
5. **测试与分工（30 秒）**：7 个自动化测试、浏览器冒烟测试、5 人并行分工。
6. **价值收束（30 秒）**：科学施肥、化肥减量增效、藏粮于地、科技助农。

## 12. 已知边界

- 本项目是教学与课程演示系统，不替代土壤检测、植物病理诊断和当地农技人员现场指导。
- 化学肥料的具体用量必须结合土壤检测、目标产量、作物品种和产品标签确定；系统不提供脱离场景的“万能用量”。
- 图片识别、多轮对话未作为默认功能，比赛时间有限时优先保证“文本输入 → 本地 RAG → 建议输出”的完整链路。





## 13. 汇报 PPT

- 最终演示文稿：`deliverables/禾诊_OHCP农作物缺素症智能体_汇报.pptx`
- 页面预览：`deliverables/汇报PPT预览.png`
- 共 8 页：封面、需求理解、系统架构、实现演示、人员分工、创新点、问题反思与展望、致谢。
- 使用当前文件夹中的 `pdf.pptx` 模板版式与配色，保留原有深色科技风。


## 14. 外网部署

Netlify 不能直接运行 Spring Boot 后端。推荐架构为：`Netlify 前端 + Render/Railway 后端 + Netlify /api 反向代理`。

完整步骤见 `DEPLOY_NETLIFY.md`。项目已提供 `netlify.toml`、`Dockerfile` 和 `render.yaml`。

