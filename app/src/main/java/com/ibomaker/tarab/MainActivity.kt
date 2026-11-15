package com.ibomaker.tarab

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import org.json.JSONArray
import org.json.JSONTokener

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var content: FrameLayout
    private var browser: WebView? = null
    private var address: EditText? = null
    private var videoPlayer: VideoView? = null
    private var currentTab = "home"

    private val bg = 0xFF101114.toInt()
    private val panel = 0xFF202329.toInt()
    private val accent = 0xFFE0B45B.toInt()
    private val white = 0xFFF5F5F5.toInt()
    private val muted = 0xFFB5B7BD.toInt()
    private val pickFile = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildShell()
        showHome()
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 73)
        }
    }

    private fun buildShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF191B20.toInt())
            gravity = Gravity.CENTER
        }
        listOf(
            "الرئيسية" to "home",
            "المتصفح" to "browser",
            "التنزيلات" to "downloads",
            "مكتبتي" to "library",
            "الإعدادات" to "settings"
        ).forEach { (label, tab) ->
            val b = TextView(this).apply {
                text = label
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(if (tab == currentTab) accent else muted)
                setPadding(2, 14, 2, 14)
                setOnClickListener { selectTab(tab) }
            }
            nav.addView(b, LinearLayout.LayoutParams(0, 56, 1f))
        }
        root.addView(nav)
        setContentView(root)
    }

    private fun selectTab(tab: String) {
        currentTab = tab
        when (tab) {
            "home" -> showHome()
            "browser" -> showBrowser()
            "downloads" -> showDownloads()
            "library" -> showLibrary()
            "settings" -> showSettings()
        }
    }

    private fun heading(title: String, subtitle: String? = null): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 22, 20, 12)
        }
        box.addView(TextView(this).apply {
            text = title
            textSize = 26f
            setTextColor(white)
        })
        if (subtitle != null) box.addView(TextView(this).apply {
            text = subtitle
            textSize = 13f
            setTextColor(muted)
            setPadding(0, 5, 0, 0)
        })
        return box
    }

    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(white)
            setOnClickListener { action() }
        }

    private fun card(title: String, description: String, action: () -> Unit): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            setBackgroundColor(panel)
            setOnClickListener { action() }
        }
        box.addView(TextView(this).apply {
            text = title
            textSize = 17f
            setTextColor(white)
        })
        box.addView(TextView(this).apply {
            text = description
            textSize = 12f
            setTextColor(muted)
            setPadding(0, 5, 0, 0)
        })
        return box
    }

    private fun showHome() {
        content.removeAllViews()
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 10, 18, 18)
        }
        page.addView(heading("طرب", "استمع وشاهد وأدر ملفاتك في مكان واحد"))
        page.addView(card("تصفح الإنترنت", "افتح المواقع وابحث عن المحتوى", ::showBrowser))
        addGap(page, 12)
        page.addView(card("التنزيلات", "تابع الملفات التي نزّلتها", ::showDownloads))
        addGap(page, 12)
        page.addView(card("مكتبتي", "افتح ملفات الصوت والفيديو من جهازك", ::showLibrary))
        addGap(page, 12)
        page.addView(TextView(this).apply {
            text = "ملاحظة: يعتمد تنزيل الوسائط على إتاحة رابط مباشر وسياسة الموقع. لا يمكن تجاوز DRM أو قيود المنصات."
            textSize = 12f
            setTextColor(muted)
            setPadding(4, 12, 4, 0)
        })
        content.addView(page)
    }

    private fun addGap(parent: LinearLayout, height: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, height))
    }

    private fun showBrowser() {
        content.removeAllViews()
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 6, 8, 6)
            gravity = Gravity.CENTER_VERTICAL
        }
        address = EditText(this).apply {
            hint = "ابحث أو أدخل رابطاً"
            singleLine = true
            textSize = 13f
            setTextColor(white)
            setHintTextColor(muted)
            setBackgroundColor(panel)
            setPadding(12, 0, 12, 0)
        }
        toolbar.addView(address, LinearLayout.LayoutParams(0, 48, 1f))
        toolbar.addView(button("فتح") { navigate(address?.text?.toString().orEmpty()) })
        toolbar.addView(button("وسائط") { inspectMedia() })
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }
        val web = browser ?: WebView(this).also { browser = it }
        configureBrowser(web, progress)
        (web.parent as? ViewGroup)?.removeView(web)
        page.addView(toolbar)
        page.addView(progress, LinearLayout.LayoutParams(-1, 3))
        page.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(page)
    }

    private fun configureBrowser(web: WebView, progress: ProgressBar) {
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.loadsImagesAutomatically = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.settings.javaScriptCanOpenWindowsAutomatically = false
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = true
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, value: Int) {
                progress.progress = value
                progress.visibility = if (value in 1..99) View.VISIBLE else View.GONE
            }
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
            override fun onPageFinished(view: WebView?, url: String?) {
                if (currentTab == "browser") address?.setText(url.orEmpty())
            }
        }
        web.setDownloadListener(DownloadListener { url, userAgent, disposition, mimeType, _ ->
            enqueueDownload(url, userAgent, disposition, mimeType)
        })
        if (web.url == null) web.loadUrl("https://www.google.com")
    }

    private fun navigate(raw: String) {
        val value = raw.trim()
        if (value.isEmpty()) return
        val url = when {
            value.startsWith("https://") || value.startsWith("http://") -> value
            value.contains(".") && !value.contains(" ") -> "https://$value"
            else -> "https://www.google.com/search?q=" + Uri.encode(value)
        }
        browser?.loadUrl(url) ?: showBrowser().also { browser?.loadUrl(url) }
    }

    private fun inspectMedia() {
        val web = browser ?: return
        val script = """
            (function() {
              const found = new Map();
              document.querySelectorAll('video,audio,source').forEach(e => {
                const u = e.currentSrc || e.src;
                if (u && /^https?:/.test(u)) found.set(u, e.getAttribute('title') || e.getAttribute('aria-label') || u);
              });
              document.querySelectorAll('a[href]').forEach(a => {
                const u = a.href;
                if (/\.(mp4|m4v|webm|mp3|m4a|aac|wav|ogg)(\?|$)/i.test(u)) found.set(u, a.innerText || u);
              });
              return JSON.stringify(Array.from(found, ([url,title]) => ({url:url,title:title})));
            })();
        """.trimIndent()
        web.evaluateJavascript(script) { result ->
            try {
                val decoded = JSONTokener(result ?: "null").nextValue() as? String
                val items = JSONArray(decoded ?: "[]")
                if (items.length() == 0) {
                    Toast.makeText(this, "لم يعثر المتصفح على روابط وسائط مباشرة في هذه الصفحة", Toast.LENGTH_LONG).show()
                    return@evaluateJavascript
                }
                val labels = Array(items.length()) { i ->
                    val obj = items.getJSONObject(i)
                    obj.optString("title", obj.optString("url"))
                }
                android.app.AlertDialog.Builder(this)
                    .setTitle("الوسائط المتاحة في الصفحة")
                    .setItems(labels) { _, which ->
                        val obj = items.getJSONObject(which)
                        enqueueDownload(obj.getString("url"), null, null, null)
                    }
                    .setNegativeButton("إغلاق", null)
                    .show()
            } catch (_: Exception) {
                Toast.makeText(this, "تعذر قراءة روابط الوسائط من الصفحة", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun enqueueDownload(url: String, userAgent: String?, disposition: String?, mimeType: String?) {
        try {
            val name = URLUtil.guessFileName(url, disposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(name)
                .setDescription("طرب أندرويد")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            if (!mimeType.isNullOrBlank()) request.setMimeType(mimeType)
            if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
            val cookie = CookieManager.getInstance().getCookie(url)
            if (!cookie.isNullOrBlank()) request.addRequestHeader("Cookie", cookie)
            val id = (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(this, "بدأ التنزيل رقم $id", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "فشل بدء التنزيل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDownloads() {
        content.removeAllViews()
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 8, 18, 12)
        }
        page.addView(heading("التنزيلات", "الملفات التي يديرها نظام أندرويد"))
        page.addView(button("تحديث القائمة") { showDownloads() })
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = manager.query(DownloadManager.Query())
        var count = 0
        cursor.use {
            val titleCol = it.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val statusCol = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val reasonCol = it.getColumnIndex(DownloadManager.COLUMN_REASON)
            val idCol = it.getColumnIndex(DownloadManager.COLUMN_ID)
            while (it.moveToNext()) {
                count++
                val title = if (titleCol >= 0) it.getString(titleCol) ?: "ملف" else "ملف"
                val status = if (statusCol >= 0) it.getInt(statusCol) else -1
                val reason = if (reasonCol >= 0) it.getInt(reasonCol) else 0
                val id = if (idCol >= 0) it.getLong(idCol) else -1L
                val localUriCol = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val mediaTypeCol = it.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)
                val localUri = if (localUriCol >= 0) it.getString(localUriCol) else null
                val mediaType = if (mediaTypeCol >= 0) it.getString(mediaTypeCol) else null
                val state = when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> "اكتمل"
                    DownloadManager.STATUS_FAILED -> "فشل ($reason)"
                    DownloadManager.STATUS_RUNNING -> "جارٍ التنزيل"
                    DownloadManager.STATUS_PENDING -> "بانتظار البدء"
                    DownloadManager.STATUS_PAUSED -> "متوقف مؤقتاً"
                    else -> "حالة غير معروفة"
                }
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                row.addView(card(title, state) {
                    if (status == DownloadManager.STATUS_SUCCESSFUL && !localUri.isNullOrBlank()) {
                        openDownloadedFile(Uri.parse(localUri), mediaType)
                    } else Toast.makeText(this, "معرّف التنزيل: $id", Toast.LENGTH_SHORT).show()
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(button("حذف") {
                    manager.remove(id)
                    showDownloads()
                })
                list.addView(row)
                addGap(list, 8)
            }
        }
        if (count == 0) list.addView(TextView(this).apply {
            text = "لا توجد تنزيلات بعد"
            setTextColor(muted)
            setPadding(8, 20, 8, 20)
        })
        scroll.addView(list)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(page)
    }

    private fun openDownloadedFile(uri: Uri, mimeType: String?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType?.takeIf { it.isNotBlank() } ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "لا يوجد تطبيق مناسب لفتح هذا الملف", Toast.LENGTH_LONG).show()
        }
    }

    private fun showLibrary() {
        content.removeAllViews()
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 8, 18, 18)
        }
        page.addView(heading("مكتبتي", "افتح ملفاتك الصوتية والمرئية من الجهاز"))
        page.addView(card("فتح ملف صوتي", "MP3 و M4A و WAV والصيغ التي يدعمها الجهاز") {
            chooseFile("audio/*")
        })
        addGap(page, 12)
        page.addView(card("فتح فيديو", "تشغيل ملف فيديو محلي") {
            chooseFile("video/*")
        })
        addGap(page, 12)
        page.addView(card("مجلد التنزيلات", "عرض الملفات التي نزّلتها") {
            try { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
            catch (_: Exception) { }
        })
        content.addView(page)
    }

    private fun chooseFile(type: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            this.type = type
        }
        startActivityForResult(intent, pickFile)
    }

    @Deprecated("Use Activity Result APIs in a future update")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickFile || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val mime = contentResolver.getType(uri).orEmpty()
        try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
        if (mime.startsWith("video/")) playVideo(uri) else playAudio(uri)
    }

    private fun playAudio(uri: Uri) {
        content.removeAllViews()
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
        }
        page.addView(TextView(this).apply {
            text = "مشغل الصوت"
            textSize = 25f
            setTextColor(white)
        })
        page.addView(TextView(this).apply {
            text = uri.lastPathSegment ?: "ملف صوتي"
            setTextColor(muted)
            setPadding(0, 12, 0, 20)
        })
        page.addView(button("تشغيل في الخلفية") {
            val intent = Intent(this, AudioPlaybackService::class.java).apply {
                action = AudioPlaybackService.ACTION_PLAY
                putExtra(AudioPlaybackService.EXTRA_URI, uri.toString())
            }
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        })
        page.addView(button("إيقاف مؤقت") {
            startService(Intent(this, AudioPlaybackService::class.java).setAction(AudioPlaybackService.ACTION_PAUSE))
        })
        page.addView(button("استئناف") {
            startService(Intent(this, AudioPlaybackService::class.java).setAction(AudioPlaybackService.ACTION_RESUME))
        })
        page.addView(button("إيقاف") {
            startService(Intent(this, AudioPlaybackService::class.java).setAction(AudioPlaybackService.ACTION_STOP))
        })
        content.addView(page)
    }

    private fun playVideo(uri: Uri) {
        releasePlayers()
        content.removeAllViews()
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 18, 12, 12)
        }
        page.addView(heading("مشغل الفيديو"))
        val video = VideoView(this)
        videoPlayer = video
        video.setMediaController(android.widget.MediaController(this))
        video.setVideoURI(uri)
        video.setOnErrorListener { _, _, _ ->
            Toast.makeText(this, "تعذر تشغيل صيغة هذا الفيديو", Toast.LENGTH_LONG).show()
            true
        }
        page.addView(video, LinearLayout.LayoutParams(-1, 0, 1f))
        page.addView(button("تشغيل الفيديو") { video.start() })
        content.addView(page)
    }

    private fun showSettings() {
        content.removeAllViews()
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 8, 18, 18)
        }
        page.addView(heading("الإعدادات", "طرب أندرويد"))
        page.addView(card("حول التطبيق", "نسخة أندرويد مستقلة قيد التطوير") {
            android.app.AlertDialog.Builder(this)
                .setTitle("طرب أندرويد")
                .setMessage("متصفح وتنزيلات مباشرة ومشغّل للملفات المحلية. دعم التنزيل يختلف حسب الموقع والصيغة.")
                .setPositiveButton("حسناً", null).show()
        })
        addGap(page, 10)
        page.addView(card("مسح بيانات التصفح", "حذف سجل وذاكرة المتصفح المؤقتة") {
            browser?.clearHistory()
            browser?.clearCache(true)
            Toast.makeText(this, "تم مسح سجل وذاكرة المتصفح", Toast.LENGTH_SHORT).show()
        })
        addGap(page, 10)
        page.addView(card("سياسة التنزيل", "لا يتم تجاوز DRM أو أنظمة الحماية أو قيود المنصات") {})
        content.addView(page)
    }

    private fun releasePlayers() {
        try { videoPlayer?.stopPlayback() } catch (_: Exception) { }
        videoPlayer = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        browser?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (currentTab == "browser" && browser?.canGoBack() == true) browser?.goBack()
        else if (currentTab != "home") selectTab("home")
        else super.onBackPressed()
    }

    override fun onDestroy() {
        releasePlayers()
        browser?.stopLoading()
        browser?.destroy()
        browser = null
        super.onDestroy()
    }
}
