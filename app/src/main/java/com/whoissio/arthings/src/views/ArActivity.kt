package com.whoissio.arthings.src.views

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT
import android.net.Uri
import android.os.*
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.DpToMetersViewSizer
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.TransformableNode
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.orhanobut.logger.Logger
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ActivityArBinding
import com.whoissio.arthings.src.BaseActivity
import com.whoissio.arthings.src.infra.Constants.GLTF_RF_PATH
import com.whoissio.arthings.src.infra.Constants.GLTF_RF_SCALE
import com.whoissio.arthings.src.infra.Constants.GLTF_SOLAR_PATH
import com.whoissio.arthings.src.infra.Constants.GLTF_SOLAR_SCALE
import com.whoissio.arthings.src.infra.Constants.PERMISSION_ARRAY
import com.whoissio.arthings.src.infra.Constants.PERMISSION_REQUEST_CODE
import com.whoissio.arthings.src.infra.Helper.hasPermissions
import com.whoissio.arthings.src.infra.Helper.launchPermissionSettings
import com.whoissio.arthings.src.infra.Helper.shouldShowAnyRequestPermissionRationales
import com.whoissio.arthings.src.infra.utils.*
import com.whoissio.arthings.src.models.DeviceInfo
import com.whoissio.arthings.src.viewmodels.ArViewModel
import com.whoissio.arthings.src.views.components.MyArFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class ArActivity : BaseActivity.DBActivity<ActivityArBinding, ArViewModel>(R.layout.activity_ar) {

  override val vm: ArViewModel by viewModels()

  private lateinit var arFragment: MyArFragment
  private lateinit var cloudAnchorManager: CloudAnchorManager

  private val gltfSolar = RenderableSource.builder()
    .setSource(this, Uri.parse(GLTF_SOLAR_PATH), RenderableSource.SourceType.GLTF2)
    .setScale(GLTF_SOLAR_SCALE)
    .setRecenterMode(RenderableSource.RecenterMode.ROOT)
    .build()

  private val gltfRf = RenderableSource.builder()
    .setSource(this, Uri.parse(GLTF_RF_PATH), RenderableSource.SourceType.GLTF2)
    .setScale(GLTF_RF_SCALE)
    .setRecenterMode(RenderableSource.RecenterMode.ROOT)
    .build()

  private val nodeChoiceRenderer by lazy {
    ViewRenderable.builder()
      .setView(this, R.layout.view_node_attacher)
      .setSizer(DpToMetersViewSizer(1000))
      .build()
  }
  private var nodeChoiceAnchorNode: AnchorNode? = null

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
      setOnSessionInitializationListener(this@ArActivity::onSessionInitialized)
      setOnTapArPlaneListener { hitResult, plane, motionEvent ->
        val anchor = hitResult.createAnchor()
        nodeChoiceRenderer
          .thenAccept { addArChoiceViewToScene(anchor, it) }
          .exceptionally { onRenderError(it) }
      }
    }
    val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
    vm.isDepthApiEnabled.value = cameraManager.cameraIdList.any {
      val characteristics = cameraManager.getCameraCharacteristics(it)
      val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
      capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) == true
    }

    with(binding) {
      btnExport.setOnClickListener{ onClickBtnExport() }
    }

    with(vm) {
      humidity.observe(this@ArActivity) {
        showToast("$it")
      }
    }
  }

  private val onUpdateAnchorList = object : ChildEventListener {
    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

    }

    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

    }

    override fun onChildRemoved(snapshot: DataSnapshot) {

    }

    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

    }

    override fun onCancelled(error: DatabaseError) {

    }
  }

  private fun onSessionInitialized(it: Session?) {
    it ?: return
    cloudAnchorManager = CloudAnchorManager(it)
    arFragment.arSceneView.scene.addOnUpdateListener {
      cloudAnchorManager.onUpdate()
    }

    with(Firebase.database) {
      getReference("anchors").get()
        .addOnSuccessListener {
          Logger.d(it.value)
        }
        .addOnFailureListener {
          it.printStackTrace()
        }
      getReference("anchors").addChildEventListener(onUpdateAnchorList)
    }
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

  private fun addModelToScene(anchor: Anchor, renderable: ModelRenderable?) {
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
    ViewRenderable.builder()
      .setView(this, R.layout.view_solar_node)
      .setSizer(DpToMetersViewSizer(1000))
      .build()
      .thenAccept { setUpSolarNodeInfoView(it, anchorNode) }
      .exceptionally { onRenderError(it) }

    kotlin.runCatching {
      arFragment.arSceneView.let { it.session?.estimateFeatureMapQualityForHosting(it.arFrame?.camera?.pose) }
    }
      .onSuccess {
        Logger.d(it?.name)
        cloudAnchorManager.hostCloudAnchor(anchor) {
          Logger.d(it.cloudAnchorId)
          Logger.d(it.pose)
        }
      }
      .onFailure {
      it.printStackTrace()
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
      setOnClickListener {

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
        setOnClickListener { onClickArButton(anchor, gltfSolar, GLTF_SOLAR_PATH) }
      }
      findViewById<MaterialButton>(R.id.btn_rf).apply {
        setOnTouchListener(this@ArActivity::onTouchArButton)
        setOnClickListener { onClickArButton(anchor, gltfRf, GLTF_RF_PATH) }
      }
    }
    nodeChoiceAnchorNode?.renderable = renderable
    nodeChoiceAnchorNode?.setParent(arFragment.arSceneView.scene)
  }

  private fun onClickArButton(anchor: Anchor, renderer: RenderableSource, id: String) {
    nodeChoiceAnchorNode?.setParent(null)
    ModelRenderable.builder()
      .setSource(this, renderer)
      .setRegistryId(id)
      .build()
      .thenAccept { addModelToScene(anchor, it) }
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
    super.onStop()
    Firebase.database.getReference("anchors").removeEventListener(onUpdateAnchorList)
  }
}