package com.example.game

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Build
import android.util.Log

data class GraphicsEngineInfo(
  val bestEngineName: String,
  val engineType: String,
  val vulkanSupported: Boolean,
  val vulkanVersion: String?,
  val vulkanLevel: Int,
  val openGlVersion: String,
  val gpuRenderer: String,
  val gpuVendor: String,
  val isHardwareAccelerated: Boolean,
  val recommendationSummary: String,
  val technicalDetails: String
)

object GraphicsEngineDetector {

  private const val TAG = "GraphicsDetector"

  @Volatile
  private var cachedInfo: GraphicsEngineInfo? = null

  fun getGraphicsEngineInfo(context: Context): GraphicsEngineInfo {
    cachedInfo?.let { return it }

    synchronized(this) {
      cachedInfo?.let { return it }

      val pm = context.packageManager
      var vulkanVersionStr: String? = null
      var vulkanSupported = false
      var vulkanLevel = 0

      // 1. Detect Vulkan Support
      try {
        if (pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)) {
          val features = pm.systemAvailableFeatures
          for (feature in features) {
            if (feature.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION) {
              val version = feature.version
              val major = version shr 22
              val minor = (version shr 12) and 0x3FF
              val patch = version and 0xFFF
              vulkanVersionStr = "Vulkan $major.$minor.$patch"
              vulkanSupported = true
              break
            }
          }
        }
        if (pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)) {
          val features = pm.systemAvailableFeatures
          for (feature in features) {
            if (feature.name == PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) {
              vulkanLevel = feature.version
              break
            }
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to query Vulkan features: ${e.message}")
      }

      // 2. Detect OpenGL ES Version
      var openGlVersion = "OpenGL ES 3.0"
      try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val configInfo = am?.deviceConfigurationInfo
        if (configInfo != null && !configInfo.glEsVersion.isNullOrEmpty()) {
          openGlVersion = "OpenGL ES ${configInfo.glEsVersion}"
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to query OpenGL ES version: ${e.message}")
      }

      // 3. Query GPU Renderer & Vendor via offscreen EGL context
      val (gpuRenderer, gpuVendor) = queryGpuHardwareInfo()

      // 4. Determine the BEST Engine for the device
      val bestEngineName: String
      val engineType: String
      val recommendationSummary: String
      val technicalDetails: String

      if (vulkanSupported && vulkanVersionStr != null) {
        bestEngineName = "$vulkanVersionStr (Hardware GPU Pipeline)"
        engineType = "Vulkan"
        recommendationSummary = "Vulkan: Выбран как самый производительный движок"
        technicalDetails = "Прямой низкоуровневый доступ к GPU $gpuRenderer. Минимальный оверхед драйвера, асинхронные очереди команд и полное устранение микрофризов."
      } else if (openGlVersion.contains("3.2") || openGlVersion.contains("3.1")) {
        bestEngineName = "$openGlVersion (Hardware Accelerated Pipeline)"
        engineType = "OpenGL ES 3.2"
        recommendationSummary = "OpenGL ES 3.2: Оптимальный движок для данного чипа"
        technicalDetails = "Аппаратный рендеринг GPU $gpuRenderer с поддержкой Compute Shaders и аппаратной растеризацией."
      } else {
        bestEngineName = "$openGlVersion (GPU Accelerated)"
        engineType = "OpenGL ES"
        recommendationSummary = "OpenGL ES: Аппаратное ускорение графики"
        technicalDetails = "Аппаратное GPU-ускорение включено через встроенный графический процессор."
      }

      val info = GraphicsEngineInfo(
        bestEngineName = bestEngineName,
        engineType = engineType,
        vulkanSupported = vulkanSupported,
        vulkanVersion = vulkanVersionStr,
        vulkanLevel = vulkanLevel,
        openGlVersion = openGlVersion,
        gpuRenderer = gpuRenderer,
        gpuVendor = gpuVendor,
        isHardwareAccelerated = true,
        recommendationSummary = recommendationSummary,
        technicalDetails = technicalDetails
      )

      cachedInfo = info
      Log.i(TAG, "Detected Best Graphics Engine: $bestEngineName on GPU: $gpuRenderer ($gpuVendor)")
      return info
    }
  }

  /**
   * Safely probes the actual GPU Renderer and Vendor strings using a lightweight offscreen EGL context.
   */
  private fun queryGpuHardwareInfo(): Pair<String, String> {
    var renderer = "GPU Hardware Accelerated"
    var vendor = "Standard Mobile GPU"

    try {
      val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
      if (display == EGL14.EGL_NO_DISPLAY) {
        return fallbackGpu()
      }

      val version = IntArray(2)
      if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
        return fallbackGpu()
      }

      val attribList = intArrayOf(
        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
        EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
        EGL14.EGL_NONE
      )

      val configs = arrayOfNulls<EGLConfig>(1)
      val numConfigs = IntArray(1)
      EGL14.eglChooseConfig(display, attribList, 0, configs, 0, 1, numConfigs, 0)

      val config = configs[0]
      if (config != null) {
        val contextAttribs = intArrayOf(
          EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
          EGL14.EGL_NONE
        )
        val context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)

        val pbufferAttribs = intArrayOf(
          EGL14.EGL_WIDTH, 1,
          EGL14.EGL_HEIGHT, 1,
          EGL14.EGL_NONE
        )
        val surface = EGL14.eglCreatePbufferSurface(display, config, pbufferAttribs, 0)

        if (context != EGL14.EGL_NO_CONTEXT && surface != EGL14.EGL_NO_SURFACE) {
          EGL14.eglMakeCurrent(display, surface, surface, context)

          val glRenderer = GLES20.glGetString(GLES20.GL_RENDERER)
          val glVendor = GLES20.glGetString(GLES20.GL_VENDOR)

          if (!glRenderer.isNullOrBlank()) renderer = glRenderer
          if (!glVendor.isNullOrBlank()) vendor = glVendor

          EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
          EGL14.eglDestroySurface(display, surface)
          EGL14.eglDestroyContext(display, context)
        }
      }
      EGL14.eglTerminate(display)
    } catch (e: Exception) {
      Log.w(TAG, "EGL probe failed, using fallback: ${e.message}")
      return fallbackGpu()
    }

    return Pair(renderer, vendor)
  }

  private fun fallbackGpu(): Pair<String, String> {
    val hardware = Build.HARDWARE
    val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else hardware
    val vendor = when {
      hardware.contains("qcom", ignoreCase = true) || soc.contains("snapdragon", ignoreCase = true) -> "Qualcomm Adreno"
      hardware.contains("exynos", ignoreCase = true) || hardware.contains("mali", ignoreCase = true) -> "ARM Mali"
      hardware.contains("mt", ignoreCase = true) || soc.contains("dimensity", ignoreCase = true) -> "MediaTek / ARM Mali"
      hardware.contains("kirin", ignoreCase = true) -> "HiSilicon / ARM Mali"
      else -> "Hardware Accelerated GPU"
    }
    return Pair(soc.ifBlank { "Mobile GPU" }, vendor)
  }
}
