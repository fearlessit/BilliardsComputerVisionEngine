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

package fi.samzone.sportsai.billiards.ui.videosource

import java.awt.Dimension
import java.awt.Graphics
import java.awt.GridLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.awt.image.ImageProducer
import java.awt.image.WritableRaster
import java.io.ObjectOutputStream.PutField

import javax.print.attribute.standard.MediaSize.Engineering
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel

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

import fi.samzone.sportsai.vision.algorithms.VisionAlgorithms
import fi.samzone.sportsai.billiards.engine.BilliardVisionEngine
import fi.samzone.sportsai.billiards.engine.BilliardVisionEngineState
import fi.samzone.sportsai.billiards.tableobjects.ShortTermSpot
import fi.samzone.sportsai.billiards.tableobjects.Table
import fi.samzone.sportsai.billiards.tableobjects.smartvision.InferredBall
import fi.samzone.sportsai.billiards.ui.components.InfoPanel
import fi.samzone.sportsai.billiards.ui.controllers.KeyPressed
import fi.samzone.sportsai.billiards.ui.controllers.ProcessWindowMouseAdapter
import fi.samzone.sportsai.vision.ui.componenets.MatPanel
import fi.samzone.sportsai.vision.ui.componenets.MatPanelWithTextFlash
import fi.samzone.utils.ConfigurationProperties
import static fi.samzone.sportsai.vision.algorithms.GeometricMath.*
import static fi.samzone.sportsai.vision.constants.Color.*

import groovy.transform.Field

import org.codehaus.groovy.transform.sc.ListOfExpressionsExpression
import org.codehaus.groovy.transform.trait.Traits.Implemented
import org.opencv.core.Core

import static org.opencv.imgcodecs.Imgcodecs.*
import static org.opencv.highgui.HighGui.*
import static org.opencv.imgproc.Imgproc.*
import static org.opencv.imgproc.Imgproc.Canny as canny
import static org.opencv.core.Core.*
import static org.opencv.core.CvType.*

System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

@Field final static VIDEO_IMAGE_SIZE = new Size(
		ConfigurationProperties.readInt('Input_video_width'),
		ConfigurationProperties.readInt('Input_video_height'))

@Field final static CORNER_DETECTION_IMAGE_SIZE = new Size(
		ConfigurationProperties.readInt('Corner_detection_image_width'),
		ConfigurationProperties.readInt('Corner_detection_image_height'))


@Field final static TABLE_DETECTION_IMAGE_SIZE = new Size(
		ConfigurationProperties.readInt('Table_detection_image_width'),
		ConfigurationProperties.readInt('Table_detection_image_height'))

billiardVisionEngine = new BilliardVisionEngine()
VideoInputApplicationState videoInputApplicationState = new VideoInputApplicationState()


videoInputApplicationState.camera = ConfigurationProperties.readBoolean('online_mode') ?
		new VideoCapture(ConfigurationProperties.readInt('video_capture_input_camera_number')) :
		new VideoCapture("assets/video/${ConfigurationProperties.read('input_video_file_name')}")

		
videoInputApplicationState.processPanel = createStandaloneOnePanelFrame(VIDEO_IMAGE_SIZE, videoInputApplicationState, 20, 50, new ProcessWindowMouseAdapter(billiardVisionEngine.engineState, videoInputApplicationState))
videoInputApplicationState.smallPanel = createStandaloneOnePanelFrame(CORNER_DETECTION_IMAGE_SIZE, videoInputApplicationState, VIDEO_IMAGE_SIZE.width +50, TABLE_DETECTION_IMAGE_SIZE.height*2 +150)
videoInputApplicationState.tablePanel = createStandaloneOnePanelFrame(TABLE_DETECTION_IMAGE_SIZE, videoInputApplicationState, VIDEO_IMAGE_SIZE.width +50, 50)
videoInputApplicationState.binaryTablePanel = createStandaloneOnePanelFrame(TABLE_DETECTION_IMAGE_SIZE, videoInputApplicationState, VIDEO_IMAGE_SIZE.width +50, 100 +TABLE_DETECTION_IMAGE_SIZE.height)
JPanel infoPanel = createInfoFrame(videoInputApplicationState, 775, VIDEO_IMAGE_SIZE.height +100)


long totalProcessTime = 0

videoInputApplicationState.mainThread = new Thread( {
	while (true) {
		if (!videoInputApplicationState.isPaused) {
			videoInputApplicationState.camera.read(billiardVisionEngine.engineState.cameraMat)
			billiardVisionEngine.processFrame()
			repaintVideoPanels(videoInputApplicationState)
			executePossibleCommand(videoInputApplicationState)
			infoPanel.updateInfos()
			//mainThreadSleep(videoInputApplicationState.sleepBetweenFrames)
		}
		else
			sleep(100) // no point to run infinite nop-loop, give little time to user to disable pause
	}
})
videoInputApplicationState.mainThread.start()


void repaintVideoPanels(VideoInputApplicationState state) {
	drawOverDisplay(state)
	state.processPanel.repaint(billiardVisionEngine.engineState.processMat)
	state.smallPanel.repaint(billiardVisionEngine.engineState.smallMat)
	state.tablePanel.repaint(billiardVisionEngine.engineState.tableMat)
	state.binaryTablePanel.repaint(billiardVisionEngine.engineState.binaryTableMat)
}

void executePossibleCommand(VideoInputApplicationState state) {
	if (state.command != null) {
		String command = state.command.split(',')[0]
		int parameter = state.command.split(',')[1] as int
		switch (command) {
			case 'REWIND' :
				rewindVideo(state.camera, parameter)
				break
			case 'POSITION' :
				state.camera.set(0, parameter)
				break
		}
		state.command = null
	}

}

void drawOverDisplay(state) {
	drawTableContours(state)
	drawBalls()
	//drawSpots()
}

Point transformCoordinate(Point sourceCordinate, Mat sourceCordinateSystem, Mat targetCordinateSystem) {
	double x = sourceCordinate.x * (targetCordinateSystem.rows() as double) / (sourceCordinateSystem.rows() as double)
	double y = sourceCordinate.y * (targetCordinateSystem.cols() as double) / (sourceCordinateSystem.cols() as double)
	return new Point(x, y)
}

void drawTableContours(VideoInputApplicationState state) {

	BilliardVisionEngineState engineState = billiardVisionEngine.engineState
	Table table = engineState.table

	//circle(state.smallMat, pointOnTable, 5, BLUE)
	def tableLineColor = engineState.isForcedCorners ? RED : GREEN
	circle(engineState.processMat, table.backLeftCornerPoint, 20, tableLineColor, 5)
	circle(engineState.processMat, table.backRightCornerPoint, 20, tableLineColor, 5)
	circle(engineState.processMat, table.frontLeftCornerPoint, 20, tableLineColor, 5)
	circle(engineState.processMat, table.frontRightCornerPoint, 20, tableLineColor, 5)
	line(engineState.processMat, table.backLeftCornerPoint, table.backRightCornerPoint, tableLineColor, 3)
	line(engineState.processMat, table.backRightCornerPoint, table.frontRightCornerPoint, tableLineColor, 3)
	line(engineState.processMat, table.frontRightCornerPoint, table.frontLeftCornerPoint, tableLineColor, 3)
	line(engineState.processMat, table.frontLeftCornerPoint, table.backLeftCornerPoint, tableLineColor, 3)
}

void drawBalls() {
	Mat displayMat = billiardVisionEngine.engineState.processMat
	for (InferredBall ball in billiardVisionEngine.engineState.table.balls) {
		if (ball.isLikelyRecognizedAsBall()) {
			Point ballInDisplayMat = transformTablePointToDisplay(ball.lastValidPoint)
			circle(displayMat, ballInDisplayMat, 18, YELLOW, 2)
			if (ball.isStill()) {
				circle(displayMat, ballInDisplayMat, 21, RED, 2)
			}
		}
	}
}



void drawSpots() {
	for (spot in billiardVisionEngine.engineState.table.shortTermSpots) {
		Point spotPoint = transformTablePointToDisplay(spot.tablePoint)
		circle(billiardVisionEngine.engineState.processMat, spotPoint, 2, WHITE, 2)
	}
}

Point transformTablePointToDisplay(Point tablePoint) {

	Boolean needSideReverse = billiardVisionEngine.engineState.table.tableNeedSideReverse()

	// Perspective transformation with warping matrix M (https://docs.opencv.org/2.4/modules/imgproc/doc/geometric_transformations.html#warpperspective):
	// dst(x,y) = src((M_11*x +M_12*y +M_13) / (M_31*x +M_32*y +M_33), (M_21*x +M_22*y +M_23) / (M_31*x +M_32*y +M_33))

	MatOfPoint2f src = needSideReverse ?
			new MatOfPoint2f(
			new Point(0, 0), new Point(TABLE_DETECTION_IMAGE_SIZE.width, 0),
			new Point(TABLE_DETECTION_IMAGE_SIZE.width, TABLE_DETECTION_IMAGE_SIZE.height), new Point(0, TABLE_DETECTION_IMAGE_SIZE.height)
			)
			:
			new MatOfPoint2f(
			new Point(TABLE_DETECTION_IMAGE_SIZE.width, TABLE_DETECTION_IMAGE_SIZE.height), new Point(TABLE_DETECTION_IMAGE_SIZE.width, 0),
			new Point(0, 0), new Point(0, TABLE_DETECTION_IMAGE_SIZE.height)
			)

	MatOfPoint2f dest = new MatOfPoint2f(
			billiardVisionEngine.engineState.table.frontRightCornerPoint, billiardVisionEngine.engineState.table.backRightCornerPoint,
			billiardVisionEngine.engineState.table.backLeftCornerPoint, billiardVisionEngine.engineState.table.frontLeftCornerPoint
			)

	Mat m = getPerspectiveTransform(src, dest)
	double divider = m.get(2,0)[0]*tablePoint.x +m.get(2,1)[0]*tablePoint.y +m.get(2,2)[0]
	double displayX = (m.get(0,0)[0]*tablePoint.x +m.get(0,1)[0]*tablePoint.y +m.get(0,2)[0]) / divider
	double displayY = (m.get(1,0)[0]*tablePoint.x +m.get(1,1)[0]*tablePoint.y +m.get(1,2)[0]) / divider
	return new Point(displayX, displayY)
}

MatPanel createStandaloneOnePanelFrame(Size frameSize, VideoInputApplicationState videoInputApplicationState, locationX = 0, locationY = 0, optionalMouseListener=null) {
	MatPanel contentPanel = new MatPanelWithTextFlash()
	contentPanel.setPreferredSize(new Dimension(frameSize.width as int,frameSize.height as int))
	JFrame standaloneOnePanelFrame = createFrame(videoInputApplicationState, locationX, locationY, optionalMouseListener)
	standaloneOnePanelFrame.contentPane.add(contentPanel)
	standaloneOnePanelFrame.pack()
	return contentPanel
}

JPanel createInfoFrame(VideoInputApplicationState state, locationX, locationY) {
	InfoPanel infoPanel = new InfoPanel(billiardVisionEngine.engineState, state)
	infoPanel.setPreferredSize(new Dimension((VIDEO_IMAGE_SIZE.width / 2.65) as int, 300))
	JFrame infoFrame = createFrame(state, locationX, locationY)
	infoFrame.contentPane.add(infoPanel)
	infoFrame.setResizable(true)
	infoFrame.pack()
	return infoPanel
}

JFrame createFrame(VideoInputApplicationState videoInputApplicationState, locationX, locationY, optionalMouseListener=null) {
	JFrame frame = new JFrame()
	frame.addKeyListener(new KeyPressed(billiardVisionEngine.engineState, videoInputApplicationState))
	if (optionalMouseListener)
		frame.addMouseListener(optionalMouseListener)
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
	frame.setLocation(locationX as int, locationY as int)
	frame.setResizable(false)
	frame.setVisible(true)
	return frame
}



void rewindVideo(VideoCapture camera, int milliSeconds) {
	double value = camera.get(0) +milliSeconds
	if (value < 0)
		value = 0
	camera.set(0, value)
}

void mainThreadSleep(sleepBetweenFrames) {
	try {
		mainThread.sleep(sleepBetweenFrames)
	}
	catch (e) {
	}
}

