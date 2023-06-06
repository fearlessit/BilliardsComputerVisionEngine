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
import org.opencv.core.MatOfRect
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

import org.opencv.objdetect.CascadeClassifier;
import javax.swing.*

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
	boolean delay_low = false
	boolean delay_high = false
	boolean gameStart = false
	boolean gameEnd = false
	int points = 0

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
		gameState()
		engineState.table.inferBalls(engineState.detectedBallsInFrame)
		stopFrameTiming()
	}

	
	void findCorners() {
		if (engineState.frameCounter % 1 == 0) {
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
					
					//Count the balls that "can be" detected in the frame
					int ballCount = engineState.detectedBallsInFrame.size()
					
					for (int i = 0; i < ballCount; i++) 
						{
							String luku = engineState.detectedBallsInFrame[i]
							luku = luku.replace("{", "")
							luku = luku.replace("}", "")
							
							String[] separatedValues = luku.split(",")
							String luku1 = separatedValues[0]
							String luku2 = separatedValues[1]
							
							double ballX = Double.parseDouble(luku1)
							double ballY = Double.parseDouble(luku2)
							
							ballX = ballX.round()
							ballY = ballY.round()
							if (engineState.frameCounter % 5 == 0) 
								{
									if (!delay_low && ballX < 10 && ballY < 10)
										{
											delay_low = true
											points += 1
											println("You got a point!" + " Back Left")
										}
									else if (ballX < 8 && ballY > 190)
										{
											points += 1
											println("You got a point!" + " Front Left")
										}
									else if (ballX > 380 && ballY < 5) 
										{
											points += 1
											println("You got a point!" + " Back Right")
										}
									else if (ballX > 390 && ballY > 190)
										{
											points += 1
											println("You got a point!" + " Front Right")
										}
									else if (!delay_high && ballX < 210 && ballX > 190 && ballY > 178) 
										{
											points += 1
											delay_high = true
											println("You got a point!" + " Front Middle")
										}
									else if (!delay_low && ballX < 201 && ballX > 198 && ballY < 5) 
										{
											delay_low = true
											points += 1
											println("You got a point!" + " Back Middle")
										}
									}
									
									
									if (engineState.frameCounter % 20 == 0) 
										{
											delay_low = false
										}
									if (engineState.frameCounter % 200 == 0) 
										{
											delay_high = false
										}
								}	
							}
						}
					}


	void gameState() 
	{
		int ballCount = engineState.detectedBallsInFrame.size()
		List ballListX = []
		List ballListY = []
		List approved = []
		List inside = []
		
		double ballX;
		double ballY
		
		for (int i = 0; i < ballCount; i++) 
			{
				String luku = engineState.detectedBallsInFrame[i]
				luku = luku.replace("{", "")
				luku = luku.replace("}", "")
				
				String[] separatedValues = luku.split(",")
				String luku0 = separatedValues[0]
				String luku1 = separatedValues[1]
				
				ballX = Double.parseDouble(luku0)
				ballY = Double.parseDouble(luku1)
				
				ballX = ballX.round()
				ballY = ballY.round()
				
				ballListX.add(ballX)
				ballListY.add(ballY)
			}
		
		for (int a = 0; a < ballListY.size(); a++) 
			{
				int sum = ballListY[a]
				
				if (sum > 90 && sum < 110)  
					{
						approved.add(a)
					}
			}
			
		if (engineState.frameCounter % 25 == 0) 
			{
			if (!gameStart && approved.size() > 1)
				{
					gameEnd = false
					gameStart = true
					println("Game started!")
				}
			}
		if (!gameEnd && ballCount == 1 && points > 0) 
			{
			if (engineState.frameCounter % 100 == 0) 
				{
					if (!gameEnd && ballCount == 1 && points > 0) 
						{
							gameEnd = true
							gameStart = false
							gameOver()
							points = 0
						}
					}
				}
			}
		
		
		//Making the "Game Over" window when game ends
		void gameOver() 
		{
			JFrame frame = new JFrame("GameOver");
			
					JLabel label = new JLabel("Game Over!" + " You got " + points + " points");
					label.setHorizontalAlignment(JLabel.CENTER);
					
					frame.getContentPane().add(label);
					frame.setSize(300, 200);
					frame.setVisible(true);
					println("Game Over!")
		}
		
		
		
boolean isPointCloseToCorner(Point point, Point corner, double distanceThreshold) {
    double distance = Math.sqrt(Math.pow(point.x - corner.x, 2) + Math.pow(point.y - corner.y, 2));
    return distance <= distanceThreshold;
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
