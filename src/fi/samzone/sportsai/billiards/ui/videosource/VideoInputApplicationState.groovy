package fi.samzone.sportsai.billiards.ui.videosource
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.videoio.VideoCapture

import fi.samzone.sportsai.billiards.tableobjects.Table
import fi.samzone.sportsai.billiards.tableobjects.smartvision.InferredBall
import fi.samzone.sportsai.billiards.tableobjects.smartvision.InferredTableCornerPoint
import fi.samzone.sportsai.vision.ui.componenets.MatPanel
import fi.samzone.sportsai.vision.ui.componenets.MatPanelWithTextFlash

class VideoInputApplicationState implements Serializable {

	int sleepBetweenFrames = 10

	VideoCapture camera
	MatPanelWithTextFlash processPanel
	MatPanel smallPanel
	MatPanel tablePanel
	MatPanel binaryTablePanel
	
	Boolean isPaused = false
	String command = null
	
	Thread mainThread

}

