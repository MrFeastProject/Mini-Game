package com.example.game

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import com.example.R
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CosmicDark
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarSilver
import com.example.ui.theme.StarWhite
import kotlinx.coroutines.delay

private const val GAME_URL = "file:///android_asset/game/index.html"
private const val TAG = "CosmicBattle"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CosmicBattleScreen(
  onExitApp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val gamePreferences = remember { GamePreferences(context) }

  var webViewInstance by remember { mutableStateOf<WebView?>(null) }
  var reloadTrigger by remember { mutableIntStateOf(0) }
  var isLoading by remember { mutableStateOf(true) }
  var loadingProgress by remember { mutableIntStateOf(0) }
  var hasError by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  var showExitDialog by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showClearDataDialog by remember { mutableStateOf(false) }

  var currentFps by remember { mutableIntStateOf(gamePreferences.fpsLimit) }
  var isKeepScreenOn by remember { mutableStateOf(gamePreferences.isKeepScreenOn) }
  var isImmersive by remember { mutableStateOf(gamePreferences.isImmersive) }
  var isNotificationsEnabled by remember { mutableStateOf(gamePreferences.isNotificationsEnabled) }

  // Custom Fullscreen View support (WebChromeClient)
  var customView by remember { mutableStateOf<View?>(null) }
  var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

  // Notification permission launcher for Android 13+ (API 33+)
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      Toast.makeText(context, "Уведомления включены!", Toast.LENGTH_SHORT).show()
      NotificationHelper.sendBattleNotification(
        context,
        context.getString(R.string.test_notification_title),
        context.getString(R.string.test_notification_body)
      )
    } else {
      Toast.makeText(context, "Разрешение на уведомления отклонено", Toast.LENGTH_SHORT).show()
      isNotificationsEnabled = false
      gamePreferences.isNotificationsEnabled = false
    }
  }

  // Universal Anti-Hang Watchdog (Samsung One UI, Xiaomi, Pixel):
  // 1. Ensures the loading splash NEVER freezes or blocks the game.
  // 2. Automatically skips the 5.5s intro if it gets stuck, launching directly into the menu.
  LaunchedEffect(reloadTrigger) {
    NotificationHelper.createNotificationChannel(context)
    delay(300)
    isLoading = false

    // Skip intro if still visible after 1.2s to guarantee smooth game launch
    delay(900)
    webViewInstance?.evaluateJavascript(
      """
      (function() {
        try {
          if (typeof skipIntroNow === 'function') {
            skipIntroNow();
          } else {
            var intro = document.getElementById('intro');
            if (intro && !intro.classList.contains('hidden')) {
              intro.classList.add('hidden');
              if (typeof showMenu === 'function') showMenu();
            }
          }
        } catch(e) {}
      })();
      """.trimIndent(),
      null
    )
  }

  // Handle keep screen on
  DisposableEffect(isKeepScreenOn) {
    if (isKeepScreenOn) {
      activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    onDispose {
      activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }

  // Handle Fullscreen Immersive Mode
  LaunchedEffect(isImmersive) {
    activity?.let { act ->
      val window = act.window
      val insetsController = WindowCompat.getInsetsController(window, window.decorView)
      if (isImmersive) {
        insetsController.systemBarsBehavior =
          WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
      } else {
        insetsController.show(WindowInsetsCompat.Type.systemBars())
      }
    }
  }

  // Apply FPS changes dynamically
  LaunchedEffect(currentFps) {
    gamePreferences.fpsLimit = currentFps
    gamePreferences.applyFpsSettings(activity, webViewInstance)
  }

  // Handle Android Back Navigation
  BackHandler(enabled = true) {
    val wv = webViewInstance
    if (customView != null) {
      customViewCallback?.onCustomViewHidden()
    } else if (wv != null && wv.canGoBack()) {
      wv.goBack()
    } else {
      showExitDialog = true
    }
  }

  // Ensure cookies flushed on disposal
  DisposableEffect(Unit) {
    onDispose {
      CookieManager.getInstance().flush()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(CosmicDark)
  ) {
    // Custom View for HTML5 Fullscreen (if game triggers it)
    if (customView != null) {
      AndroidView(
        factory = {
          FrameLayout(it).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(customView)
          }
        },
        modifier = Modifier.fillMaxSize()
      )
    } else {
      // Main WebView container
      androidx.compose.runtime.key(reloadTrigger) {
        AndroidView(
          modifier = Modifier
            .fillMaxSize()
            .testTag("game_webview"),
          factory = { ctx ->
          WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Cosmic dark background: NEVER flashes white on render/redraw
            setBackgroundColor(android.graphics.Color.parseColor("#030308"))
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()

            // Setup Cookies & Local Storage persistence
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            // Native Android Bridge for haptic vibrations and device capabilities
            addJavascriptInterface(AndroidBridge(ctx), "AndroidBridge")

            // WebSettings configuration tuned for ALL devices & Android versions (Android 7 - 16)
            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true // HTML5 LocalStorage & SessionStorage persistence
              databaseEnabled = true // IndexedDB & WebSQL persistence
              cacheMode = WebSettings.LOAD_DEFAULT // Fast local disk asset caching
              allowFileAccess = true
              allowContentAccess = true
              allowFileAccessFromFileURLs = true
              allowUniversalAccessFromFileURLs = true
              mediaPlaybackRequiresUserGesture = false // Game sounds play without gesture block
              useWideViewPort = true
              loadWithOverviewMode = true
              builtInZoomControls = false
              displayZoomControls = false
              setSupportZoom(false) // Passes all touches directly to game canvas
              mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
              loadsImagesAutomatically = true
              blockNetworkImage = false

              // Critical fix for Samsung One UI & Android 10+:
              // Prevents system Dark Mode from inverting dark HTML canvas to white!
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                forceDark = WebSettings.FORCE_DARK_OFF
              }

              // Fix text zooming on devices with custom DPI / font scaling
              textZoom = 100

              // Safe browsing can hang on github.io on some mobile networks
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = false
              }
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                offscreenPreRaster = true
              }

              // Clean standard Chrome mobile User-Agent (avoids restrictions triggered by "; wv")
              val defaultUa = userAgentString
              if (defaultUa.contains("; wv")) {
                userAgentString = defaultUa.replace("; wv", "").replace("Version/4.0 ", "")
              }
            }

            webChromeClient = object : WebChromeClient() {
              override fun onProgressChanged(view: WebView?, newProgress: Int) {
                loadingProgress = newProgress
                if (newProgress >= 85) {
                  isLoading = false
                }
              }

              override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                  Log.d(TAG, "[WebView Console] ${it.message()} -- line ${it.lineNumber()}")
                }
                return true
              }

              override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                customView = view
                customViewCallback = callback
              }

              override fun onHideCustomView() {
                customView = null
                customViewCallback = null
              }
            }

            webViewClient = object : WebViewClient() {
              override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                hasError = false
              }

              // As soon as the first frame renders, dismiss loading immediately!
              override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                isLoading = false
                injectPageEnhancements(view, activity, gamePreferences)
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoading = false
                CookieManager.getInstance().flush()
                injectPageEnhancements(view, activity, gamePreferences)
              }

              // Robust renderer recovery: if OS kills renderer due to low RAM on any phone, reload cleanly
              override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
              ): Boolean {
                Log.w(TAG, "WebView RenderProcessGone! Did crash: ${detail?.didCrash()}")
                view?.destroy()
                webViewInstance = null
                reloadTrigger++
                return true
              }

              override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
              ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                  // Only treat real fatal failures as error
                  val code = error?.errorCode ?: 0
                  if (code != ERROR_FILE_NOT_FOUND) {
                    hasError = true
                    isLoading = false
                    errorMessage = error?.description?.toString() ?: "Network Connection Error"
                  }
                }
              }

              override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
              ): Boolean {
                return false
              }
            }

            loadUrl(GAME_URL)
            webViewInstance = this
          }
        },
        update = { webView ->
          webViewInstance = webView
        }
      )
    }
  }

    // Top Neon Loading Progress Indicator
    if (isLoading && !hasError) {
      LinearProgressIndicator(
        progress = { (loadingProgress.coerceAtLeast(10)) / 100f },
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp)
          .align(Alignment.TopCenter),
        color = NeonCyan,
        trackColor = CosmicDark.copy(alpha = 0.5f)
      )
    }

    // Splash / Initial Loading Overlay with Skip Button
    AnimatedVisibility(
      visible = isLoading && !hasError,
      enter = fadeIn(),
      exit = fadeOut(animationSpec = tween(durationMillis = 250)),
      modifier = Modifier.fillMaxSize()
    ) {
      CosmicLoadingOverlay(
        progress = loadingProgress,
        onSkip = {
          isLoading = false
          webViewInstance?.let { wv ->
            wv.evaluateJavascript(
              """
              (function() {
                var intro = document.getElementById('intro');
                if (intro) { intro.classList.add('hidden'); }
                if (typeof showMenu === 'function') { showMenu(); }
              })();
              """.trimIndent(),
              null
            )
          }
        }
      )
    }

    // Error Overlay if offline / failed to load
    if (hasError) {
      CosmicErrorOverlay(
        errorMessage = errorMessage,
        onRetry = {
          hasError = false
          isLoading = true
          reloadTrigger++
        }
      )
    }

    // Floating HUD Settings Button (sleek, semi-transparent, unobtrusive)
    Box(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 16.dp, end = 12.dp)
    ) {
      IconButton(
        onClick = { showSettingsDialog = true },
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(CosmicSurface.copy(alpha = 0.75f))
          .border(1.dp, NeonCyan.copy(alpha = 0.4f), CircleShape)
          .testTag("floating_menu_button")
      ) {
        Icon(
          imageVector = Icons.Default.Settings,
          contentDescription = "Cosmic Settings",
          tint = NeonCyan,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    // Full Game Settings Dialog
    if (showSettingsDialog) {
      CosmicSettingsDialog(
        currentFps = currentFps,
        isImmersive = isImmersive,
        isKeepScreenOn = isKeepScreenOn,
        isNotificationsEnabled = isNotificationsEnabled,
        onSelectFps = { fps ->
          currentFps = fps
          gamePreferences.fpsLimit = fps
          gamePreferences.applyFpsSettings(activity, webViewInstance)
          Toast.makeText(context, "FPS лимит: $fps FPS", Toast.LENGTH_SHORT).show()
        },
        onToggleImmersive = {
          val newVal = !isImmersive
          isImmersive = newVal
          gamePreferences.isImmersive = newVal
        },
        onToggleKeepScreenOn = {
          val newVal = !isKeepScreenOn
          isKeepScreenOn = newVal
          gamePreferences.isKeepScreenOn = newVal
        },
        onToggleNotifications = {
          val newVal = !isNotificationsEnabled
          isNotificationsEnabled = newVal
          gamePreferences.isNotificationsEnabled = newVal
          if (newVal) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              if (ContextCompat.checkSelfPermission(
                  context,
                  Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
              ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              } else {
                Toast.makeText(context, "Уведомления активны", Toast.LENGTH_SHORT).show()
              }
            }
          }
        },
        onSendTestNotification = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
              context,
              Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
          ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          } else {
            val sent = NotificationHelper.sendBattleNotification(
              context,
              context.getString(R.string.test_notification_title),
              context.getString(R.string.test_notification_body)
            )
            if (sent) {
              Toast.makeText(context, "Уведомление отправлено!", Toast.LENGTH_SHORT).show()
            } else {
              Toast.makeText(context, "Проверьте разрешения на уведомления", Toast.LENGTH_SHORT).show()
            }
          }
        },
        onReload = {
          showSettingsDialog = false
          hasError = false
          isLoading = true
          reloadTrigger++
        },
        onClearData = {
          showSettingsDialog = false
          showClearDataDialog = true
        },
        onDismiss = { showSettingsDialog = false }
      )
    }

    // Exit Game Confirmation Dialog
    if (showExitDialog) {
      AlertDialog(
        onDismissRequest = { showExitDialog = false },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.RocketLaunch,
              contentDescription = null,
              tint = NeonCyan,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.exit_title),
              color = StarWhite,
              fontWeight = FontWeight.Bold
            )
          }
        },
        text = {
          Text(
            text = stringResource(R.string.exit_message),
            color = StarSilver
          )
        },
        containerColor = CosmicSurface,
        confirmButton = {
          Button(
            onClick = {
              showExitDialog = false
              onExitApp()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = AlertRed,
              contentColor = StarWhite
            ),
            modifier = Modifier.testTag("confirm_exit_button")
          ) {
            Text(stringResource(R.string.exit_confirm))
          }
        },
        dismissButton = {
          TextButton(
            onClick = { showExitDialog = false },
            modifier = Modifier.testTag("cancel_exit_button")
          ) {
            Text(stringResource(R.string.cancel), color = NeonCyan)
          }
        }
      )
    }

    // Clear Game Storage / Cache Confirmation Dialog
    if (showClearDataDialog) {
      AlertDialog(
        onDismissRequest = { showClearDataDialog = false },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = AlertRed,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Очистить данные игры?",
              color = StarWhite,
              fontWeight = FontWeight.Bold
            )
          }
        },
        text = {
          Text(
            text = "Это удалит все локальные сохранения, очки и кэшированные файлы игры в WebView.",
            color = StarSilver
          )
        },
        containerColor = CosmicSurface,
        confirmButton = {
          Button(
            onClick = {
              showClearDataDialog = false
              webViewInstance?.let { wv ->
                wv.clearCache(true)
                wv.clearFormData()
                wv.clearHistory()
                android.webkit.WebStorage.getInstance().deleteAllData()
                CookieManager.getInstance().removeAllCookies(null)
                reloadTrigger++
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = AlertRed,
              contentColor = StarWhite
            )
          ) {
            Text("Очистить и перезапустить")
          }
        },
        dismissButton = {
          TextButton(onClick = { showClearDataDialog = false }) {
            Text("Отмена", color = NeonCyan)
          }
        }
      )
    }
  }
}

/**
 * Injects non-intrusive runtime fixes:
 * 1. Makes the 5.5s intro skippable immediately upon tap.
 * 2. Neutralizes any Telegram API exceptions when running standalone on Android.
 * 3. Applies FPS limiter safely.
 */
private fun injectPageEnhancements(
  view: WebView?,
  activity: Activity?,
  prefs: GamePreferences
) {
  if (view == null) return

  val script = """
    (function() {
      try {
        // 1. Make intro skippable on click or tap
        var intro = document.getElementById('intro');
        if (intro && !window.__introSkipAttached) {
          window.__introSkipAttached = true;
          var skipIntro = function() {
            intro.classList.add('hidden');
            if (typeof showMenu === 'function') {
              showMenu();
            }
          };
          intro.addEventListener('pointerdown', skipIntro, { once: true });
          intro.addEventListener('click', skipIntro, { once: true });
        }

        // 2. Mock safe Telegram WebApp methods so it never throws on clicks outside Telegram
        if (window.Telegram && window.Telegram.WebApp) {
          var twa = window.Telegram.WebApp;
          if (!twa.requestFullscreen) {
            twa.requestFullscreen = function() {};
          }
          if (!twa.exitFullscreen) {
            twa.exitFullscreen = function() {};
          }
        }
      } catch(e) {
        console.warn('[CosmicBattle] Injection error: ' + e);
      }
    })();
  """.trimIndent()

  view.evaluateJavascript(script, null)
  prefs.applyFpsSettings(activity, view)
}

@Composable
private fun CosmicLoadingOverlay(
  progress: Int,
  onSkip: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "rocket_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.94f,
    targetValue = 1.06f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.25f,
    targetValue = 0.75f,
    animationSpec = infiniteRepeatable(
      animation = tween(1100, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            CosmicDark,
            Color(0xFF0C1126),
            CosmicDark
          )
        )
      ),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(24.dp)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(90.dp)
          .scale(pulseScale)
      ) {
        // Glowing halo
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(NeonCyan.copy(alpha = glowAlpha * 0.35f))
        )
        // Icon container
        Surface(
          modifier = Modifier.size(68.dp),
          shape = CircleShape,
          color = CosmicSurfaceVariant,
          border = androidx.compose.foundation.BorderStroke(2.dp, NeonCyan)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.RocketLaunch,
              contentDescription = "Cosmic Battle Loading",
              tint = NeonCyan,
              modifier = Modifier.size(34.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "COSMIC BATTLE",
        fontSize = 22.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = StarWhite
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Синхронизация звёздного сектора...",
        fontSize = 13.sp,
        color = StarSilver
      )

      Spacer(modifier = Modifier.height(16.dp))

      CircularProgressIndicator(
        modifier = Modifier.size(28.dp),
        color = NeonCyan,
        strokeWidth = 3.dp
      )

      if (progress > 0) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "$progress%",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = NeonPurple
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Instant Skip button so loading NEVER traps the user on any device
      OutlinedButton(
        onClick = onSkip,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = NeonCyan
        ),
        modifier = Modifier.testTag("skip_loading_button")
      ) {
        Text(
          text = stringResource(R.string.skip_loading),
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = Icons.Default.ArrowForward,
          contentDescription = null,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}

@Composable
private fun CosmicErrorOverlay(
  errorMessage: String,
  onRetry: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(CosmicDark.copy(alpha = 0.95f))
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = CosmicSurface,
      border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Surface(
          shape = CircleShape,
          color = AlertRed.copy(alpha = 0.15f),
          modifier = Modifier.size(56.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.WifiOff,
              contentDescription = null,
              tint = AlertRed,
              modifier = Modifier.size(28.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = stringResource(R.string.connection_error_title),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = StarWhite
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = stringResource(R.string.connection_error_desc),
          fontSize = 13.sp,
          color = StarSilver,
          textAlign = TextAlign.Center
        )

        if (errorMessage.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = errorMessage,
            fontSize = 11.sp,
            color = AlertRed.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = onRetry,
          colors = ButtonDefaults.buttonColors(
            containerColor = NeonCyan,
            contentColor = CosmicDark
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("retry_connection_button")
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.retry),
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CosmicSettingsDialog(
  currentFps: Int,
  isImmersive: Boolean,
  isKeepScreenOn: Boolean,
  isNotificationsEnabled: Boolean,
  onSelectFps: (Int) -> Unit,
  onToggleImmersive: () -> Unit,
  onToggleKeepScreenOn: () -> Unit,
  onToggleNotifications: () -> Unit,
  onSendTestNotification: () -> Unit,
  onReload: () -> Unit,
  onClearData: () -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Settings,
          contentDescription = null,
          tint = NeonCyan,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Настройки Cosmic Battle",
          color = StarWhite,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // --- Section: FPS Limit ---
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Ограничение FPS",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = StarWhite
              )
            }
            Text(
              text = "$currentFps FPS",
              color = NeonCyan,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            GamePreferences.FPS_OPTIONS.forEach { fps ->
              val isSelected = fps == currentFps
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) NeonCyan else CosmicSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isSelected) NeonCyan else StarSilver.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                  .clickable { onSelectFps(fps) }
                  .testTag("fps_chip_$fps")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = null,
                      tint = CosmicDark,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                  }
                  Text(
                    text = "$fps",
                    color = if (isSelected) CosmicDark else StarWhite,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp
                  )
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Синхронизирует экран устройства и игровой цикл Canvas",
            fontSize = 11.sp,
            color = StarSilver
          )
        }

        HorizontalDivider(color = CosmicSurfaceVariant)

        // --- Section: Notifications ---
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = NeonPurple,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text(
                  text = "Боевые уведомления",
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 14.sp,
                  color = StarWhite
                )
                Text(
                  text = "Оповещения о событиях флота",
                  fontSize = 11.sp,
                  color = StarSilver
                )
              }
            }
            Switch(
              checked = isNotificationsEnabled,
              onCheckedChange = { onToggleNotifications() },
              colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = CosmicSurfaceVariant,
                uncheckedThumbColor = StarSilver,
                uncheckedTrackColor = CosmicDark
              )
            )
          }

          if (isNotificationsEnabled) {
            OutlinedButton(
              onClick = onSendTestNotification,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("send_test_notification_button"),
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = NeonCyan
              ),
              border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
            ) {
              Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Проверить уведомление", fontSize = 12.sp)
            }
          }
        }

        HorizontalDivider(color = CosmicSurfaceVariant)

        // --- Section: Screen & Immersive Toggles ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (isImmersive) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Полноэкранный режим",
                fontSize = 13.sp,
                color = StarWhite
              )
            }
            Switch(
              checked = isImmersive,
              onCheckedChange = { onToggleImmersive() },
              colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = CosmicSurfaceVariant
              )
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.LockClock,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Экран всегда включен",
                fontSize = 13.sp,
                color = StarWhite
              )
            }
            Switch(
              checked = isKeepScreenOn,
              onCheckedChange = { onToggleKeepScreenOn() },
              colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = CosmicSurfaceVariant
              )
            )
          }
        }

        HorizontalDivider(color = CosmicSurfaceVariant)

        // --- Section: Export / Share APK ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Поделиться игрой / Экспорт APK",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = StarWhite
          )
          Text(
            text = "Отправьте установочный APK друзьям или скачайте в память устройства",
            fontSize = 11.sp,
            color = StarSilver
          )

          Button(
            onClick = { ApkShareHelper.shareApk(context) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("share_apk_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = NeonPurple,
              contentColor = StarWhite
            )
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Поделиться игрой (APK)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = { ApkShareHelper.saveApkToDownloads(context) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("download_apk_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = NeonCyan
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
          ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Скачать APK в Загрузки", fontSize = 13.sp, fontWeight = FontWeight.Medium)
          }
        }

        HorizontalDivider(color = CosmicSurfaceVariant)

        // --- Section: Maintenance Actions ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onReload,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = CosmicSurfaceVariant,
              contentColor = NeonCyan
            )
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Перезагрузить игру", fontSize = 13.sp)
          }

          OutlinedButton(
            onClick = onClearData,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = AlertRed
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f))
          ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Очистить сохранения и кэш", fontSize = 13.sp)
          }
        }
      }
    },
    containerColor = CosmicSurface,
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Закрыть", color = NeonCyan, fontWeight = FontWeight.Bold)
      }
    }
  )
}
