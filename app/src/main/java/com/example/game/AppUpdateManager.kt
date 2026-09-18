package com.example.game

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateManager {
  private const val TAG = "AppUpdateManager"
  const val DEFAULT_REPO = "MrFeastProject/CosmicBattle"

  data class UpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val isUpdateAvailable: Boolean,
    val title: String,
    val changelog: String,
    val downloadUrl: String,
    val apkSize: Long = 0L
  )

  sealed class UpdateCheckResult {
    data class Success(val updateInfo: UpdateInfo) : UpdateCheckResult()
    data class NoUpdate(val currentVersion: String, val latestVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
  }

  sealed class DownloadProgress {
    data class Progress(
      val percent: Int,
      val downloadedBytes: Long,
      val totalBytes: Long
    ) : DownloadProgress()
    data class Completed(val apkFile: File) : DownloadProgress()
    data class Failed(val error: String) : DownloadProgress()
  }

  /**
   * Compares two semantic version strings (e.g. "1.0.4" vs "1.0.3").
   * Returns true if remoteVersion is strictly newer than currentVersion.
   */
  fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
    try {
      val remoteClean = remoteVersion.trim().removePrefix("v").removePrefix("V")
      val currentClean = currentVersion.trim().removePrefix("v").removePrefix("V")

      val remoteParts = remoteClean.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }
      val currentParts = currentClean.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }

      val maxLen = maxOf(remoteParts.size, currentParts.size)
      for (i in 0 until maxLen) {
        val r = remoteParts.getOrElse(i) { 0 }
        val c = currentParts.getOrElse(i) { 0 }
        if (r > c) return true
        if (r < c) return false
      }
      return false
    } catch (e: Exception) {
      Log.e(TAG, "Error comparing versions: $remoteVersion vs $currentVersion", e)
      return false
    }
  }

  /**
   * Checks GitHub Releases API and fallback version.json for the latest version.
   */
  suspend fun checkForUpdates(
    currentVersion: String,
    repo: String = DEFAULT_REPO
  ): UpdateCheckResult = withContext(Dispatchers.IO) {
    val cleanRepo = repo.trim().removePrefix("https://github.com/").removeSuffix("/")
    val apiUrl = "https://api.github.com/repos/$cleanRepo/releases/latest"

    Log.i(TAG, "Checking for updates at $apiUrl (Current: $currentVersion)...")

    try {
      val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8000
        readTimeout = 8000
        setRequestProperty("Accept", "application/vnd.github.v3+json")
        setRequestProperty("User-Agent", "CosmicBattle-Android-App")
      }

      val responseCode = connection.responseCode
      if (responseCode in 200..299) {
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val tagName = json.optString("tag_name", "").trim()
        val title = json.optString("name", "Новая версия Cosmic Battle")
        val changelog = json.optString("body", "Улучшения стабильности и новые возможности.")
        
        var downloadUrl = ""
        var apkSize = 0L

        // Look for .apk file in assets
        val assets = json.optJSONArray("assets") ?: JSONArray()
        for (i in 0 until assets.length()) {
          val asset = assets.getJSONObject(i)
          val name = asset.optString("name", "")
          if (name.endsWith(".apk", ignoreCase = true)) {
            downloadUrl = asset.optString("browser_download_url", "")
            apkSize = asset.optLong("size", 0L)
            break
          }
        }

        // Fallback standard release APK URL if not listed directly in assets
        if (downloadUrl.isEmpty()) {
          downloadUrl = "https://github.com/$cleanRepo/releases/download/$tagName/CosmicBattle-$tagName.apk"
        }

        val isNew = isNewerVersion(tagName, currentVersion)
        val info = UpdateInfo(
          latestVersion = tagName,
          currentVersion = currentVersion,
          isUpdateAvailable = isNew,
          title = title,
          changelog = changelog,
          downloadUrl = downloadUrl,
          apkSize = apkSize
        )

        return@withContext if (isNew) {
          UpdateCheckResult.Success(info)
        } else {
          UpdateCheckResult.NoUpdate(currentVersion, tagName)
        }
      } else if (responseCode == 404) {
        // Try fallback to raw version.json in the repository main/master branch
        val fallbackResult = checkRawVersionJson(cleanRepo, currentVersion)
        if (fallbackResult != null) {
          return@withContext fallbackResult
        }
        return@withContext UpdateCheckResult.Error("Репозиторий $cleanRepo на GitHub не найден или нет релизов (404)")
      } else {
        return@withContext UpdateCheckResult.Error("Ошибка сервера GitHub: код $responseCode")
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to fetch from GitHub API: ${e.message}. Trying fallback...")
      val fallbackResult = checkRawVersionJson(cleanRepo, currentVersion)
      if (fallbackResult != null) {
        return@withContext fallbackResult
      }
      return@withContext UpdateCheckResult.Error("Не удалось подключиться к GitHub: ${e.localizedMessage ?: "Сетевая ошибка"}")
    }
  }

  private suspend fun checkRawVersionJson(
    repo: String,
    currentVersion: String
  ): UpdateCheckResult? = withContext(Dispatchers.IO) {
    val branches = listOf("main", "master")
    for (branch in branches) {
      try {
        val url = "https://raw.githubusercontent.com/$repo/$branch/version.json"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
          connectTimeout = 6000
          readTimeout = 6000
          setRequestProperty("User-Agent", "CosmicBattle-Android-App")
        }
        if (connection.responseCode in 200..299) {
          val text = connection.inputStream.bufferedReader().use { it.readText() }
          val json = JSONObject(text)
          val ver = json.optString("version", "").trim()
          val title = json.optString("title", "Обновление $ver")
          val changelog = json.optString("changelog", "Новая версия Cosmic Battle")
          val downloadUrl = json.optString("downloadUrl", "https://github.com/$repo/releases/download/$ver/CosmicBattle.apk")
          val isNew = isNewerVersion(ver, currentVersion)

          val info = UpdateInfo(
            latestVersion = ver,
            currentVersion = currentVersion,
            isUpdateAvailable = isNew,
            title = title,
            changelog = changelog,
            downloadUrl = downloadUrl
          )
          return@withContext if (isNew) UpdateCheckResult.Success(info) else UpdateCheckResult.NoUpdate(currentVersion, ver)
        }
      } catch (_: Exception) {}
    }
    null
  }

  /**
   * Downloads the update APK with progress reporting and redirect support (handles GitHub/AWS redirects).
   */
  suspend fun downloadApk(
    context: Context,
    downloadUrl: String,
    onProgress: (DownloadProgress) -> Unit
  ) = withContext(Dispatchers.IO) {
    try {
      val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
      val targetApk = File(updateDir, "CosmicBattle-update.apk")
      if (targetApk.exists()) {
        targetApk.delete()
      }

      var currentUrl = downloadUrl
      var connection: HttpURLConnection
      var redirectCount = 0
      val maxRedirects = 6

      while (true) {
        val url = URL(currentUrl)
        connection = (url.openConnection() as HttpURLConnection).apply {
          instanceFollowRedirects = true
          connectTimeout = 15000
          readTimeout = 20000
          setRequestProperty("User-Agent", "CosmicBattle-Android-App")
        }

        val status = connection.responseCode
        if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
            status == HttpURLConnection.HTTP_MOVED_PERM ||
            status == HttpURLConnection.HTTP_SEE_OTHER ||
            status == 307 || status == 308) {
          val newUrl = connection.getHeaderField("Location")
          if (newUrl != null && redirectCount < maxRedirects) {
            redirectCount++
            currentUrl = newUrl
            connection.disconnect()
            continue
          }
        }
        break
      }

      if (connection.responseCode !in 200..299) {
        onProgress(DownloadProgress.Failed("Ошибка загрузки файла: HTTP ${connection.responseCode}"))
        return@withContext
      }

      val fileLength = connection.contentLength.toLong()
      val input = BufferedInputStream(connection.inputStream, 8192)
      val output = FileOutputStream(targetApk)

      val data = ByteArray(8192)
      var totalDownloaded: Long = 0
      var lastReportPercent = -1
      var count: Int

      while (input.read(data).also { count = it } != -1) {
        output.write(data, 0, count)
        totalDownloaded += count

        val percent = if (fileLength > 0) {
          ((totalDownloaded * 100) / fileLength).toInt().coerceIn(0, 100)
        } else {
          // If content length unknown, report gradual progress
          (totalDownloaded / (100 * 1024)).toInt().coerceIn(0, 95)
        }

        if (percent != lastReportPercent) {
          lastReportPercent = percent
          onProgress(DownloadProgress.Progress(percent, totalDownloaded, fileLength))
        }
      }

      output.flush()
      output.close()
      input.close()
      connection.disconnect()

      if (targetApk.length() == 0L) {
        onProgress(DownloadProgress.Failed("Загруженный файл пуст"))
      } else {
        onProgress(DownloadProgress.Completed(targetApk))
      }
    } catch (e: Exception) {
      Log.e(TAG, "Download failed: ${e.message}", e)
      onProgress(DownloadProgress.Failed(e.localizedMessage ?: "Сбой при скачивании обновления"))
    }
  }

  /**
   * Installs the downloaded APK using Android PackageInstaller via FileProvider.
   * If permission to install unknown apps is missing on Android 8+, prompts settings.
   */
  fun installApk(context: Context, apkFile: File): Boolean {
    if (!apkFile.exists() || apkFile.length() == 0L) {
      Log.e(TAG, "Cannot install: APK file does not exist or is empty.")
      return false
    }

    try {
      // Check unknown sources permission on Android 8.0+
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!context.packageManager.canRequestPackageInstalls()) {
          val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(settingsIntent)
          // We also still proceed to show the install intent
        }
      }

      val apkUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile
      )

      val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

      context.startActivity(installIntent)
      return true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
      return false
    }
  }

  /**
   * Creates a self-test update APK from the running app's APK.
   * Useful for testing the exact download progress bar and re-install cycle
   * even before a new release is pushed to GitHub!
   */
  fun createSelfTestApk(context: Context): File? {
    return try {
      val sourceDir = context.applicationInfo.sourceDir ?: return null
      val sourceApk = File(sourceDir)
      if (!sourceApk.exists()) return null

      val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
      val target = File(updateDir, "CosmicBattle-self-test.apk")
      sourceApk.copyTo(target, overwrite = true)
      target
    } catch (e: Exception) {
      Log.e(TAG, "Failed to create self-test APK: ${e.message}")
      null
    }
  }

  fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 Б"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
      gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f ГБ", gb)
      mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f МБ", mb)
      kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f КБ", kb)
      else -> "$bytes Б"
    }
  }
}
