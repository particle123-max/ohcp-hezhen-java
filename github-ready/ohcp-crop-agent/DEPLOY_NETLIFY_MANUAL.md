# 不用 GitHub：Netlify + Render 手动部署

## 先说明两个平台的能力

- **Netlify**：支持拖拽上传静态网站文件夹。
- **Render**：不支持直接上传文件夹或 ZIP。没有 GitHub 时，可选择：
  1. 部署 Docker Hub 上的预构建镜像（推荐）；
  2. 使用 GitLab、Bitbucket 等公开 Git 仓库；
  3. 使用其他支持本地目录上传的平台，例如 Railway CLI。

本文按“不使用 GitHub”给出两条路线。

---

# 路线一：Netlify 手动上传 + Render Docker 镜像（完全不用 Git）

## 第 1 步：本机安装 Docker Desktop

安装后启动 Docker Desktop，并注册/登录 Docker Hub：

<https://hub.docker.com>

在 PowerShell 中检查：

```powershell
docker --version
docker login
```

## 第 2 步：构建并推送后端镜像

```powershell
cd D:\.codex\测试\ohcp-crop-agent

docker build -t 你的DockerHub账号/ohcp-crop-agent:latest .

docker push 你的DockerHub账号/ohcp-crop-agent:latest
```

例如账号是 `abc123`，镜像地址就是：

```text
docker.io/abc123/ohcp-crop-agent:latest
```

## 第 3 步：在 Render 部署镜像

登录 <https://dashboard.render.com>：

1. 点击 **New** → **Web Service**
2. 选择 **Deploy an existing image**
3. Image URL 填：
   ```text
   docker.io/你的DockerHub账号/ohcp-crop-agent:latest
   ```
4. Health Check Path：
   ```text
   /api/health
   ```
5. 部署

部署成功后 Render 会提供一个公网地址，例如：

```text
https://ohcp-crop-agent-api.onrender.com
```

浏览器测试：

```text
https://ohcp-crop-agent-api.onrender.com/api/health
```

看到 `"status":"UP"` 表示后端上线成功。

## 第 4 步：修改 Netlify 上传目录里的 `_redirects`

打开：

```text
D:\.codex\测试\ohcp-crop-agent\netlify-deploy\_redirects
```

把：

```text
https://YOUR-BACKEND.onrender.com/api/:splat
```

替换成你的 Render 实际地址，例如：

```text
https://ohcp-crop-agent-api.onrender.com/api/:splat
```

完整内容应类似：

```text
/api/*  https://ohcp-crop-agent-api.onrender.com/api/:splat  200
/*      /index.html  200
```

## 第 5 步：上传哪一个文件夹

上传整个文件夹：

```text
D:\.codex\测试\ohcp-crop-agent\netlify-deploy
```

里面只有前端文件：

```text
netlify-deploy/
├─ index.html
├─ styles.css
├─ app.js
├─ favicon.svg
└─ _redirects
```

**不要上传**以下文件夹：

- `src`
- `data`
- `deliverables`
- `ohcp-crop-agent` 整个项目目录
- Java 后端目录

## 第 6 步：Netlify Drop 上传

打开：

<https://app.netlify.com/drop>

把整个 `netlify-deploy` 文件夹拖进去。

Netlify 会生成：

```text
https://随机名称.netlify.app
```

测试：

1. 首页是否能打开；
2. 点击“水稻缺氮”样例；
3. 点击“开始智能诊断”；
4. 是否能显示缺氮候选和施肥建议。

再测试代理：

```text
https://你的站点.netlify.app/api/health
```

如果也返回 `"status":"UP"`，说明前后端连接成功。

---

# 路线二：不用 Docker、也不用 GitHub，使用 GitLab/Bitbucket

如果你不想安装 Docker，可以把项目上传到 GitLab 或 Bitbucket 的公开仓库。Render 支持 Git 提供商，不需要 GitHub。

## 第 1 步：上传到 GitLab

在 <https://gitlab.com> 创建公开项目，然后：

```powershell
cd D:\.codex\测试\ohcp-crop-agent
git init
git add .
git commit -m "OHCP crop agent"
git branch -M main
git remote add origin https://gitlab.com/你的账号/ohcp-crop-agent.git
git push -u origin main
```

## 第 2 步：在 Render 创建 Web Service

1. **New** → **Web Service**
2. 连接 GitLab 仓库
3. Runtime 选择 **Docker**
4. Dockerfile Path：`./Dockerfile`
5. Health Check Path：`/api/health`
6. 部署

后续 Netlify 上传步骤与路线一第 4–6 步完全相同。

---

# 最终关系图

```text
Netlify Drop
  netlify-deploy 文件夹
        ↓
https://你的站点.netlify.app
        ↓ /api/* 反向代理
Render Docker 服务
        ↓
Spring Boot JAR
        ↓
本地 JSON 知识库
```

---

# 最重要的一点

Netlify 只能上传前端静态文件夹：

```text
D:\.codex\测试\ohcp-crop-agent\netlify-deploy
```

Java 后端必须部署到 Render，然后把 Render 地址写进 `netlify-deploy/_redirects`。
