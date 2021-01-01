package fi.samzone.sportsai.billiards.ui.controllers

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

import org.opencv.core.Point

import fi.samzone.sportsai.billiards.engine.BilliardVisionEngineState
import fi.samzone.sportsai.billiards.tableobjects.smartvision.InferredTableCornerPoint
import fi.samzone.sportsai.billiards.ui.videosource.VideoInputApplicationState

class ProcessWindowMouseAdapter extends MouseAdapter {

	BilliardVisionEngineState billiardVisionEngineState
	VideoInputApplicationState videoInputApplicationState
	int determineCornerIndex = 0
	Point determineCorner
	

	ProcessWindowMouseAdapter(BilliardVisionEngineState billiardVisionEngineState, VideoInputApplicationState videApplicationApplicationState) {
		this.videoInputApplicationState = videApplicationApplicationState
		this.billiardVisionEngineState = billiardVisionEngineState
	}


	public void mouseClicked(MouseEvent e) {
		if (e.button in [1,3] && !billiardVisionEngineState.isForcedCorners) {
			videoInputApplicationState.processPanel.flashText("Please enter to forced corner mode before with middle mouse key.", e.x, e.y, 3500)
			videoInputApplicationState.mainThread.interrupt()
			return
		}
		whenMiddleButtonPressedThenToggleUseOfForcedCorners(e)
		whenRightButtonPressedChangeCornerPointToDetermine(e)
		if (billiardVisionEngineState.isForcedCorners && e.button == 1) {
			InferredTableCornerPoint newCornerPoint = new InferredTableCornerPoint()
			newCornerPoint.x = e.x
			newCornerPoint.y = e.y -billiardVisionEngineState.ballMinRadius*2
			if (determineCornerIndex == 0) billiardVisionEngineState.table.backLeftCornerPoint = newCornerPoint 
			if (determineCornerIndex == 1) billiardVisionEngineState.table.backRightCornerPoint = newCornerPoint 
			if (determineCornerIndex == 2) billiardVisionEngineState.table.frontRightCornerPoint = newCornerPoint 
			if (determineCornerIndex == 3) billiardVisionEngineState.table.frontLeftCornerPoint = newCornerPoint
		}
		videoInputApplicationState.mainThread.interrupt()
		
	}
	
	private void whenMiddleButtonPressedThenToggleUseOfForcedCorners(MouseEvent e) {
		if (e.button == 2) {
			billiardVisionEngineState.isForcedCorners = billiardVisionEngineState.isForcedCorners == false
			String infoFlashText = billiardVisionEngineState.isForcedCorners ? 
					'You can now change position of corner buttons with mouse clicks.' :
					'Computer vision will now try to determine corners of the table.'
			videoInputApplicationState.processPanel.flashText(infoFlashText, e.x, e.y)
			if (!billiardVisionEngineState.isForcedCorners) {
				billiardVisionEngineState.table.backLeftCornerPoint = new InferredTableCornerPoint()
				billiardVisionEngineState.table.backRightCornerPoint = new InferredTableCornerPoint()
				billiardVisionEngineState.table.frontLeftCornerPoint = new InferredTableCornerPoint()
				billiardVisionEngineState.table.frontRightCornerPoint = new InferredTableCornerPoint()
			}
			videoInputApplicationState.mainThread.interrupt()
		}
	}
	
	private void whenRightButtonPressedChangeCornerPointToDetermine(MouseEvent e) {
		if (e.button == 3) {
			determineCornerIndex = (++determineCornerIndex) % 4
			videoInputApplicationState.processPanel.flashText("Now determine next corner, please.", e.x, e.y)
		}
	}

}
