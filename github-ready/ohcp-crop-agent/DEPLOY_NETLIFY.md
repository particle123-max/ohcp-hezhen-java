# 通过 Netlify 让禾诊网站外部访问

## 先说结论

当前项目是：

```text
HTML/CSS/JS 前端 + Spring Boot Java 后端 + 本地 JSON 知识库
```

Netlify 只能运行静态前端，**不能直接运行 Spring Boot 的 JAR**。所以必须采用前后端分离部署：

```text
用户浏览器
   ↓
Netlify（前端静态页面）
   ↓  /api/* 反向代理
Render / Railway（Spring Boot 后端）
   ↓
本地 JSON 知识库（随 JAR 一起部署）
```

这样页面和接口都通过 Netlify 域名访问，不需要单独处理跨域。

---

## 一、准备 GitHub 仓库

建议只把 `ohcp-crop-agent` 作为 Git 仓库根目录，避免把 `crop-agent`、截图和其他临时文件一起提交。

```powershell
cd D:\.codex\测试\ohcp-crop-agent
git init
git add .
git commit -m "OHCP crop nutrient diagnosis agent"
git branch -M main
git remote add origin https://github.com/你的账号/ohcp-crop-agent.git
git push -u origin main
```

仓库中已经包含：

- `Dockerfile`：供 Render/Railway 构建 Java 后端
- `render.yaml`：Render 部署参考
- `netlify.toml`：Netlify 静态发布和 API 代理配置

---

## 二、部署 Spring Boot 后端到 Render

### 1. 新建 Web Service

登录 <https://render.com>，选择：

- **New** → **Web Service**
- 连接刚才的 GitHub 仓库
- Language/Runtime：**Docker**
- Dockerfile Path：`./Dockerfile`
- Health Check Path：`/api/health`

Render 会自动使用项目中的 `Dockerfile` 构建并运行：

```text
java -jar app.jar
```

项目已经支持 Render 注入的 `PORT` 环境变量：

```yaml
server:
  port: ${PORT:8080}
```

### 2. 得到后端地址

部署成功后，Render 会生成类似地址：

```text
https://ohcp-crop-agent-api.onrender.com
```

在浏览器测试：

```text
https://ohcp-crop-agent-api.onrender.com/api/health
```

看到：

```json
{"status":"UP","coreEngine":"LOCAL_RAG", "...": "..."}
```

说明后端成功上线。

> Render 免费实例长时间无请求后会休眠，第一次访问可能需要等待几十秒。正式答辩前建议提前打开一次预热。

---

## 三、修改 Netlify 的 API 代理地址

打开项目根目录的 `netlify.toml`，把：

```toml
to = "https://YOUR-BACKEND.onrender.com/api/:splat"
```

改成你的 Render 后端地址：

```toml
to = "https://ohcp-crop-agent-api.onrender.com/api/:splat"
```

完整配置应类似：

```toml
[build]
  publish = "src/main/resources/static"
  command = "echo Static frontend: no build step required"

[[redirects]]
  from = "/api/*"
  to = "https://ohcp-crop-agent-api.onrender.com/api/:splat"
  status = 200
  force = true

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200
```

提交修改：

```powershell
git add netlify.toml
git commit -m "Configure Netlify API proxy"
git push
```

---

## 四、在 Netlify 部署前端

### 方式 A：连接 GitHub 自动部署（推荐）

登录 <https://app.netlify.com>：

1. **Add new site** → **Import an existing project**
2. 选择 GitHub 仓库
3. Build command 留空，或填：
   ```text
   echo Static frontend: no build step required
   ```
4. Publish directory：
   ```text
   src/main/resources/static
   ```
5. 点击 Deploy

成功后 Netlify 会生成类似地址：

```text
https://your-site-name.netlify.app
```

打开该地址，页面会：

1. 从 Netlify 加载 `index.html`、`styles.css`、`app.js`
2. 浏览器请求 `/api/diagnose`
3. Netlify 将该请求代理到 Render 后端
4. 页面显示候选、风险、建议和来源

### 方式 B：Netlify CLI 手动部署

安装并登录：

```powershell
npm install -g netlify-cli
netlify login
```

在项目根目录执行：

```powershell
cd D:\.codex\测试\ohcp-crop-agent
netlify init
netlify deploy --prod
```

部署目录选择：

```text
src/main/resources/static
```

`netlify.toml` 会自动应用 API 代理规则。

---

## 五、上线后的检查清单

### 前端检查

```text
https://你的站点.netlify.app
```

- 首页能打开
- 知识库版本显示 `v1.1.0`
- 点击“水稻缺氮”样例能填入症状
- 点击“开始智能诊断”能返回结果

### 后端检查

```text
https://你的后端.onrender.com/api/health
```

- 返回 `status: UP`
- `knowledgeEntries: 8`
- `coreEngine: LOCAL_RAG`

### 代理检查

在 Netlify 域名下请求：

```text
https://你的站点.netlify.app/api/health
```

如果也返回 `status: UP`，说明代理配置成功。

---

## 六、常见问题

### 1. Netlify 页面能打开，但点击诊断报错

优先检查：

```toml
to = "https://YOUR-BACKEND.onrender.com/api/:splat"
```

是否已经替换成真实后端地址。

### 2. Render 后端第一次访问很慢

免费实例休眠导致，等待几十秒后重试。答辩前提前访问 `/api/health` 预热。

### 3. 能否只用 Netlify，不部署后端？

不能直接运行当前 Java 后端。仅在 Netlify 托管时，需要把 Java 业务逻辑改写为 Netlify Functions（Node.js），这会改变课程要求中的 Spring Boot + Spring AI 技术栈，因此不推荐。

### 4. 是否需要配置 CORS？

当前推荐方案使用 Netlify 的 `/api/*` 反向代理，浏览器访问的是同源 `/api`，不需要 CORS。

如果前端直接请求 Render 域名，则需要在 Spring Boot 中增加 CORS 配置；当前不建议这样做。

---

## 七、最短部署流程

```text
1. GitHub 上传 ohcp-crop-agent
2. Render 用 Dockerfile 部署后端
3. 测试 Render /api/health
4. 把 Render 地址写入 netlify.toml
5. Netlify 导入 GitHub 仓库
6. Publish directory = src/main/resources/static
7. 打开 Netlify 域名测试诊断
```
