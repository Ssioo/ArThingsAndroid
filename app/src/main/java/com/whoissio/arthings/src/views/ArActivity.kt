package com.whoissio.arthings.src.views

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.media.Image
import android.os.*
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.viewModels
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.BaseArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.gson.Gson
import com.orhanobut.logger.Logger
import com.whoissio.arthings.BuildConfig
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ActivityArBinding
import com.whoissio.arthings.src.BaseActivity
import com.whoissio.arthings.src.infra.Constants
import com.whoissio.arthings.src.infra.utils.ArRendererProvider
import com.whoissio.arthings.src.infra.Constants.GLTF_RF_PATH
import com.whoissio.arthings.src.infra.Constants.GLTF_SOLAR_PATH
import com.whoissio.arthings.src.infra.Constants.PERMISSION_ARRAY
import com.whoissio.arthings.src.infra.Constants.PERMISSION_REQUEST_CODE
import com.whoissio.arthings.src.infra.Converters.toPx
import com.whoissio.arthings.src.infra.Helper.hasPermissions
import com.whoissio.arthings.src.infra.Helper.launchPermissionSettings
import com.whoissio.arthings.src.infra.Helper.parseFunction
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
class  ArActivity : BaseActivity.DBActivity<ActivityArBinding, ArViewModel>(R.layout.activity_ar),
  BaseArFragment.OnTapArPlaneListener,
  BaseArFragment.OnSessionInitializationListener {

  override val vm: ArViewModel by viewModels()

  @Inject lateinit var cloudAnchorManager: CloudAnchorManager
  @Inject lateinit var arRendererProvider: ArRendererProvider

  private val arFragment: MyArFragment by lazy { supportFragmentManager.findFragmentById(R.id.ar_view) as MyArFragment }

  private var nodeChoiceAnchorNode: AnchorNode = AnchorNode()
  private val cameraManager by lazy { getSystemService(CAMERA_SERVICE) as CameraManager }

  override fun initView(savedInstanceState: Bundle?) {
    /* AR CORE APK 있는지, Permission 부여되었는지 검사 */
    if (!hasPermissions()) {
      return if (shouldShowAnyRequestPermissionRationales()) launchPermissionSettings()
      else requestPermissions(PERMISSION_ARRAY, PERMISSION_REQUEST_CODE)
    }
    if (!hasValidARCoreAndUpToDate()) return

    /* AR Plane 초기화 */
    arFragment.apply {
      setOnSessionInitializationListener(this@ArActivity)
      setOnTapArPlaneListener(this@ArActivity)
      arSceneView.scene.addOnUpdateListener { cloudAnchorManager.onUpdate() }
    }

    binding.btnExport.setOnClickListener { onClickBtnExport() } // BLE 데이터 내보내기 테스트 21.03.24
    binding.btnRefresh.setOnClickListener { onClickRefresh() }
    binding.btnAdd.setOnClickListener { onClickAddNode() }
    binding.btnRender.setOnClickListener { findNodeAreas() }
    binding.addableNode1.setOnClickListener {

    }
    binding.addableNode2.setOnClickListener {

    }
    binding.addableNode3.setOnClickListener {

    }

    vm.isDepthApiEnabled.value = cameraManager.cameraIdList.any {
      cameraManager.getCameraCharacteristics(it)
        .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) == true
    }
    vm.isAddableOpen.observe(this, this::onChangeNodeAddable)
  }

  private fun findNodeAreas() {
    /* 화면 프레임으로부터 NodeArea 획득한 후 render*/
    showProgress()
    binding.root.postDelayed({
      val frame = arFragment.arSceneView.session?.update()
      acquireNodeArea(frame).forEach {
        renderNodeAreaOverlayOn(it, frame)
      }
      hideProgress()
    }, if (BuildConfig.DEBUG) 1000 else 5000)
  }

  /* 화면 프레임에서 중요 영역을 뽑아 냄 */
  private fun acquireNodeArea(frame: Frame?): List<Any> {
    val image: Image?
    return try {
      image = frame?.acquireCameraImage()
      /* 무언가 image로부터 중요 영역을 뽑아냄 */
      image?.close()
      return emptyList()
      /*listOf(
        NodeArea(300f, 720f, 0.2f, 0),
        NodeArea(400f, 960f, 0.1f, 1),
        NodeArea(700f, 840f, 0.2f, 2),
      ) // 데모*/
    } catch (e: Exception) {
      e.printStackTrace()
      emptyList()
    }
  }

  /* 화면 x,y 픽셀 좌표를 토대로, 구역 Overlay 생성 */
  private fun renderNodeAreaOverlayOn(nodeArea: Any, frame: Frame?) {
    /*frame?.hitTest(nodeArea.pxX, nodeArea.pxY)?.also { Logger.d(it) }?.firstOrNull()?.let {
      val anchor = it.createAnchor()
      when (nodeArea.type) {
        0 -> arRendererProvider.getPlaneRenderer(1f, 0f, 0f, 0.1f)
        1 -> arRendererProvider.getPlaneRenderer(0f, 1f, 0f, 0.1f)
        else -> arRendererProvider.getPlaneRenderer(0f, 0f, 1f, 0.1f)
      }
        .thenAccept {
          val parentNode = AnchorNode(anchor).apply {
            setParent(arFragment.arSceneView.scene)
          }
          AnchorNode().apply {
            this.renderable = ShapeFactory.makeSphere(nodeArea.radius, Vector3(0.0f, 0.0f, 0.0f), it)
            setParent(parentNode)
          }
          if (nodeArea.type == 1) {
            ModelRenderable.builder()
              .setRegistryId(Constants.GLTF_EXCLAMATION_PATH)
              .setSource(this, arRendererProvider.gltfExclamation)
              .build()
              .thenAccept {
                AnchorNode().apply {
                  localPosition = Vector3(0f, 0.06f, -0.02f)
                  this.renderable = it
                  setParent(parentNode)
                }
              }
          }
        }
    }*/
  }

  private fun onChangeNodeAddable(it: Boolean) {
    listOf(
      binding.nodeAddGroup.animate().translationY(if (it) 0f else 250f.toPx(this)),
      binding.btnAdd.animate().rotation(if (it) -45f else 0f)
        .translationY(if (it) (-250f).toPx(this) else 0f),
    ).forEach {
      it.setDuration(500)
        .setInterpolator(AccelerateDecelerateInterpolator())
        .start()
    }
  }

  private fun onClickAddNode() {
    vm.isAddableOpen.value = !vm.isAddableOpen.value!!
  }

  var countTemp = 1

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
    /*when (countTemp) {
      1 -> {
        // 빨강
        arRendererProvider.getPlaneRenderer(1f, 0f, 0f, 0.1f)
          .thenAccept {
            val parentNode = AnchorNode(anchor).apply {
              setParent(arFragment.arSceneView.scene)
            }
            AnchorNode(anchor).apply {
              this.renderable = ShapeFactory.makeSphere(Math.random().div(3).toFloat(), Vector3(0.0f, 0.0f, 0.0f), it)
              setParent(parentNode)
            }

          }
        countTemp += 1
      }
      else -> {
        // 초록
        arRendererProvider.getPlaneRenderer(0f, 1f, 0f, 0.1f)
          .thenAccept {
            val parentNode = AnchorNode(anchor).apply {
              setParent(arFragment.arSceneView.scene)
            }
            AnchorNode(anchor).apply {
              this.renderable = ShapeFactory.makeSphere(Math.random().div(2).toFloat(), Vector3(0.0f, 0.15f, 0.0f), it)
              setParent(parentNode)
            }
          }
      }
    }*/

  }

  override fun onSessionInitialization(session: Session?) {
    vm.cloudedAnchors.observe(this) {
      it.filter { it.id.isNotEmpty() && it.room == 1 }
        .forEach { anchor ->
          cloudAnchorManager.resolveCloudAnchor(session, anchor.id) {
            createArBleNode(it, arRendererProvider.gltfSolar to GLTF_SOLAR_PATH, anchor.address, anchor.type)
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
    type: String
  ) {
    val targetAnchor = vm.cloudedAnchors.value?.find { it.address == targetBleAddr } ?: return
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
          targetAnchor.data.forEach { c -> addDataView(c.name) { c.parseFunction(it) } }
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
        if (targetAnchor.id.isEmpty()) uploadAnchorToTargetAddr(anchor, targetBleAddr, type)

        /*ModelRenderable.builder()
          .setRegistryId(Constants.GLTF_EXCLAMATION_PATH)
          .setSource(this, arRendererProvider.gltfExclamation)
          .build()
          .thenAccept {
            AnchorNode().apply {
              localPosition = Vector3(0f, 0.06f, -0.02f)
              this.renderable = it
              setParent(anchorNode)
            }
          }*/
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