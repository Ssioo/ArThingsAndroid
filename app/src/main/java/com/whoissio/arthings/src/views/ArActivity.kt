package com.whoissio.arthings.src.views

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.os.*
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import com.google.android.material.button.MaterialButton
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.BaseArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.gson.Gson
import com.orhanobut.logger.Logger
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ActivityArBinding
import com.whoissio.arthings.src.BaseActivity
import com.whoissio.arthings.src.infra.ArRendererProvider
import com.whoissio.arthings.src.infra.Constants.GLTF_RF_PATH
import com.whoissio.arthings.src.infra.Constants.GLTF_SOLAR_PATH
import com.whoissio.arthings.src.infra.Constants.PERMISSION_ARRAY
import com.whoissio.arthings.src.infra.Constants.PERMISSION_REQUEST_CODE
import com.whoissio.arthings.src.infra.Constants.SAMPLE_NODE_MAC_ADDRESS
import com.whoissio.arthings.src.infra.Helper.hasPermissions
import com.whoissio.arthings.src.infra.Helper.launchPermissionSettings
import com.whoissio.arthings.src.infra.Helper.shouldShowAnyRequestPermissionRationales
import com.whoissio.arthings.src.infra.utils.*
import com.whoissio.arthings.src.models.DeviceInfo
import com.whoissio.arthings.src.viewmodels.ArViewModel
import com.whoissio.arthings.src.views.components.MyArFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ArActivity : BaseActivity.DBActivity<ActivityArBinding, ArViewModel>(R.layout.activity_ar),
  BaseArFragment.OnTapArPlaneListener,
  BaseArFragment.OnSessionInitializationListener
{
  override val vm: ArViewModel by viewModels()
  @Inject lateinit var cloudAnchorManager: CloudAnchorManager
  @Inject lateinit var arRendererProvider: ArRendererProvider

  private lateinit var arFragment: MyArFragment

  private var nodeChoiceAnchorNode: AnchorNode? = null
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
    arFragment = supportFragmentManager.findFragmentById(R.id.ar_view) as MyArFragment
    arFragment.apply {
      setOnSessionInitializationListener(this@ArActivity)
      setOnTapArPlaneListener(this@ArActivity)
      arSceneView.scene.addOnUpdateListener { cloudAnchorManager.onUpdate() }
    }

    with(binding) {
      btnExport.setOnClickListener { onClickBtnExport() }
      btnRefresh.setOnClickListener { onClickRefresh() }
    }

    with(vm) {
      isDepthApiEnabled.value = cameraManager.cameraIdList.any {
        cameraManager.getCameraCharacteristics(it)
          .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
          ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) == true
      }
    }
  }

  override fun onTapPlane(hitResult: HitResult?, plane: Plane?, motionEvent: MotionEvent?) {
    val anchor = hitResult?.createAnchor() ?: return
    arRendererProvider.nodeChoiceRenderer
      .thenAccept { addArChoiceViewToScene(anchor, it) }
      .exceptionally { onRenderError(it) }
  }

  override fun onSessionInitialization(session: Session?) {
    vm.cloudedAnchors.observe(this) {
      it.forEach {
        cloudAnchorManager.resolveCloudAnchor(session, it.id) {
          onClickArButton(it, arRendererProvider.gltfSolar, GLTF_SOLAR_PATH, false)
        }
      }
    }
  }

  // 전체 데이터 갱신
  private fun onClickRefresh() {

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

  private fun createArNodeToScene(anchor: Anchor, renderable: ModelRenderable?, isNewAnchor: Boolean = true) {
    val anchorNode = AnchorNode(anchor).apply {
      setParent(arFragment.arSceneView.scene)
    }
    TransformableNode(arFragment.transformationSystem).apply {
      setParent(anchorNode)
      this.renderable = renderable
      setOnTapListener { hitTestResult, motionEvent ->

      }
      select()
    }
    arRendererProvider.getNodeViewRenderer(R.layout.view_solar_node)
      .thenAccept { setUpSolarNodeInfoView(it, anchorNode) }
      .exceptionally { onRenderError(it) }

    if (isNewAnchor) uploadAnchor(anchor)
  }

  private fun uploadAnchor(anchor: Anchor) {
    try {
      arFragment.arSceneView.let { it.session?.estimateFeatureMapQualityForHosting(it.arFrame?.camera?.pose) }
        .also { Logger.d(it?.name) }
      cloudAnchorManager.hostCloudAnchor(arFragment.arSceneView.session, anchor) {
        vm.createNewHostAnchor(it, SAMPLE_NODE_MAC_ADDRESS)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun setUpSolarNodeInfoView(it: ViewRenderable, anchor: AnchorNode) {
    it.view.apply {
      val tvTemp = findViewById<TextView>(R.id.temperature)
      val tvHumidity = findViewById<TextView>(R.id.humidity)

      with(vm) {
        temperature.observe(this@ArActivity) { tvTemp.text = String.format("%.1f", it) }
        humidity.observe(this@ArActivity) { tvHumidity.text = String.format("%.1f", it) }
      }
    }
    Node().apply {
      localPosition = Vector3(0f, 0.06f, -0.02f)
      this.renderable = it
      setParent(anchor)
    }
  }

  private fun addArChoiceViewToScene(anchor: Anchor, renderable: ViewRenderable) {
    if (nodeChoiceAnchorNode != null) {
      nodeChoiceAnchorNode?.anchor = anchor
      return
    }
    nodeChoiceAnchorNode = AnchorNode(anchor)
    renderable.view.apply {
      findViewById<MaterialButton>(R.id.btn_solar).apply {
        setOnTouchListener(this@ArActivity::onTouchArButton)
        setOnClickListener { onClickArButton(anchor, arRendererProvider.gltfSolar, GLTF_SOLAR_PATH) }
      }
      findViewById<MaterialButton>(R.id.btn_rf).apply {
        setOnTouchListener(this@ArActivity::onTouchArButton)
        setOnClickListener { onClickArButton(anchor, arRendererProvider.gltfRf, GLTF_RF_PATH) }
      }
    }
    nodeChoiceAnchorNode?.renderable = renderable
    nodeChoiceAnchorNode?.setParent(arFragment.arSceneView.scene)
  }

  private fun onClickArButton(anchor: Anchor, renderer: RenderableSource, id: String, isNew: Boolean = true) {
    nodeChoiceAnchorNode?.setParent(null)
    ModelRenderable.builder()
      .setSource(this, renderer)
      .setRegistryId(id)
      .build()
      .thenAccept { createArNodeToScene(anchor, it, isNew) }
      .exceptionally { onRenderError(it) }
  }

  private fun onTouchArButton(v: View?, event: MotionEvent?): Boolean {
    when (event?.action) {
      MotionEvent.ACTION_DOWN -> v?.alpha = 0.38f
      MotionEvent.ACTION_UP -> v?.alpha = 1f
    }
    return false
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