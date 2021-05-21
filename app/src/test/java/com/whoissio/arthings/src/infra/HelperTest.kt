package com.whoissio.arthings.src.infra

import com.whoissio.arthings.src.infra.Helper.isAvailable
import com.whoissio.arthings.src.models.CachedData
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

class HelperTest {

  lateinit var expiredCache: CachedData<String>
  lateinit var validCache: CachedData<String>

  val expirationTime = 300000

  @Before
  fun setUp() {
    expiredCache = CachedData(data = "Expired", cachedAt = Date().apply { time = System.currentTimeMillis() - expirationTime - 1 })
    validCache = CachedData(data = "Test", cachedAt = Date().apply { time = System.currentTimeMillis() - expirationTime }) // 경계값 분석
  }

  @After
  fun tearDown() {

  }

  @Test
  fun isCachedDataAvailable() {
    Assert.assertEquals(true, validCache.isAvailable())
    Assert.assertEquals(false, expiredCache.isAvailable())
  }

  @Test
  fun randomBleScanRecordGenerator() {
    Assert.assertEquals(30, Helper.randomBleRecordGenerator().size)
  }
}