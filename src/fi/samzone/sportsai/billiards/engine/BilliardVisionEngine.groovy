/*
 BCVE: Billiard Computer Vision Engine
 Copyright (C) 2020  Sampo Yrjänäinen
 
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 */
 
package fi.samzone.sportsai.billiards.engine

import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Rect2d
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.highgui.HighGui
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.Videoio
import org.opencv.core.Core
import static org.opencv.imgcodecs.Imgcodecs.*
import static org.opencv.highgui.HighGui.*
import static org.opencv.imgproc.Imgproc.*
import static org.opencv.imgproc.Imgproc.Canny as canny
import static org.opencv.core.Core.*
import static org.opencv.core.CvType.*

import fi.samzone.sportsai.vision.algorithms.VisionAlgorithms
import fi.samzone.sportsai.billiards.tableobjects.ShortTermSpot
import fi.samzone.sportsai.billiards.tableobjects.Table
import fi.samzone.sportsai.billiards.tableobjects.smartvision.InferredBall
import fi.samzone.sportsai.billiards.ui.videosource.VideoInputApplicationState
import fi.samzone.utils.ConfigurationProperties
import static fi.samzone.sportsai.vision.algorithms.GeometricMath.*
import static fi.samzone.sportsai.vision.constants.Color.*

class BilliardVisionEngine {

	BilliardVisionEngineState engineState = new BilliardVisionEngineState(this)
	Size VIDEO_IMAGE_SIZE
	Size CORNER_DETECTION_IMAGE_SIZE
	Size TABLE_DETECTION_IMAGE_SIZE

	private long frameStartedEpoch

	public BilliardVisionEngine(
			Size VIDEO_IMAGE_SIZE = new Size(ConfigurationProperties.readInt('Input_video_width'), ConfigurationProperties.readInt('Input_video_height')),
			Size CORNER_DETECTION_IMAGE_SIZE = new Size(ConfigurationProperties.readInt('Corner_detection_image_width'), ConfigurationProperties.readInt('Corner_detection_image_height')),
			Size TABLE_DETECTION_IMAGE_SIZE = new Size(ConfigurationProperties.readInt('Table_detection_image_width'), ConfigurationProperties.readInt('Table_detection_image_height'))) {

		this.VIDEO_IMAGE_SIZE = VIDEO_IMAGE_SIZE
		this.CORNER_DETECTION_IMAGE_SIZE = CORNER_DETECTION_IMAGE_SIZE
		this.TABLE_DETECTION_IMAGE_SIZE = TABLE_DETECTION_IMAGE_SIZE
	}


	public void processFrame() {
		startFrameTiming()
		resize(engineState.cameraMat, engineState.processMat, VIDEO_IMAGE_SIZE)
		findCorners()
		warpTablePerspective()
		detectBalls()
		engineState.table.inferBalls(engineState.detectedBallsInFrame)
		stopFrameTiming()
	}

	void findCorners() {
		if (engineState.frameCounter % 25 == 0) {
			resize(engineState.processMat, engineState.smallMat, CORNER_DETECTION_IMAGE_SIZE)
			Point pointOnTable = VisionAlgorithms.findSimilarPointOnCenterSpiral(engineState.smallMat)
			Mat mask = new Mat()
			Scalar newColor = WHITE
			Scalar diff = new Scalar(engineState.floodFillDiff, engineState.floodFillDiff, engineState.floodFillDiff)
			Rect boundingRect = new Rect()
			floodFill(engineState.smallMat, mask, pointOnTable, newColor, boundingRect, diff, diff, 8 | (255 << 8))

			Point leftCornerSmall = VisionAlgorithms.findColorOnLine(engineState.smallMat, WHITE, boundingRect.height, boundingRect.x, boundingRect.y, 0, 1)
			Point rightCornerSmall = VisionAlgorithms.findColorOnLine(engineState.smallMat, WHITE, boundingRect.height, boundingRect.x +boundingRect.width -1, boundingRect.y, 0, 1)
			Point topCornerSmall = VisionAlgorithms.findColorOnLine(engineState.smallMat, WHITE, boundingRect.width, boundingRect.x, boundingRect.y, 1, 0)
			Point bottomCornerSmall = VisionAlgorithms.findColorOnLine(engineState.smallMat, WHITE, boundingRect.width, boundingRect.x, boundingRect.y +boundingRect.height -1, 1, 0)

			if (!engineState.isForcedCorners) {
				engineState.table.backLeftCornerPoint.recordPoint(transformCoordinate(bottomCornerSmall, engineState.smallMat, engineState.processMat))
				engineState.table.backRightCornerPoint.recordPoint(transformCoordinate(rightCornerSmall, engineState.smallMat, engineState.processMat))
				engineState.table.frontLeftCornerPoint.recordPoint(transformCoordinate(leftCornerSmall, engineState.smallMat, engineState.processMat))
				engineState.table.frontRightCornerPoint.recordPoint(transformCoordinate(topCornerSmall, engineState.smallMat, engineState.processMat))
			}
		}
	}

	void detectBalls(BilliardVisionEngineState state) {
		cvtColor(engineState.tableMat, engineState.binaryTableMat, Imgproc.COLOR_RGB2GRAY)
		canny(engineState.binaryTableMat, engineState.binaryTableMat, 50.0, 150.0, 3, true)

		Mat hierarchy = new Mat()
		List<MatOfPoint> immutableContours = []
		findContours(engineState.binaryTableMat, immutableContours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_NONE)
		List<List<Point>> contours = immutableContours.collect { new LinkedList(it.toList()) }
		engineState.detectedBallsInFrame = []
		boolean continueWithSameObject = false
		List<Point> contour
		while (contours.size() > 0) {
			if (!continueWithSameObject)
				contour = contours.pop()
			continueWithSameObject = false
			// Optimization: if this contour is already too big ditch it
			//if (VisionAlgorithms.getRadiusOptimizedWithMaxValue(contour, engineState.ballMaxRadius) >= engineState.ballMaxRadius) continue
			for (int contourIndex=0; contourIndex < contours.size(); contourIndex++) {
				List<Point> comparsionContour = contours[contourIndex]
				// Optimization: if this contour is already too big ditch it
				//if (VisionAlgorithms.getRadiusOptimizedWithMaxValue(contour, engineState.ballMaxRadius) >= engineState.ballMaxRadius) continue
				if (VisionAlgorithms.isContoursOfSameObject(contour, comparsionContour, engineState.maxContourDistanceWithSameObject)) {
					contour.addAll (comparsionContour)
					continueWithSameObject = true
					contours.remove(comparsionContour)
					contourIndex--
				}
			}
			if (!continueWithSameObject || contours.size() == 0) {
				double contourRadius = getRadiusOptimizedWithMaxValue(contour, engineState.ballMaxRadius)
				if (contourRadius > engineState.ballMinRadius && contourRadius < engineState.ballMaxRadius)
					engineState.detectedBallsInFrame << getGeometricAverage(contour)
			}
		}
	}


	Point transformCoordinate(Point sourceCordinate, Mat sourceCordinateSystem, Mat targetCordinateSystem) {
		double x = sourceCordinate.x * (targetCordinateSystem.rows() as double) / (sourceCordinateSystem.rows() as double)
		double y = sourceCordinate.y * (targetCordinateSystem.cols() as double) / (sourceCordinateSystem.cols() as double)
		return new Point(x, y)
	}

	void warpTablePerspective() {

		Table table = engineState.table
		Boolean needSideReverse = table.tableNeedSideReverse()

		MatOfPoint2f src = new MatOfPoint2f(
				table.frontRightCornerPoint, table.backRightCornerPoint,
				table.backLeftCornerPoint, table.frontLeftCornerPoint
				)

		MatOfPoint2f dest = needSideReverse ?
				new MatOfPoint2f(
				new Point(0, 0), new Point(TABLE_DETECTION_IMAGE_SIZE.width, 0),
				new Point(TABLE_DETECTION_IMAGE_SIZE.width, TABLE_DETECTION_IMAGE_SIZE.height), new Point(0, TABLE_DETECTION_IMAGE_SIZE.height)
				)
				:
				new MatOfPoint2f(
				new Point(TABLE_DETECTION_IMAGE_SIZE.width, TABLE_DETECTION_IMAGE_SIZE.height), new Point(TABLE_DETECTION_IMAGE_SIZE.width, 0),
				new Point(0, 0), new Point(0, TABLE_DETECTION_IMAGE_SIZE.height)
				)

		Mat warpingMat = getPerspectiveTransform(src, dest)
		warpPerspective(engineState.processMat, engineState.tableMat, warpingMat, TABLE_DETECTION_IMAGE_SIZE)
	}


	private void startFrameTiming() {
		long frameStartedEpoch = System.currentTimeMillis()
	}
	
	private void stopFrameTiming() {
		engineState.frameProcessingTimeInMillis = System.currentTimeMillis() -frameStartedEpoch
		engineState.frameCounter++
	}

}
