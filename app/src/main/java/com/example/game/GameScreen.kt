package com.example.game

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.R
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CosmicDark
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarSilver
import com.example.ui.theme.StarWhite

private const val GAME_URL = "https://mrfeastproject.github.io/Battle/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CosmicBattleScreen(
  onExitApp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activity = context as? Activity

  var webViewInstance by remember { mutableStateOf<WebView?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  var loadingProgress by remember { mutableIntStateOf(0) }
  var hasError by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  var showExitDialog by remember { mutableStateOf(false) }
  var showMenuDialog by remember { mutableStateOf(false) }
  var showClearDataDialog by remember { mutableStateOf(false) }
  var isKeepScreenOn by remember { mutableStateOf(true) }
  var isImmersive by remember { mutableStateOf(true) }

  // Custom Fullscreen View support (WebChromeClient)
  var customView by remember { mutableStateOf<View?>(null) }
  var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

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

            // Hardware acceleration & focus
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()

            // Setup Cookies & Local Storage persistence
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            // WebSettings configuration for full game data storage and optimal canvas rendering
            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true // LocalStorage & SessionStorage persistence
              databaseEnabled = true // IndexedDB & WebSQL persistence
              cacheMode = WebSettings.LOAD_DEFAULT // Cache game assets locally for fast load
              allowFileAccess = true
              allowContentAccess = true
              mediaPlaybackRequiresUserGesture = false // Enable game audio/SFX without explicit gesture block
              useWideViewPort = true
              loadWithOverviewMode = true
              builtInZoomControls = false
              displayZoomControls = false
              setSupportZoom(false) // Disable pinch zoom so touch events are sent directly to game canvas
              mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }

            webChromeClient = object : WebChromeClient() {
              override fun onProgressChanged(view: WebView?, newProgress: Int) {
                loadingProgress = newProgress
                if (newProgress >= 100) {
                  isLoading = false
                }
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
                isLoading = true
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoading = false
                CookieManager.getInstance().flush()
              }

              override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
              ) {
                super.onReceivedError(view, request, error)
                // Only trigger error screen if it's the main frame
                if (request?.isForMainFrame == true) {
                  hasError = true
                  isLoading = false
                  errorMessage = error?.description?.toString() ?: "Network Connection Error"
                }
              }

              override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
              ): Boolean {
                val url = request?.url?.toString() ?: return false
                // Keep game URLs within the WebView
                return if (url.contains("github.io/Battle") || url.startsWith("https://mrfeastproject.github.io")) {
                  false
                } else {
                  // External links can load inside or outside
                  false
                }
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

    // Top Neon Loading Progress Indicator
    if (isLoading && !hasError) {
      LinearProgressIndicator(
        progress = { loadingProgress / 100f },
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp)
          .align(Alignment.TopCenter),
        color = NeonCyan,
        trackColor = CosmicDark.copy(alpha = 0.5f)
      )
    }

    // Splash / Initial Loading Overlay
    AnimatedVisibility(
      visible = isLoading && loadingProgress < 75 && !hasError,
      enter = fadeIn(),
      exit = fadeOut(animationSpec = tween(durationMillis = 400)),
      modifier = Modifier.fillMaxSize()
    ) {
      CosmicLoadingOverlay(progress = loadingProgress)
    }

    // Error Overlay if offline / failed to load
    if (hasError) {
      CosmicErrorOverlay(
        errorMessage = errorMessage,
        onRetry = {
          hasError = false
          isLoading = true
          webViewInstance?.reload()
        }
      )
    }

    // Subtle Floating HUD Menu Button (does not obstruct game)
    Box(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 16.dp, end = 12.dp)
    ) {
      IconButton(
        onClick = { showMenuDialog = true },
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(CosmicSurface.copy(alpha = 0.7f))
          .border(1.dp, NeonCyan.copy(alpha = 0.35f), CircleShape)
          .testTag("floating_menu_button")
      ) {
        Icon(
          imageVector = Icons.Default.MoreVert,
          contentDescription = "Game Menu",
          tint = NeonCyan,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    // Game Quick Menu Dialog
    if (showMenuDialog) {
      CosmicMenuDialog(
        isImmersive = isImmersive,
        isKeepScreenOn = isKeepScreenOn,
        onToggleImmersive = { isImmersive = !isImmersive },
        onToggleKeepScreenOn = { isKeepScreenOn = !isKeepScreenOn },
        onReload = {
          showMenuDialog = false
          hasError = false
          isLoading = true
          webViewInstance?.reload()
        },
        onClearData = {
          showMenuDialog = false
          showClearDataDialog = true
        },
        onDismiss = { showMenuDialog = false }
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
              text = "Clear Game Data?",
              color = StarWhite,
              fontWeight = FontWeight.Bold
            )
          }
        },
        text = {
          Text(
            text = "This will clear all saved game progress, high scores, and cached battle assets stored in WebView.",
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
                wv.loadUrl(GAME_URL)
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = AlertRed,
              contentColor = StarWhite
            )
          ) {
            Text("Clear & Restart")
          }
        },
        dismissButton = {
          TextButton(onClick = { showClearDataDialog = false }) {
            Text("Cancel", color = NeonCyan)
          }
        }
      )
    }
  }
}

@Composable
private fun CosmicLoadingOverlay(progress: Int) {
  val infiniteTransition = rememberInfiniteTransition(label = "rocket_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.92f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 0.8f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
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
            Color(0xFF0D122B),
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
          .size(100.dp)
          .scale(pulseScale)
      ) {
        // Glowing halo
        Box(
          modifier = Modifier
            .size(90.dp)
            .clip(CircleShape)
            .background(NeonCyan.copy(alpha = glowAlpha * 0.3f))
        )
        // Icon container
        Surface(
          modifier = Modifier.size(72.dp),
          shape = CircleShape,
          color = CosmicSurfaceVariant,
          border = androidx.compose.foundation.BorderStroke(2.dp, NeonCyan)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.RocketLaunch,
              contentDescription = "Cosmic Battle Loading",
              tint = NeonCyan,
              modifier = Modifier.size(36.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "COSMIC BATTLE",
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = StarWhite
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Preparing hyperdrive engines...",
        fontSize = 14.sp,
        color = StarSilver
      )

      Spacer(modifier = Modifier.height(20.dp))

      CircularProgressIndicator(
        modifier = Modifier.size(32.dp),
        color = NeonCyan,
        strokeWidth = 3.dp
      )

      if (progress > 0) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "$progress%",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = NeonPurple
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
          modifier = Modifier.size(64.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.WifiOff,
              contentDescription = null,
              tint = AlertRed,
              modifier = Modifier.size(32.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = stringResource(R.string.connection_error_title),
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = StarWhite
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = stringResource(R.string.connection_error_desc),
          fontSize = 14.sp,
          color = StarSilver,
          textAlign = TextAlign.Center
        )

        if (errorMessage.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = errorMessage,
            fontSize = 12.sp,
            color = AlertRed.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

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

@Composable
private fun CosmicMenuDialog(
  isImmersive: Boolean,
  isKeepScreenOn: Boolean,
  onToggleImmersive: () -> Unit,
  onToggleKeepScreenOn: () -> Unit,
  onReload: () -> Unit,
  onClearData: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.RocketLaunch,
          contentDescription = null,
          tint = NeonCyan,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Cosmic Battle Controls",
          color = StarWhite,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilledTonalButton(
          onClick = onReload,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = CosmicSurfaceVariant,
            contentColor = NeonCyan
          )
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Reload Game")
        }

        FilledTonalButton(
          onClick = onToggleImmersive,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = CosmicSurfaceVariant,
            contentColor = if (isImmersive) NeonCyan else StarSilver
          )
        ) {
          Icon(
            if (isImmersive) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (isImmersive) "Exit Fullscreen" else "Enter Fullscreen")
        }

        FilledTonalButton(
          onClick = onToggleKeepScreenOn,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = CosmicSurfaceVariant,
            contentColor = if (isKeepScreenOn) NeonCyan else StarSilver
          )
        ) {
          Icon(Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (isKeepScreenOn) "Screen: Always On" else "Screen: Auto Timeout")
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
          Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Clear Saved Game Data")
        }
      }
    },
    containerColor = CosmicSurface,
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = NeonCyan)
      }
    }
  )
}
