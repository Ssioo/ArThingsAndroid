package com.whoissio.arthings.src.views

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.net.Uri
import android.os.*
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.util.Preconditions
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
import com.google.gson.Gson
import com.orhanobut.logger.Logger
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ActivityMainBinding
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
import com.whoissio.arthings.src.viewmodels.MainViewModel
import java.util.*


class MainActivity : BaseActivity.DBActivity<ActivityMainBinding, MainViewModel>(R.layout.activity_main) {
  override val bindingProvider: (LayoutInflater) -> ActivityMainBinding =
    ActivityMainBinding::inflate
  override val vmProvider: () -> MainViewModel =
    { ViewModelProvider(this).get(MainViewModel::class.java) }

  private lateinit var arFragment: MyArFragment

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
      setOnSessionInitializationListener {
        vm.isDepthApiEnabled.value = arSceneView.session?.isDepthModeSupported(Config.DepthMode.AUTOMATIC) == true
      }
      setOnTapArPlaneListener { hitResult, plane, motionEvent ->
        val anchor = hitResult.createAnchor()
        nodeChoiceRenderer
          .thenAccept { addArChoiceViewToScene(anchor, it) }
          .exceptionally { onRenderError(it) }
      }
    }

    binding.btnExport.setOnClickListener{ onClickBtnExport() }

    vm.humidity.observe(this) {
      showToast("$it")
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
      setOnTapListener { hitTestResult, motionEvent ->

      }
    }
    val transform = TransformableNode(arFragment.transformationSystem).apply {
      setParent(anchorNode)
      this.renderable = renderable
    }
    ViewRenderable.builder()
      .setView(this, R.layout.view_solar_node)
      .setSizer(DpToMetersViewSizer(1000))
      .build()
      .thenAccept { setUpSolarNodeInfoView(it, anchorNode) }
      .exceptionally { onRenderError(it) }
    arFragment.arSceneView.scene.addChild(anchorNode)
    transform.select()
  }

  private fun setUpSolarNodeInfoView(it: ViewRenderable, anchor: AnchorNode) {
    it.view.apply {
      val tvTemp = findViewById<TextView>(R.id.temperature)
      vm.temperature.observe(this@MainActivity) {
        tvTemp.text = String.format("%.1f", it)
      }
      val tvHumidity = findViewById<TextView>(R.id.humidity)
      vm.humidity.observe(this@MainActivity) {
        tvHumidity.text = String.format("%.1f", it)
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
        setOnTouchListener(this@MainActivity::onTouchArButton)
        setOnClickListener { onClickArButton(anchor, gltfSolar, GLTF_SOLAR_PATH) }
      }
      findViewById<MaterialButton>(R.id.btn_rf).apply {
        setOnTouchListener(this@MainActivity::onTouchArButton)
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
}