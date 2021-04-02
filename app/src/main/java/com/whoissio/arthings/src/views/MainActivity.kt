package com.whoissio.arthings.src.views

import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.*
import androidx.lifecycle.ViewModelProvider
import com.google.ar.core.*
import com.google.gson.Gson
import com.orhanobut.logger.Logger
import com.whoissio.arthings.ApplicationClass.Companion.scanner
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ActivityMainBinding
import com.whoissio.arthings.src.BaseActivity
import com.whoissio.arthings.src.infra.Constants.PERMISSION_ARRAY
import com.whoissio.arthings.src.infra.Constants.PERMISSION_REQUEST_CODE
import com.whoissio.arthings.src.infra.Constants.SAMPLE_NODE_MAC_ADDRESS
import com.whoissio.arthings.src.infra.Helper.hasPermissions
import com.whoissio.arthings.src.infra.Helper.launchPermissionSettings
import com.whoissio.arthings.src.infra.Helper.shouldShowAnyRequestPermissionRationales
import com.whoissio.arthings.src.infra.utils.BackgroundRenderer
import com.whoissio.arthings.src.infra.utils.DisplayRotationHelper
import com.whoissio.arthings.src.infra.utils.TapHelper
import com.whoissio.arthings.src.models.Device
import com.whoissio.arthings.src.models.DeviceInfo
import com.whoissio.arthings.src.models.RssiTimeStamp
import com.whoissio.arthings.src.viewmodels.MainViewModel
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.collections.ArrayList
import kotlin.concurrent.withLock


class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>(R.layout.activity_main),
  GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

  private var isARActive = false
  private var isGlAttached = false
  private var isSurfaceCreated = false
  private var captureSessionChangesPossible = true

  private var sharedSession: Session? = null
  private var sharedCamera: SharedCamera? = null
  private var cameraId: String? = null
  private var previewCaptureRequestBuilder: CaptureRequest.Builder? = null
  private var captureSession: CameraCaptureSession? = null
  private var backgroundThread: HandlerThread? = null
  private var backgroundHandler: Handler? = null

  private var backgroundRenderer = BackgroundRenderer()
  private val safeToExitApp = ConditionVariable()
  private val displayRotationHelper by lazy { DisplayRotationHelper(this) }
  private val tapHelper by lazy { TapHelper(this) }

  private val cameraDeviceCallback = object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
      vm.cameraDevice = camera
      createCameraPreviewSession()
    }

    override fun onClosed(camera: CameraDevice) {
      vm.cameraDevice = null
      safeToExitApp.open()
    }

    override fun onDisconnected(camera: CameraDevice) = vm.releaseCameraDevice()

    override fun onError(camera: CameraDevice, error: Int) {
      vm.releaseCameraDevice()
      finish()
    }
  }

  private val cameraSessionStateCallback = object : CameraCaptureSession.StateCallback() {
    override fun onConfigured(session: CameraCaptureSession) {
      captureSession = session
      setRepeatingCaptureRequest()
    }

    override fun onActive(session: CameraCaptureSession) {
      if (!isARActive) resumeAR()
      val lock = ReentrantLock()
      val condition = lock.newCondition()
      lock.withLock {
        captureSessionChangesPossible = true
        condition.signal()
      }
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
      if (!isARActive) resumeAR()
    }
  }

  /* Methods Start */
  override fun initView(savedInstanceState: Bundle?) {
    binding.gvMain.apply {
      preserveEGLContextOnPause = true
      setEGLContextClientVersion(2)
      setEGLConfigChooser(8, 8, 8, 8, 16, 0)
      setRenderer(this@MainActivity)
      renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
      setOnTouchListener(tapHelper)
    }
    binding.btnExport.setOnClickListener {
      pauseAR()
      vm.pauseScanBle()
      startActivity(Intent(this, BleResultActivity::class.java).apply {
        vm.scannedDevices.value?.let {
          var result: List<DeviceInfo> = listOf()
          it.forEach {
            result = result.plus(DeviceInfo(it.key.address, it.key.txPower, it.value))
          }
          putExtra("Data", Gson().toJson(result).also { Logger.d(it) })
        }
      })
    }
  }

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    isSurfaceCreated = true
    GLES20.glClearColor(0f, 0f, 0f, 1f)
    try {
      backgroundRenderer.createOnGlThread(this)
      openCamera()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    GLES20.glViewport(0, 0, width, height)
    displayRotationHelper.onSurfaceChanged(width, height)
  }

  override fun onDrawFrame(gl: GL10?) {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    if (!vm.shouldUpdateSurfaceTexture.get()) return
    displayRotationHelper.updateSessionIfNeeded(sharedSession)
    try {
      onDrawFrameARCore()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) = Unit

  private fun setRepeatingCaptureRequest() {
    previewCaptureRequestBuilder?.let {
      captureSession?.setRepeatingRequest(it.build(), vm.cameraCaptureCallback, backgroundHandler)
    }
  }

  private fun onDrawFrameARCore() {
    if (!isARActive) return
    val frame = sharedSession?.update() ?: return
    val camera = frame.camera
    isGlAttached = true
    onTap(frame, camera)
    backgroundRenderer.draw(frame)

    if (camera.trackingState == TrackingState.PAUSED) return

  }

  private fun onTap(frame: Frame, camera: Camera) {
    if (camera.trackingState != TrackingState.TRACKING) return
    tapHelper.poll()?.let {
      frame.hitTest(it).lastOrNull()?.let {
        runOnUiThread {
          showToast("${it.distance} m")
        }
        /*if ((trackable is Plane && trackable.isPoseInPolygon(it.hitPose) && PlaneRenderer.calculateDistanceToPlane(it.hitPose, camera.pose) > 0)
          || (trackable is Point && trackable.orientationMode === Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)) {

          val objColor = when (trackable) {
            is Point -> floatArrayOf(66.0f, 133.0f, 244.0f, 255.0f)
            is Plane -> floatArrayOf(139.0f, 195.0f, 74.0f, 255.0f)
            else -> DEFAULT_COLOR
          }
          vm.anchors.add(ColoredAnchor(it.createAnchor(), objColor))
          return@forEach
        }*/
      }
    }
  }

  private fun createCameraPreviewSession() {
    try {
      sharedSession?.setCameraTextureName(backgroundRenderer.cameraTextureId)
      sharedCamera?.surfaceTexture?.setOnFrameAvailableListener(this)

      previewCaptureRequestBuilder =
        vm.cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)

      val surfaceList = sharedCamera?.arCoreSurfaces ?: mutableListOf()
      surfaceList.forEach { previewCaptureRequestBuilder?.addTarget(it) }

      val wrappedCallback =
        sharedCamera?.createARSessionStateCallback(cameraSessionStateCallback, backgroundHandler)
          ?: throw Error("WrappedCallback is Null: createCameraPreviewSession")
      vm.cameraDevice?.createCaptureSession(surfaceList, wrappedCallback, backgroundHandler)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  @SuppressLint("MissingPermission")
  private fun openCamera() {
    if (vm.cameraDevice != null) return
    if (!hasPermissions()) {
      return if (shouldShowAnyRequestPermissionRationales()) launchPermissionSettings()
      else requestPermissions(PERMISSION_ARRAY, PERMISSION_REQUEST_CODE)
    }
    if (!hasValidARCoreAndUpToDate()) return
    if (sharedSession == null) {
      try {
        sharedSession = Session(this, setOf(Session.Feature.SHARED_CAMERA))
      } catch (e: Exception) {
        e.printStackTrace()
        return
      }
      sharedSession?.configure(
        sharedSession?.config?.apply {
          focusMode = Config.FocusMode.AUTO
          if (sharedSession?.isDepthModeSupported(Config.DepthMode.AUTOMATIC) == true) {
            depthMode = Config.DepthMode.AUTOMATIC
            vm.isDepthApiEnabled.postValue(true)
          }
          instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
        }
      )
    }
    sharedCamera = sharedSession?.sharedCamera
    cameraId = sharedSession?.cameraConfig?.cameraId
    if (cameraId == null) return
    try {
      val wrappedCallback =
        sharedCamera?.createARDeviceStateCallback(cameraDeviceCallback, backgroundHandler) ?: return
      val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
      val characteristics = cameraManager.getCameraCharacteristics(cameraId!!)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        vm.keysThatCanCauseCaptureDelaysWhenModified = characteristics.availableSessionKeys
      }
      captureSessionChangesPossible = false
      cameraId?.let { cameraManager.openCamera(it, wrappedCallback, backgroundHandler) }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun closeCamera() {
    captureSession?.close()
    captureSession = null
    if (vm.cameraDevice != null) {
      waitUntilCameraCaptureSessionIsActive()
      safeToExitApp.close()
      vm.cameraDevice?.close()
      safeToExitApp.block()
    }
  }

  private fun waitUntilCameraCaptureSessionIsActive() {
    val lock = ReentrantLock()
    val condition = lock.newCondition()
    lock.withLock {
      while (!captureSessionChangesPossible) {
        try {
          condition.await()
        } catch (e: InterruptedException) {
          e.printStackTrace()
        }
      }
    }
  }

  private fun hasValidARCoreAndUpToDate(): Boolean {
    return when (ArCoreApk.getInstance().checkAvailability(this)) {
      ArCoreApk.Availability.SUPPORTED_INSTALLED -> true
      ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD, ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
        try {
          ArCoreApk.getInstance().requestInstall(this, true) == ArCoreApk.InstallStatus.INSTALLED
        } catch (e: Exception) {
          false
        }
      }
      ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE, ArCoreApk.Availability.UNKNOWN_ERROR, ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
        showToast("AR 사용이 불가능한 기종입니다.")
        false
      }
      else -> false
    }
  }

  private fun startBackgroundThread() {
    backgroundThread = HandlerThread("sharedCameraBackground")
    backgroundThread?.start()
    backgroundHandler = Handler(backgroundThread?.looper!!)
  }

  private fun stopBackgroundThread() {
    if (backgroundThread != null) {
      backgroundThread?.quitSafely()
      try {
        backgroundThread?.join()
        backgroundThread = null
        backgroundHandler = null
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  private fun disableInstantPlacement() {
    sharedSession?.configure(sharedSession?.config?.apply {
      instantPlacementMode = Config.InstantPlacementMode.DISABLED
    })
  }

  private fun resumeAR() {
    if (sharedSession == null || isARActive) return
    try {
      backgroundRenderer.suppressTimestampZeroRendering(false)
      sharedSession?.resume()
      isARActive = true
      sharedCamera?.setCaptureCallback(vm.cameraCaptureCallback, backgroundHandler)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun pauseAR() {
    if (!isARActive) return
    sharedSession?.pause()
    isARActive = false
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    when (requestCode) {
      PERMISSION_REQUEST_CODE -> {
        if (grantResults.any { it != PackageManager.PERMISSION_GRANTED })
          finish()
      }
    }
  }

  override fun onResume() {
    super.onResume()
    waitUntilCameraCaptureSessionIsActive()
    startBackgroundThread()
    binding.gvMain.onResume()
    resumeAR()
    if (isSurfaceCreated) openCamera()
    displayRotationHelper.onResume()
    vm.resumeScanBle()
  }

  override fun onPause() {
    vm.shouldUpdateSurfaceTexture.set(false)
    binding.gvMain.onPause()
    waitUntilCameraCaptureSessionIsActive()
    displayRotationHelper.onPause()
    pauseAR()
    closeCamera()
    stopBackgroundThread()
    vm.pauseScanBle()
    super.onPause()
  }

  override fun onDestroy() {
    sharedSession?.close()
    sharedSession = null
    super.onDestroy()
  }

  override fun getViewModel(): MainViewModel =
    ViewModelProvider(this).get(MainViewModel::class.java)
}