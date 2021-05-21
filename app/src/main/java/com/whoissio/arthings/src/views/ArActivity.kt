package com.whoissio.arthings.src.views

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.media.Image
import android.os.*
import android.view.MotionEvent
import android.widget.TextView
import androidx.activity.viewModels
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.DpToMetersViewSizer
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.BaseArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.gson.Gson
import com.orhanobut.logger.Logger
import com.whoissio.arthings.BuildConfig
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ActivityArBinding
import com.whoissio.arthings.src.BaseActivity
import com.whoissio.arthings.src.infra.ArRendererProvider
import com.whoissio.arthings.src.infra.Constants.DATE_FORMAT
import com.whoissio.arthings.src.infra.Constants.GLTF_RF_PATH
import com.whoissio.arthings.src.infra.Constants.GLTF_SOLAR_PATH
import com.whoissio.arthings.src.infra.Constants.PERMISSION_ARRAY
import com.whoissio.arthings.src.infra.Constants.PERMISSION_REQUEST_CODE
import com.whoissio.arthings.src.infra.Helper.hasPermissions
import com.whoissio.arthings.src.infra.Helper.launchPermissionSettings
import com.whoissio.arthings.src.infra.Helper.shouldShowAnyRequestPermissionRationales
import com.whoissio.arthings.src.infra.core.MockFunction
import com.whoissio.arthings.src.infra.utils.*
import com.whoissio.arthings.src.models.DeviceInfo
import com.whoissio.arthings.src.viewmodels.ArViewModel
import com.whoissio.arthings.src.views.components.MyArFragment
import com.whoissio.arthings.src.views.components.NodeChoiceView
import com.whoissio.arthings.src.views.components.NodeInfoView
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ArActivity : BaseActivity.DBActivity<ActivityArBinding, ArViewModel>(R.layout.activity_ar),
  BaseArFragment.OnTapArPlaneListener,
  BaseArFragment.OnSessionInitializationListener {
  override val vm: ArViewModel by viewModels()
  @Inject
  lateinit var cloudAnchorManager: CloudAnchorManager
  @Inject
  lateinit var arRendererProvider: ArRendererProvider

  private val arFragment: MyArFragment by lazy { supportFragmentManager.findFragmentById(R.id.ar_view) as MyArFragment }

  private var nodeChoiceAnchorNode: AnchorNode = AnchorNode()
  private val cameraManager by lazy { getSystemService(CAMERA_SERVICE) as CameraManager }

  override fun onCreate(savedInstanceState: Bundle?) {
    if (!hasPermissions()) {
      return if (shouldShowAnyRequestPermissionRationales()) launchPermissionSettings()
      else requestPermissions(PERMISSION_ARRAY, PERMISSION_REQUEST_CODE)
    }
    if (!hasValidARCoreAndUpToDate()) return
    super.onCreate(savedInstanceState)
  }

  override fun initView(savedInstanceState: Bundle?) {
    arFragment.apply {
      setOnSessionInitializationListener(this@ArActivity)
      setOnTapArPlaneListener(this@ArActivity)
      arSceneView.scene.addOnUpdateListener { cloudAnchorManager.onUpdate() }
    }

    binding.btnExport.setOnClickListener { onClickBtnExport() }
    binding.btnRefresh.setOnClickListener { onClickRefresh() }

    vm.isDepthApiEnabled.value = cameraManager.cameraIdList.any {
      cameraManager.getCameraCharacteristics(it)
        .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) == true
    }
  }

  override fun onTapPlane(hitResult: HitResult?, plane: Plane?, motionEvent: MotionEvent?) {
    val anchor = hitResult?.createAnchor() ?: return
    val scannedBles = vm.scannedDevicesDataSortedByDistance.value ?: emptyList()
    val readyToUploadBles = vm.notUploadedBleData.value ?: emptyList()
    if (scannedBles.isEmpty()) {
      return showAlert("Scan nearby ble nodes before registering an AR node")
    }
    if (readyToUploadBles.isEmpty()) {
      return showAlert("No more node to be uploaded, check your node list")
    }
    val targetBleAddr = readyToUploadBles.first().first.address
    Logger.d(targetBleAddr)
    acquireConfidenceImage()
    arRendererProvider.nodeChoiceRenderer
      .thenAccept { addArChoiceViewToScene(anchor, it, targetBleAddr) }
      .exceptionally { onRenderError(it) }
  }

  override fun onSessionInitialization(session: Session?) {
    vm.cloudedAnchors.observe(this) {
      it.filter { it.id.isNotEmpty() && it.room == 1 }
        .forEach { anchor ->
          cloudAnchorManager.resolveCloudAnchor(session, anchor.id) {
            createArBleNode(it, arRendererProvider.gltfSolar to GLTF_SOLAR_PATH, anchor.address, anchor.type, false)
          }
      }
    }
  }

  private fun acquireConfidenceImage() {
    var depthImage: Image? = null
    var confidenceImage: Image? = null
    try {
      val frame = arFragment.arSceneView.session?.update()
      depthImage = frame?.acquireRawDepthImage()
      confidenceImage = frame?.acquireRawDepthConfidenceImage()
      Logger.d("${depthImage?.width}, ${depthImage?.height}")
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      depthImage?.close()
      confidenceImage?.close()
    }
  }

  // 전체 데이터 갱신
  @MockFunction
  private fun onClickRefresh() {
    showProgress()
    binding.root.postDelayed({
      vm.refreshData()
      hideProgress()
    }, if (BuildConfig.DEBUG) 1000 else 5000)
  }

  private fun onClickBtnExport() {
    vm.pauseScanBle()
    startActivity(Intent(this, BleResultActivity::class.java).apply {
      vm.scannedDevices.value?.let {
        putExtra(
          "Data",
          Gson().toJson(it.map { DeviceInfo(it.key.address, it.key.txPower, it.value) })
        )
      }
    })
  }

  private fun addArChoiceViewToScene(
    anchor: Anchor,
    renderable: ViewRenderable,
    targetBleAddr: String
  ) {
    nodeChoiceAnchorNode.apply {
      isEnabled = true
      setParent(arFragment.arSceneView.scene)
      this.anchor = anchor
      this.renderable = renderable
    }
    NodeChoiceView(view = renderable.view, anchor = anchor)
      .setOnClickListener { it, which ->
        nodeChoiceAnchorNode.isEnabled = false
        when (which) {
          NodeChoiceView.ButtonChoice.BUTTON_SOLAR -> createArBleNode(it, arRendererProvider.gltfSolar to GLTF_SOLAR_PATH, targetBleAddr, "SOLAR")
          NodeChoiceView.ButtonChoice.BUTTON_RF -> createArBleNode(it, arRendererProvider.gltfRf to GLTF_RF_PATH, targetBleAddr, "RF")
          NodeChoiceView.ButtonChoice.BUTTON_CLOSE -> Unit
        }
      }
  }

  private fun createArBleNode(
    anchor: Anchor,
    renderer: Pair<RenderableSource, String>,
    targetBleAddr: String,
    type: String,
    isNewAnchor: Boolean = true
  ) {
    ModelRenderable.builder()
      .setSource(this, renderer.first)
      .setRegistryId(renderer.second)
      .build()
      .thenAccept {
        val anchorNode = AnchorNode(anchor).apply { setParent(arFragment.arSceneView.scene) }
        TransformableNode(arFragment.transformationSystem).apply {
          setParent(anchorNode)
          this.renderable = it
          setOnTapListener { hitTestResult, motionEvent ->

          }
          select()
        }
        val infoView = NodeInfoView(this, targetBleAddr).apply {
          addDataView("Humid") {
            0.48828125 * (it.getOrNull(27)?.toInt() ?: 0) - 6.0
          }
          addDataView("Temp") {
            0.68640625 * (it.getOrNull(28)?.toInt() ?: 0) - 46.85
          }
          vm.scannedDevicesData.observe(this@ArActivity) {
            it.filter { it.key.address == targetBleAddr }
              .toList()
              .firstOrNull()?.let {
                onUpdate(it.second)
              }
          }
        }
        infoView.build()
          .thenAccept { infoView.attach(anchorNode, it) }
          .exceptionally { onRenderError(it) }
        if (isNewAnchor) uploadAnchorToTargetAddr(anchor, targetBleAddr, type)
      }
      .exceptionally { onRenderError(it) }
  }

  private fun uploadAnchorToTargetAddr(anchor: Anchor, targetBleAddr: String, type: String) {
    AlertDialog.Builder(this)
      .setMessage("Do you want to host this anchor? to $targetBleAddr")
      .setPositiveButton("Yes") { dialog, _ ->
        dialog.dismiss()
        showProgress()
        try {
          arFragment.arSceneView.let { it.session?.estimateFeatureMapQualityForHosting(it.arFrame?.camera?.pose) }
            .also { Logger.d(it?.name) }
          cloudAnchorManager.hostCloudAnchor(arFragment.arSceneView.session, anchor) {
            hideProgress()
            vm.createNewHostAnchor(it, targetBleAddr, 1, type)
          }
        } catch (e: Exception) {
          hideProgress()
          e.printStackTrace()
        }
      }
      .setNegativeButton("No") { dialog, _ ->
        dialog.dismiss()
        anchor.detach()
      }
      .show()
  }

  private fun onRenderError(it: Throwable): Void? {
    AlertDialog.Builder(this)
      .setMessage(it.localizedMessage)
      .show()
    return null
  }

  private fun hasValidARCoreAndUpToDate(): Boolean =
    when (ArCoreApk.getInstance().checkAvailability(this)) {
      ArCoreApk.Availability.SUPPORTED_INSTALLED -> true
      ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD, ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
        kotlin.runCatching {
          ArCoreApk.getInstance().requestInstall(this, true) == ArCoreApk.InstallStatus.INSTALLED
        }
          .getOrDefault(false)
      }
      ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE, ArCoreApk.Availability.UNKNOWN_ERROR, ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
        showToast("AR 사용이 불가능한 기종입니다.")
        false
      }
      else -> false
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
    vm.resumeScanBle()
  }

  override fun onPause() {
    vm.pauseScanBle()
    super.onPause()
  }

  override fun onStop() {
    cloudAnchorManager.clear()
    super.onStop()
  }
}