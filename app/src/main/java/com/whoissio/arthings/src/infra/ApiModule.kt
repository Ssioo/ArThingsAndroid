package com.whoissio.arthings.src.infra

import com.whoissio.arthings.src.infra.Constants.CLOUD_ANCHOR_BASE_URL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

  @Provides
  fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .callTimeout(8000, TimeUnit.MILLISECONDS)
    .connectTimeout(8000, TimeUnit.MILLISECONDS)
    .build()

  @Provides
  fun retrofit(): Retrofit = Retrofit.Builder()
      .baseUrl(CLOUD_ANCHOR_BASE_URL)
      .addConverterFactory(GsonConverterFactory.create())
      .addCallAdapterFactory(RxJava3CallAdapterFactory.createAsync())
      .client(okHttpClient())
      .build()
}