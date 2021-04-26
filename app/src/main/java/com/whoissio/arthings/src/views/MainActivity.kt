package com.whoissio.arthings.src.views

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.*
import android.view.LayoutInflater
import androidx.lifecycle.ViewModelProvider
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.gson.Gson
import com.orhanobut.logger.Logger
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ActivityMainBinding
import com.whoissio.arthings.src.BaseActivity
import com.whoissio.arthings.src.infra.Constants.GLTF_SOLAR_PATH
import com.whoissio.arthings.src.infra.Constants.PERMISSION_ARRAY
import com.whoissio.arthings.src.infra.Constants.PERMISSION_REQUEST_CODE
import com.whoissio.arthings.src.infra.Helper.hasPermissions
import com.whoissio.arthings.src.infra.Helper.launchPermissionSettings
import com.whoissio.arthings.src.infra.Helper.shouldShowAnyRequestPermissionRationales
import com.whoissio.arthings.src.infra.utils.*
import com.whoissio.arthings.src.models.DeviceInfo
import com.whoissio.arthings.src.viewmodels.MainViewModel
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.concurrent.withLock


class MainActivity : BaseActivity.DBActivity<ActivityMainBinding, MainViewModel>(R.layout.activity_main) {
  override val bindingProvider: (LayoutInflater) -> ActivityMainBinding =
    ActivityMainBinding::inflate
  override val vmProvider: () -> MainViewModel =
    { ViewModelProvider(this).get(MainViewModel::class.java) }

  private lateinit var arFragment: ArFragment

  private val gltfSolar = RenderableSource.builder()
    .setSource(this, Uri.parse(GLTF_SOLAR_PATH), RenderableSource.SourceType.GLTF2)
    .setScale(0.00008f)
    .setRecenterMode(RenderableSource.RecenterMode.ROOT)
    .build()

  private var isARActive = false
  // private var isGlAttached = false
  // private var isSurfaceCreated = false
  // private var captureSessionChangesPossible = true

  private var sharedSession: Session? = null
  private var sharedCamera: SharedCamera? = null
  // private var cameraId: String? = null
  private var previewCaptureRequestBuilder: CaptureRequest.Builder? = null
  private var captureSession: CameraCaptureSession? = null
  private var backgroundThread: HandlerThread? = null
  private var backgroundHandler: Handler? = null

  private var backgroundRenderer = BackgroundRenderer()
  /*private val planeRenderer = PlaneRenderer()
  private val virtualObjectRenderer = ObjectRenderer()
  private val virtualObjectShadowRenderer = ObjectRenderer()
  private val safeToExitApp = ConditionVariable()
  private val displayRotationHelper by lazy { DisplayRotationHelper(this) }
  private val tapHelper by lazy { TapHelper(this) }

  private val anchorMatrix = FloatArray(16)*/

  /*private val cameraDeviceCallback = object : CameraDevice.StateCallback() {
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
  }*/

  /*private val cameraSessionStateCallback = object : CameraCaptureSession.StateCallback() {
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
  }*/

  /* Methods Start */
  override fun initView(savedInstanceState: Bundle?) {
    arFragment = supportFragmentManager.findFragmentById(R.id.ar_view) as ArFragment
    arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
      val anchor = hitResult.createAnchor()
      ModelRenderable.builder()
        .setSource(this, gltfSolar)
        .setRegistryId(GLTF_SOLAR_PATH)
        .build()
        .thenAccept { addModelToScene(anchor, it) }
        .exceptionally {
          AlertDialog.Builder(this)
            .setMessage(it.localizedMessage)
            .show()
          return@exceptionally null
        }
    }

    /*binding.gvMain.apply {
      preserveEGLContextOnPause = true
      setEGLContextClientVersion(2)
      setEGLConfigChooser(8, 8, 8, 8, 16, 0)
      setRenderer(this@MainActivity)
      renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
      setOnTouchListener(tapHelper)
    }*/
    binding.btnExport.setOnClickListener {
      // pauseAR()
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

  private fun addModelToScene(anchor: Anchor, renderable: ModelRenderable?) {
    val anchorNode = AnchorNode(anchor)
    val transform = TransformableNode(arFragment.transformationSystem).apply {
      setParent(anchorNode) // anchorNode.add(it)
      this.renderable = renderable
    }
    transform.setOnTapListener { hitTestResult, motionEvent ->
      Logger.d("Tapped")
      Logger.d(vm.anchors)
    }
    arFragment.arSceneView.scene.addChild(anchorNode)
    transform.select()
    vm.anchors.add(anchor)
  }

  /*override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    isSurfaceCreated = true
    GLES20.glClearColor(0f, 0f, 0f, 1f)
    try {
      backgroundRenderer.createOnGlThread(this)
      planeRenderer.createOnGlThread(this, "models/trigrid.png")
      virtualObjectRenderer.createOnGlThread(this, "models/andy.obj", "models/andy.png")
      virtualObjectShadowRenderer.apply {
        createOnGlThread(this@MainActivity, "models/andy_shadow.obj", "models/andy_shadow.png")
        setBlendMode(ObjectRenderer.BlendMode.Shadow)
        setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f)
      }
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

  override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) = Unit*/

  /*private fun setRepeatingCaptureRequest() {
    previewCaptureRequestBuilder?.let {
      captureSession?.setRepeatingRequest(it.build(), vm.cameraCaptureCallback, backgroundHandler)
    }
  }*/

  /*private fun onDrawFrameARCore() {
    if (!isARActive) return
    val frame = sharedSession?.update() ?: return
    val camera = frame.camera
    isGlAttached = true
    onTap(frame, camera)
    backgroundRenderer.draw(frame)

    if (camera.trackingState == TrackingState.PAUSED) return

    val projmtx = FloatArray(16).also { camera.getProjectionMatrix(it, 0, 0.1f, 100.0f) }
    val viewmtx = FloatArray(16).also { camera.getViewMatrix(it, 0) }
    val colorCorrectionRgba = FloatArray(4).also { frame.lightEstimate.getColorCorrection(it, 0) }

    planeRenderer.drawPlanes(
      sharedSession?.getAllTrackables(Plane::class.java) ?: emptyList(),
      camera.displayOrientedPose,
      projmtx
    )

    vm.anchors.forEach {
      if (it.trackingState == TrackingState.TRACKING) {
        it.pose.toMatrix(anchorMatrix, 0)
        virtualObjectRenderer.updateModelMatrix(anchorMatrix, scaleFactor = 1.0f)
        virtualObjectShadowRenderer.updateModelMatrix(anchorMatrix, scaleFactor = 1.0f)
        virtualObjectRenderer.draw(viewmtx, projmtx, colorCorrectionRgba)
        virtualObjectShadowRenderer.draw(viewmtx, projmtx, colorCorrectionRgba)
      }
    }
  }*/

  /*private fun onTap(frame: Frame, camera: Camera) {
    if (camera.trackingState != TrackingState.TRACKING) return
    tapHelper.poll()?.let {
      frame.hitTest(it).lastOrNull()?.let {
        runOnUiThread { showToast("${it.distance} m") }
        if (it.trackable.let { p ->
            p is Plane
              && p.isPoseInPolygon(it.hitPose)
              && PlaneRenderer.calculateDistanceToPlane(it.hitPose, camera.pose) > 0
        } || it.trackable.let { p ->
            p is Point
              && p.orientationMode === Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
        }) {
          runOnUiThread { showToast("Create Object...") }
          val createdAnchor = it.createAnchor()
          vm.anchors.add(createdAnchor)
          Logger.d(vm.anchors)
        }
      }
    }
  }*/

  /*private fun createCameraPreviewSession() {
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
  }*/

  /*@SuppressLint("MissingPermission")
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
  }*/

  /*private fun closeCamera() {
    captureSession?.close()
    captureSession = null
    if (vm.cameraDevice != null) {
      waitUntilCameraCaptureSessionIsActive()
      safeToExitApp.close()
      vm.cameraDevice?.close()
      safeToExitApp.block()
    }
  }*/

  /*private fun waitUntilCameraCaptureSessionIsActive() {
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
  }*/

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

  /*private fun startBackgroundThread() {
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
  }*/

  /*private fun disableInstantPlacement() {
    sharedSession?.configure(sharedSession?.config?.apply {
      instantPlacementMode = Config.InstantPlacementMode.DISABLED
    })
  }*/

  /*private fun resumeAR() {
    if (sharedSession == null || isARActive) return
    try {
      backgroundRenderer.suppressTimestampZeroRendering(false)
      sharedSession?.resume()
      isARActive = true
      sharedCamera?.setCaptureCallback(vm.cameraCaptureCallback, backgroundHandler)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }*/

  /*private fun pauseAR() {
    if (!isARActive) return
    sharedSession?.pause()
    isARActive = false
  }*/

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

  /*override fun onResume() {
    super.onResume()
    waitUntilCameraCaptureSessionIsActive()
    startBackgroundThread()
    // binding.gvMain.onResume()
    resumeAR()
    if (isSurfaceCreated) openCamera()
    displayRotationHelper.onResume()
    vm.resumeScanBle()
  }

  override fun onPause() {
    vm.shouldUpdateSurfaceTexture.set(false)
    // binding.gvMain.onPause()
    waitUntilCameraCaptureSessionIsActive()
    displayRotationHelper.onPause()
    pauseAR()
    closeCamera()
    stopBackgroundThread()
    vm.pauseScanBle()
    super.onPause()
  }*/

  override fun onDestroy() {
    // sharedSession?.close()
    // sharedSession = null
    super.onDestroy()
  }
}