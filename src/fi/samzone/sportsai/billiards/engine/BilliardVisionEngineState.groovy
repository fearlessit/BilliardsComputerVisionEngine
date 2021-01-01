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
import org.opencv.core.Point
import org.opencv.videoio.VideoCapture

import fi.samzone.sportsai.billiards.tableobjects.Table
import fi.samzone.sportsai.billiards.tableobjects.smartvision.InferredBall
import fi.samzone.sportsai.billiards.tableobjects.smartvision.InferredTableCornerPoint
import fi.samzone.sportsai.vision.ui.componenets.MatPanel
import fi.samzone.sportsai.vision.ui.componenets.MatPanelWithTextFlash

class BilliardVisionEngineState implements Serializable {
	
	
	BilliardVisionEngineState (BilliardVisionEngine billiardVisionEngine) {
		this.billiardVisionEngine = billiardVisionEngine
	}
	
	BilliardVisionEngine billiardVisionEngine

	//long timeTotal = 0

	// OPTIONS / DEFAULT VALUES
	double floodFillDiff = 7.5
	int sleepBetweenFrames = 0

	double ballMinRadius = 10
	double ballMaxRadius = 40

	double maxContourDistanceWithSameObject = 5


	Mat cameraMat = new Mat()
	Mat processMat = new Mat()
	Mat smallMat = new Mat()
	Mat tableMat = new Mat()
	Mat binaryTableMat = new Mat()

	Boolean isForcedCorners = false	// TODO: when detection works (near) perfectly, we do not need this anymore

	Table table = new Table()
	List<Point> detectedBallsInFrame = new LinkedList<Point>()

	int frameCounter = 0
	int frameProcessingTimeInMillis

}

