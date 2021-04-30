package com.whoissio.arthings.src.apis

import com.whoissio.arthings.src.models.CloudedAnchor
import com.whoissio.arthings.src.models.CloudedAnchorListResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.*

interface CloudAnchorApi {
  @GET("/v1beta2/management/anchors")
  fun getAnchors(
    @Query("page_size") pageSize: Int = 1000,
    @Query("order_by") orderBy: String = "last_localize_time%20desc"
  ): Single<CloudedAnchorListResponse>

  @PATCH("/v2beta2/management/anchors/{id}")
  fun updateAnchor(@Path("id") id: String, @Query("updateMask") mask: String): Single<CloudedAnchor>

  @DELETE("/v1beta2/management/anchors/{id}")
  fun deleteAnchor(@Path("id") id: String): Single<Any>
}