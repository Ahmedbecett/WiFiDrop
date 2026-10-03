package com.example.server

object WebClientHtml {
    fun getHtml(deviceName: String, pinRequired: Boolean): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>WiFiDrop — $deviceName</title>
  <style>
    :root {
      --bg: #0b0f19;
      --card-bg: rgba(22, 30, 46, 0.85);
      --card-border: rgba(255, 255, 255, 0.1);
      --accent: #38bdf8;
      --accent-gradient: linear-gradient(135deg, #38bdf8 0%, #6366f1 100%);
      --text: #f8fafc;
      --text-muted: #94a3b8;
      --success: #10b981;
      --danger: #ef4444;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
    body {
      background: radial-gradient(circle at 50% 0%, #1e293b 0%, var(--bg) 75%);
      color: var(--text);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 24px 16px;
    }
    .container { width: 100%; max-width: 720px; display: flex; flex-direction: column; gap: 20px; }
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 24px;
      background: var(--card-bg);
      backdrop-filter: blur(16px);
      border: 1px solid var(--card-border);
      border-radius: 20px;
      box-shadow: 0 10px 25px rgba(0,0,0,0.4);
    }
    .brand { display: flex; align-items: center; gap: 12px; }
    .brand-icon {
      width: 44px; height: 44px; border-radius: 12px;
      background: var(--accent-gradient);
      display: flex; align-items: center; justify-content: center;
      font-size: 22px; font-weight: bold; color: #fff;
      box-shadow: 0 4px 14px rgba(56, 189, 248, 0.35);
    }
    .brand-info h1 { font-size: 1.15rem; font-weight: 700; color: #fff; }
    .brand-info p { font-size: 0.82rem; color: var(--text-muted); display: flex; align-items: center; gap: 6px; }
    .status-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--success); display: inline-block; box-shadow: 0 0 8px var(--success); }
    .card {
      background: var(--card-bg);
      backdrop-filter: blur(16px);
      border: 1px solid var(--card-border);
      border-radius: 20px;
      padding: 24px;
      box-shadow: 0 10px 25px rgba(0,0,0,0.3);
    }
    .card-title { font-size: 1.1rem; font-weight: 600; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
    
    /* PIN Prompt */
    #pin-section { text-align: center; }
    .pin-input {
      font-size: 2rem; letter-spacing: 12px; text-align: center;
      background: rgba(0,0,0,0.3); border: 2px solid var(--card-border);
      color: #fff; padding: 12px 20px; border-radius: 14px;
      width: 240px; margin: 16px auto; outline: none; transition: 0.2s;
    }
    .pin-input:focus { border-color: var(--accent); box-shadow: 0 0 16px rgba(56, 189, 248, 0.4); }
    .btn {
      background: var(--accent-gradient); color: #fff; border: none;
      padding: 12px 28px; border-radius: 12px; font-size: 1rem;
      font-weight: 600; cursor: pointer; transition: transform 0.15s, opacity 0.15s;
    }
    .btn:hover { opacity: 0.92; transform: translateY(-1px); }
    .btn:active { transform: translateY(0); }
    .btn-secondary {
      background: rgba(255,255,255,0.08); color: #e2e8f0; border: 1px solid var(--card-border);
    }

    /* Drop Zone */
    .dropzone {
      border: 2px dashed rgba(56, 189, 248, 0.4);
      border-radius: 16px;
      padding: 40px 20px;
      text-align: center;
      cursor: pointer;
      background: rgba(56, 189, 248, 0.03);
      transition: all 0.25s ease;
    }
    .dropzone.dragover {
      border-color: var(--accent);
      background: rgba(56, 189, 248, 0.1);
      transform: scale(1.01);
    }
    .drop-icon { font-size: 44px; margin-bottom: 12px; }
    .dropzone h3 { font-size: 1.1rem; margin-bottom: 6px; }
    .dropzone p { color: var(--text-muted); font-size: 0.85rem; }

    /* Progress */
    .progress-box {
      margin-top: 18px;
      padding: 16px;
      background: rgba(0,0,0,0.3);
      border-radius: 14px;
      display: none;
    }
    .progress-info {
      display: flex; justify-content: space-between; font-size: 0.85rem; color: var(--text-muted); margin-bottom: 8px;
    }
    .progress-bar-bg {
      width: 100%; height: 10px; background: rgba(255,255,255,0.1); border-radius: 5px; overflow: hidden;
    }
    .progress-bar-fill {
      height: 100%; width: 0%; background: var(--accent-gradient);
      border-radius: 5px; transition: width 0.1s linear;
    }
    .speed-badge {
      display: inline-block; font-weight: 700; color: var(--accent);
    }

    /* Files List */
    .files-list { display: flex; flex-direction: column; gap: 10px; max-height: 380px; overflow-y: auto; }
    .file-row {
      display: flex; align-items: center; justify-content: space-between;
      padding: 12px 16px; background: rgba(255,255,255,0.03);
      border: 1px solid var(--card-border); border-radius: 12px;
      transition: background 0.15s;
    }
    .file-row:hover { background: rgba(255,255,255,0.06); }
    .file-meta { display: flex; align-items: center; gap: 12px; overflow: hidden; }
    .file-icon { font-size: 24px; min-width: 28px; }
    .file-name { font-weight: 500; font-size: 0.95rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 320px; }
    .file-size { font-size: 0.78rem; color: var(--text-muted); }
    .dl-btn {
      background: rgba(56, 189, 248, 0.15); color: var(--accent);
      border: 1px solid rgba(56, 189, 248, 0.3); border-radius: 8px;
      padding: 6px 14px; font-size: 0.85rem; font-weight: 600; text-decoration: none;
      display: inline-flex; align-items: center; gap: 6px;
    }
    .dl-btn:hover { background: var(--accent); color: #0b0f19; }
    .empty-state { text-align: center; padding: 32px 16px; color: var(--text-muted); font-size: 0.9rem; }
    
    footer { text-align: center; color: var(--text-muted); font-size: 0.8rem; margin-top: 10px; }
  </style>
</head>
<body>
  <div class="container">
    <header>
      <div class="brand">
        <div class="brand-icon">⚡</div>
        <div class="brand-info">
          <h1>$deviceName</h1>
          <p><span class="status-dot"></span> Wi-Fi Local Connection Active</p>
        </div>
      </div>
      <button class="btn btn-secondary" onclick="location.reload()">↻ Refresh</button>
    </header>

    <!-- PIN Authentication Screen (if enabled) -->
    <div class="card" id="pin-section" style="${if (pinRequired) "display: block;" else "display: none;"}">
      <div class="card-title">🔐 Enter Security PIN</div>
      <p style="color: var(--text-muted); font-size: 0.9rem; margin-bottom: 8px;">
        Look at your phone screen and enter the 6-digit PIN shown on WiFiDrop.
      </p>
      <input type="text" id="pin-input" class="pin-input" maxlength="6" placeholder="••••••" autofocus />
      <br>
      <button class="btn" onclick="submitPin()">Authorize Connection</button>
      <p id="pin-error" style="color: var(--danger); font-size: 0.85rem; margin-top: 10px; display: none;">Invalid PIN. Please check your phone screen.</p>
    </div>

    <!-- Main Transfer Interface -->
    <div id="main-content" style="${if (pinRequired) "display: none;" else "display: block;"}">
      <!-- Upload Card -->
      <div class="card">
        <div class="card-title">📤 Upload to Phone</div>
        <div class="dropzone" id="dropzone" onclick="document.getElementById('file-input').click()">
          <div class="drop-icon">📁</div>
          <h3>Drag &amp; Drop Files Here</h3>
          <p>or click to browse photos, videos, documents, and archives from your computer</p>
          <input type="file" id="file-input" multiple style="display: none;" onchange="handleFileSelect(this.files)">
        </div>

        <div class="progress-box" id="progress-box">
          <div class="progress-info">
            <span id="progress-filename">Uploading...</span>
            <span class="speed-badge" id="progress-speed">0 MB/s</span>
          </div>
          <div class="progress-bar-bg">
            <div class="progress-bar-fill" id="progress-fill"></div>
          </div>
          <div class="progress-info" style="margin-top: 6px; margin-bottom: 0;">
            <span id="progress-bytes">0 / 0 MB</span>
            <span id="progress-percent">0%</span>
          </div>
        </div>
      </div>

      <!-- Files on Phone Card -->
      <div class="card" style="margin-top: 20px;">
        <div class="card-title" style="justify-content: space-between;">
          <span>📥 Files on Phone</span>
          <button class="btn btn-secondary" style="padding: 6px 14px; font-size: 0.8rem;" onclick="loadFiles()">Refresh List</button>
        </div>
        <div class="files-list" id="files-list">
          <div class="empty-state">Loading files...</div>
        </div>
      </div>
    </div>

    <footer>
      ⚡ WiFiDrop Local File Transfer · Direct Peer-to-Peer over Wi-Fi · No Internet Required
    </footer>
  </div>

  <script>
    let isAuthed = ${!pinRequired};

    function submitPin() {
      const pin = document.getElementById('pin-input').value.trim();
      if (!pin) return;
      fetch('/api/verify-pin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pin: pin })
      })
      .then(res => res.json())
      .then(data => {
        if (data.success) {
          isAuthed = true;
          document.getElementById('pin-section').style.display = 'none';
          document.getElementById('main-content').style.display = 'block';
          loadFiles();
        } else {
          document.getElementById('pin-error').style.display = 'block';
        }
      })
      .catch(() => {
        document.getElementById('pin-error').style.display = 'block';
      });
    }

    // Drag and drop setup
    const dropzone = document.getElementById('dropzone');
    ['dragenter', 'dragover'].forEach(name => {
      dropzone.addEventListener(name, (e) => {
        e.preventDefault();
        dropzone.classList.add('dragover');
      });
    });
    ['dragleave', 'drop'].forEach(name => {
      dropzone.addEventListener(name, (e) => {
        e.preventDefault();
        dropzone.classList.remove('dragover');
      });
    });
    dropzone.addEventListener('drop', (e) => {
      const files = e.dataTransfer.files;
      if (files && files.length > 0) {
        handleFileSelect(files);
      }
    });

    async function handleFileSelect(files) {
      if (!files || files.length === 0) return;
      const progressBox = document.getElementById('progress-box');
      const filenameEl = document.getElementById('progress-filename');
      const speedEl = document.getElementById('progress-speed');
      const fillEl = document.getElementById('progress-fill');
      const bytesEl = document.getElementById('progress-bytes');
      const percentEl = document.getElementById('progress-percent');
      progressBox.style.display = 'block';

      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        filenameEl.textContent = `Uploading (` + (i + 1) + `/` + files.length + `): ` + file.name;
        fillEl.style.width = '0%';
        percentEl.textContent = '0%';
        await uploadFileWithProgress(file, fillEl, bytesEl, percentEl, speedEl);
      }

      filenameEl.textContent = '✅ All files uploaded successfully!';
      speedEl.textContent = '';
      setTimeout(() => {
        progressBox.style.display = 'none';
        loadFiles();
      }, 2500);
    }

    function uploadFileWithProgress(file, fillEl, bytesEl, percentEl, speedEl) {
      return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        const startTime = Date.now();

        xhr.upload.onprogress = (e) => {
          if (e.lengthComputable) {
            const percent = Math.round((e.loaded / e.total) * 100);
            fillEl.style.width = percent + '%';
            percentEl.textContent = percent + '%';

            const elapsedSec = Math.max(0.001, (Date.now() - startTime) / 1000);
            const speedMb = ((e.loaded / (1024 * 1024)) / elapsedSec).toFixed(1);
            speedEl.textContent = speedMb + ' MB/s';

            const loadedMb = (e.loaded / (1024 * 1024)).toFixed(1);
            const totalMb = (e.total / (1024 * 1024)).toFixed(1);
            bytesEl.textContent = loadedMb + ' / ' + totalMb + ' MB';
          }
        };

        xhr.onload = () => {
          if (xhr.status >= 200 && xhr.status < 300) {
            resolve();
          } else {
            resolve(); // proceed to next file on minor error
          }
        };
        xhr.onerror = () => resolve();

        xhr.open('POST', '/api/upload', true);
        xhr.setRequestHeader('Content-Type', file.type || 'application/octet-stream');
        xhr.setRequestHeader('X-Filename', encodeURIComponent(file.name));
        xhr.send(file);
      });
    }

    function loadFiles() {
      fetch('/api/files')
        .then(res => res.json())
        .then(files => {
          const list = document.getElementById('files-list');
          if (!files || files.length === 0) {
            list.innerHTML = '<div class="empty-state">No files received yet. Send files from phone or upload above!</div>';
            return;
          }
          list.innerHTML = '';
          files.forEach(f => {
            const row = document.createElement('div');
            row.className = 'file-row';
            const icon = getCategoryIcon(f.category);
            row.innerHTML = `
              <div class="file-meta">
                <span class="file-icon">` + icon + `</span>
                <div>
                  <div class="file-name" title="` + f.name + `">` + f.name + `</div>
                  <div class="file-size">` + f.formattedSize + `</div>
                </div>
              </div>
              <a href="` + f.downloadUrl + `" class="dl-btn" download="` + f.name + `">⬇ Download</a>
            `;
            list.appendChild(row);
          });
        })
        .catch(() => {
          document.getElementById('files-list').innerHTML = '<div class="empty-state">Unable to load files list.</div>';
        });
    }

    function getCategoryIcon(cat) {
      switch (cat) {
        case 'Images': return '🖼️';
        case 'Videos': return '🎬';
        case 'Documents': return '📄';
        case 'Audio': return '🎵';
        case 'Archives': return '📦';
        default: return '📁';
      }
    }

    if (isAuthed) {
      loadFiles();
    }
  </script>
</body>
</html>
        """.trimIndent()
    }
}
