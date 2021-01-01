package fi.samzone.sportsai.billiards.ui.components

import java.awt.Graphics
import java.awt.GridLayout

import javax.swing.JLabel
import javax.swing.JPanel

import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.videoio.VideoCapture

import fi.samzone.sportsai.billiards.engine.BilliardVisionEngineState
import fi.samzone.sportsai.billiards.tableobjects.smartvision.InferredTableCornerPoint
import fi.samzone.sportsai.billiards.ui.videosource.VideoInputApplicationState

class InfoPanel extends JPanel {
	
	BilliardVisionEngineState engineState
	VideoInputApplicationState videoApplicationState

	List<JLabel> labelValues

	InfoPanel(BilliardVisionEngineState engineState, VideoInputApplicationState state) {
		super()
		this.engineState = engineState
		this.videoApplicationState = state

		Map infoMap = getInfoMap(state)
		labelValues = new LinkedList<JLabel>()
		
		Set infoKeys = infoMap.keySet()
		int rows = infoKeys.size()
		setLayout(new GridLayout(rows, 2))
		int y=0
		for (infoKey in infoMap.keySet()) {
			String infoValue = infoMap.get(infoKey).toString()
			add(new JLabel(infoKey), y++, 0)
			JLabel labelValue = new JLabel(infoValue)
			add(labelValue, y++, 1)
			labelValues << labelValue
		}
	}
	
	void updateInfos() {
		Map infoMap = getInfoMap(videoApplicationState)
		int y=0
		for (infoKey in infoMap.keySet()) {
			String infoValue = infoMap.get(infoKey).toString()
			labelValues[y++].text = infoValue
		}

	}

	Map getInfoMap(VideoInputApplicationState state) {
		return [
				'Flood fill diff': engineState.floodFillDiff,
				'Sleep between frames': state.sleepBetweenFrames,
				'Ball minimum radius' : engineState.ballMinRadius,
				'Ball maximum radius' : engineState.ballMaxRadius,
				'Max contour distance for same object' : engineState.maxContourDistanceWithSameObject,
				'Left front corner point': engineState.table.frontRightCornerPoint,
				'Right front corner point': engineState.table.frontLeftCornerPoint,
				'Back left corner point': engineState.table.backLeftCornerPoint,
				'Back right corner point': engineState.table.backRightCornerPoint,
				'Detected balls in frame': engineState.detectedBallsInFrame?.size(),
				'Inferred balls': engineState.table.balls.size(),
				'Frame processing time (ms)': engineState.frameProcessingTimeInMillis +
						" (${(1000 / (engineState.frameProcessingTimeInMillis+1)) as int} FPS)",
				'Video position': getVideoPositionString(),
		]
	}
	
	private String getVideoPositionString() {
		double ms = videoApplicationState.camera.get(0)
		String totalSeconds = String.format ("%.2f", ms / 1000)
		String seconds = (((ms % 60000)/1000) as int) as String
		if (seconds.length() == 1)
			seconds = '0' + seconds
		String minutes = ((ms / 60000) as int) as String
		if (minutes.length() == 1)
			minutes = '0' + minutes
		String milliSeconds = (((ms % 1000)/100) as int) as String
		return "$minutes:$seconds.$milliSeconds - ($totalSeconds seconds)"
	}


}
