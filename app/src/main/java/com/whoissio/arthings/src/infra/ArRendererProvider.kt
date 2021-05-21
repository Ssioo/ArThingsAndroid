package com.whoissio.arthings.src.infra

import android.content.Context
import android.net.Uri
import androidx.annotation.LayoutRes
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.rendering.DpToMetersViewSizer
import com.google.ar.sceneform.rendering.ViewRenderable
import com.whoissio.arthings.R
import com.whoissio.arthings.src.infra.Constants.GLTF_RF_PATH
import com.whoissio.arthings.src.infra.Constants.GLTF_RF_SCALE
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.CompletableFuture
import javax.inject.Inject

@ActivityScoped
class ArRendererProvider @Inject constructor(@ActivityContext val context: Context) {

  val gltfRf: RenderableSource = RenderableSource.builder()
  .setSource(context, Uri.parse(GLTF_RF_PATH), RenderableSource.SourceType.GLTF2)
  .setScale(GLTF_RF_SCALE)
  .setRecenterMode(RenderableSource.RecenterMode.ROOT)
  .build()

  val gltfSolar: RenderableSource = RenderableSource.builder()
    .setSource(context, Uri.parse(Constants.GLTF_SOLAR_PATH), RenderableSource.SourceType.GLTF2)
    .setScale(Constants.GLTF_SOLAR_SCALE)
    .setRecenterMode(RenderableSource.RecenterMode.ROOT)
    .build()

  val nodeChoiceRenderer: CompletableFuture<ViewRenderable> by lazy {
    ViewRenderable.builder()
      .setView(context, R.layout.view_node_attacher)
      .setSizer(DpToMetersViewSizer(1000))
      .build()
  }

  fun getNodeViewRenderer(@LayoutRes id: Int): CompletableFuture<ViewRenderable> {
    return ViewRenderable.builder()
      .setView(context, id)
      .setSizer(DpToMetersViewSizer(1000))
      .build()
  }
}