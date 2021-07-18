package com.whoissio.arthings.src.infra.utils

import android.media.Image
import android.opengl.Matrix
import com.google.ar.core.*
import com.google.ar.core.exceptions.NotYetAvailableException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.experimental.and
import kotlin.math.abs


class DepthDataManager {
  companion object {
    const val FLOATS_PER_POINT = 4 // X,Y,Z,confidence.

    fun create(frame: Frame?, cameraPose: Pose?): FloatBuffer? {
      frame ?: return null
      cameraPose ?: return null
      try {
        val depthImage: Image = frame.acquireRawDepthImage()
        val confidenceImage: Image = frame.acquireRawDepthConfidenceImage()

        // To transform 2D depth pixels into 3D points we retrieve the intrinsic camera parameters
        // corresponding to the depth image. See more information about the depth values at
        // https://developers.google.com/ar/develop/java/depth/overview#understand-depth-values.
        val intrinsics: CameraIntrinsics = frame.camera.textureIntrinsics
        val modelMatrix = FloatArray(16)
        cameraPose.toMatrix(modelMatrix, 0)
        val points: FloatBuffer = convertRawDepthImagesTo3dPointBuffer(
          depthImage, confidenceImage, intrinsics, modelMatrix
        )
        depthImage.close()
        confidenceImage.close()
        return points
      } catch (e: NotYetAvailableException) {
        // This normally means that depth data is not available yet. This is normal so we will not
        // spam the logcat with this.
      }
      return null
    }

    /** Applies camera intrinsics to convert depth image into a 3D pointcloud.  */
    private fun convertRawDepthImagesTo3dPointBuffer(
      depth: Image,
      confidence: Image,
      cameraTextureIntrinsics: CameraIntrinsics,
      modelMatrix: FloatArray
    ): FloatBuffer {
      // Java uses big endian so we have to change the endianess to ensure we extract
      // depth data in the correct byte order.
      val depthImagePlane: Image.Plane = depth.planes[0]
      val depthByteBufferOriginal: ByteBuffer = depthImagePlane.buffer
      val depthByteBuffer: ByteBuffer = ByteBuffer.allocate(depthByteBufferOriginal.capacity())
      depthByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
      while (depthByteBufferOriginal.hasRemaining()) {
        depthByteBuffer.put(depthByteBufferOriginal.get())
      }
      depthByteBuffer.rewind()
      val depthBuffer: ShortBuffer = depthByteBuffer.asShortBuffer()
      val confidenceImagePlane: Image.Plane = confidence.planes[0]
      val confidenceBufferOriginal: ByteBuffer = confidenceImagePlane.buffer
      val confidenceBuffer: ByteBuffer = ByteBuffer.allocate(confidenceBufferOriginal.capacity())
      confidenceBuffer.order(ByteOrder.LITTLE_ENDIAN)
      while (confidenceBufferOriginal.hasRemaining()) {
        confidenceBuffer.put(confidenceBufferOriginal.get())
      }
      confidenceBuffer.rewind()

      // To transform 2D depth pixels into 3D points we retrieve the intrinsic camera parameters
      // corresponding to the depth image. See more information about the depth values at
      // https://developers.google.com/ar/develop/java/depth/overview#understand-depth-values.
      val intrinsicsDimensions = cameraTextureIntrinsics.imageDimensions
      val depthWidth: Int = depth.width
      val depthHeight: Int = depth.height
      val fx = cameraTextureIntrinsics.focalLength[0] * depthWidth / intrinsicsDimensions[0]
      val fy = cameraTextureIntrinsics.focalLength[1] * depthHeight / intrinsicsDimensions[1]
      val cx = cameraTextureIntrinsics.principalPoint[0] * depthWidth / intrinsicsDimensions[0]
      val cy = cameraTextureIntrinsics.principalPoint[1] * depthHeight / intrinsicsDimensions[1]

      // Allocate the destination point buffer. If the number of depth pixels is larger than
      // `maxNumberOfPointsToRender` we uniformly subsample. The raw depth image may have
      // different resolutions on different devices.
      val maxNumberOfPointsToRender = 20000f
      val step =
        Math.ceil(Math.sqrt((depthWidth * depthHeight / maxNumberOfPointsToRender).toDouble()))
          .toInt()
      val points: FloatBuffer =
        FloatBuffer.allocate(depthWidth / step * depthHeight / step * FLOATS_PER_POINT)
      val pointCamera = FloatArray(4)
      val pointWorld = FloatArray(4)
      var y = 0
      while (y < depthHeight) {
        var x = 0
        while (x < depthWidth) {

          // Depth images are tightly packed, so it's OK to not use row and pixel strides.
          val depthMillimeters: Int =
            depthBuffer.get(y * depthWidth + x).toInt() // Depth image pixels are in mm.
          if (depthMillimeters == 0) {
            // Pixels with value zero are invalid, meaning depth estimates are missing from
            // this location.
            x += step
            continue
          }
          val depthMeters = depthMillimeters / 1000.0f // Depth image pixels are in mm.

          // Retrieves the confidence value for this pixel.
          val confidencePixelValue: Byte = confidenceBuffer.get(
            y * confidenceImagePlane.rowStride
              + x * confidenceImagePlane.pixelStride
          )
          val confidenceNormalized = ((confidencePixelValue and 0xff.toByte()).toFloat()) / 255.0f
          if (confidenceNormalized < 0.3 || depthMeters > 8) {
            // Ignores "low-confidence" pixels.
            x += step
            continue
          }

          // Unprojects the depth into a 3D point in camera coordinates.
          pointCamera[0] = depthMeters * (x - cx) / fx
          pointCamera[1] = depthMeters * (cy - y) / fy
          pointCamera[2] = -depthMeters
          pointCamera[3] = 1f

          // Applies model matrix to transform point into world coordinates.
          Matrix.multiplyMV(pointWorld, 0, modelMatrix, 0, pointCamera, 0)
          points.put(pointWorld[0]) // X.
          points.put(pointWorld[1]) // Y.
          points.put(pointWorld[2]) // Z.
          points.put(confidenceNormalized)
          x += step
        }
        y += step
      }
      points.rewind()
      return points
    }

    fun filterUsingPlanes(points: FloatBuffer, allPlanes: Collection<Plane>) {
      val planeNormal = FloatArray(3)

      // Allocates the output buffer.
      val numPoints: Int = points.remaining() / DepthDataManager.FLOATS_PER_POINT

      // Each plane is checked against each point.
      for (plane: Plane in allPlanes) {
        if (plane.trackingState !== TrackingState.TRACKING || plane.subsumedBy != null) {
          continue
        }

        // Computes the normal vector of the plane.
        val planePose: Pose = plane.getCenterPose()
        planePose.getTransformedAxis(1, 1.0f, planeNormal, 0)

        // Filters points that are too close to the plane.
        for (index in 0 until numPoints) {
          // Retrieves the next point.
          val x: Float = points.get(FLOATS_PER_POINT * index)
          val y: Float = points.get(FLOATS_PER_POINT * index + 1)
          val z: Float = points.get(FLOATS_PER_POINT * index + 2)

          // Transforms point to be in world coordinates, to match plane info.
          val distance =
            ((x - planePose.tx()) * planeNormal[0]) + ((y - planePose.ty()) * planeNormal[1]) + ((z - planePose.tz()) * planeNormal[2])

          // Controls the size of objects detected.
          // Smaller values mean smaller objects will be kept.
          // Larger values will only allow detection of larger objects, but also helps reduce noise.
          if (abs(distance) > 0.03) {
            continue  // Keeps this point, since it's far enough away from the plane.
          }

          // Invalidates points that are too close to planar surfaces.
          points.put(FLOATS_PER_POINT * index, 0f)
          points.put(FLOATS_PER_POINT * index + 1, 0f)
          points.put(FLOATS_PER_POINT * index + 2, 0f)
          points.put(FLOATS_PER_POINT * index + 3, 0f)
        }
      }
    }
  }


}