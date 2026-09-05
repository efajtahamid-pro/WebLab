package com.efajtahamid.weblab.template

import com.efajtahamid.weblab.data.model.ProjectType
import java.io.File

/**
 * Writes real, runnable starter files for a new project. Nothing here is a
 * placeholder — a Static Website template opens and renders immediately in the
 * Preview tab, and a Node App template is a genuine minimal HTTP server that
 * will run once the Node runtime (Phase 4) is wired up.
 */
object ProjectTemplates {

    fun scaffold(dir: File, type: ProjectType) {
        when (type) {
            ProjectType.STATIC_SITE -> staticSite(dir)
            ProjectType.NODE_APP -> nodeApp(dir)
            ProjectType.VITE_APP -> viteApp(dir)
        }
    }

    private fun staticSite(dir: File) {
        File(dir, "index.html").writeText(
            """
            |<!DOCTYPE html>
            |<html lang="en">
            |<head>
            |  <meta charset="UTF-8" />
            |  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            |  <title>My Website</title>
            |  <link rel="stylesheet" href="style.css" />
            |</head>
            |<body>
            |  <h1>Hello from WebLab</h1>
            |  <p>Edit <code>index.html</code>, <code>style.css</code>, and <code>script.js</code> to get started.</p>
            |  <button id="greet">Click me</button>
            |  <script src="script.js"></script>
            |</body>
            |</html>
            |""".trimMargin()
        )
        File(dir, "style.css").writeText(
            """
            |body {
            |  font-family: -apple-system, Segoe UI, Roboto, sans-serif;
            |  background: #0f1117;
            |  color: #e5e7eb;
            |  padding: 24px;
            |}
            |button {
            |  background: linear-gradient(90deg, #22d3ee, #a855f7);
            |  border: none;
            |  color: #0f1117;
            |  font-weight: 600;
            |  padding: 10px 18px;
            |  border-radius: 8px;
            |}
            |""".trimMargin()
        )
        File(dir, "script.js").writeText(
            """
            |document.getElementById('greet').addEventListener('click', () => {
            |  console.log('Button clicked');
            |  alert('Hello from WebLab!');
            |});
            |""".trimMargin()
        )
    }

    private fun nodeApp(dir: File) {
        File(dir, "package.json").writeText(
            """
            |{
            |  "name": "my-node-app",
            |  "version": "1.0.0",
            |  "private": true,
            |  "scripts": {
            |    "start": "node server.js",
            |    "dev": "node server.js"
            |  },
            |  "dependencies": {}
            |}
            |""".trimMargin()
        )
        File(dir, "server.js").writeText(
            """
            |const http = require('http');
            |const fs = require('fs');
            |const path = require('path');
            |
            |const PORT = process.env.PORT || 3000;
            |
            |const server = http.createServer((req, res) => {
            |  const filePath = path.join(__dirname, 'public', req.url === '/' ? 'index.html' : req.url);
            |  fs.readFile(filePath, (err, data) => {
            |    if (err) {
            |      res.writeHead(404, { 'Content-Type': 'text/plain' });
            |      res.end('Not found');
            |      return;
            |    }
            |    res.writeHead(200, { 'Content-Type': 'text/html' });
            |    res.end(data);
            |  });
            |});
            |
            |server.listen(PORT, () => {
            |  console.log(`Server running on port ${'$'}{PORT}`);
            |});
            |""".trimMargin()
        )
        val publicDir = File(dir, "public").apply { mkdirs() }
        File(publicDir, "index.html").writeText(
            """
            |<!DOCTYPE html>
            |<html lang="en">
            |<head><meta charset="UTF-8" /><title>My Node App</title></head>
            |<body>
            |  <h1>Hello from a real Node.js server</h1>
            |</body>
            |</html>
            |""".trimMargin()
        )
    }

    private fun viteApp(dir: File) {
        File(dir, "package.json").writeText(
            """
            |{
            |  "name": "my-app",
            |  "version": "1.0.0",
            |  "private": true,
            |  "scripts": {
            |    "dev": "vite",
            |    "build": "vite build",
            |    "preview": "vite preview"
            |  },
            |  "devDependencies": {
            |    "vite": "^5.0.0"
            |  }
            |}
            |""".trimMargin()
        )
        File(dir, "index.html").writeText(
            """
            |<!DOCTYPE html>
            |<html lang="en">
            |<head>
            |  <meta charset="UTF-8" />
            |  <title>My Vite App</title>
            |</head>
            |<body>
            |  <div id="app"></div>
            |  <script type="module" src="/src/main.js"></script>
            |</body>
            |</html>
            |""".trimMargin()
        )
        val srcDir = File(dir, "src").apply { mkdirs() }
        File(srcDir, "main.js").writeText(
            """
            |import './style.css';
            |
            |document.querySelector('#app').innerHTML = `
            |  <h1>Hello Vite + WebLab</h1>
            |`;
            |""".trimMargin()
        )
        File(srcDir, "style.css").writeText("body { font-family: sans-serif; }\n")
        File(dir, "public").mkdirs()
    }
}
