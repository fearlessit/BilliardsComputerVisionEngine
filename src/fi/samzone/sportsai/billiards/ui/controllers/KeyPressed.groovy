package fi.samzone.sportsai.billiards.ui.controllers
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

import fi.samzone.sportsai.billiards.engine.BilliardVisionEngineState
import fi.samzone.sportsai.billiards.ui.videosource.VideoInputApplicationState

class KeyPressed extends KeyAdapter {
	
	BilliardVisionEngineState engineState
	VideoInputApplicationState videoApplicationState
	
	static String numberEntered = ''
	
	KeyPressed(BilliardVisionEngineState engineState, VideoInputApplicationState videoApplicationSate) {
		this.engineState = engineState
		this.videoApplicationState = videoApplicationSate
	}
	
	void keyPressed(KeyEvent e) {
		switch (e.keyCode) {
			case 109 :
				engineState.floodFillDiff -= 0.1
				break
			case 107 :
				engineState.floodFillDiff += 0.1
				break
			case 38:
				int inc = videoApplicationState.sleepBetweenFrames > 150 ? 50 : 1
				videoApplicationState.sleepBetweenFrames-=inc
				if (videoApplicationState.sleepBetweenFrames < 0)
					videoApplicationState.sleepBetweenFrames = 0
				break
			case 40:
				int inc = videoApplicationState.sleepBetweenFrames > 100 ? 50 : 1
				videoApplicationState.sleepBetweenFrames+=inc
				break
			case 37:
				videoApplicationState.command = 'REWIND,-2000'
				videoApplicationState.isPaused = false
				engineState.table.balls.clear()
				engineState.table.shortTermSpots.clear()
				break
			case 39:
				videoApplicationState.command = 'REWIND,2000'
				videoApplicationState.isPaused = false
				engineState.table.balls.clear()
				engineState.table.shortTermSpots.clear()
				break
			case 90:
				videoApplicationState.command = 'REWIND,-60000'
				videoApplicationState.isPaused = false
				engineState.table.balls.clear()
				engineState.table.shortTermSpots.clear()
				break
			case 88:
				videoApplicationState.command = 'REWIND,60000'
				videoApplicationState.isPaused = false
				engineState.table.balls.clear()
				engineState.table.shortTermSpots.clear()
				break
			case 48..57:
				numberEntered += ((e.keyCode as int) -48) as String
				break
			case 10:
				println "Set position to: ${numberEntered}"
				double value = numberEntered.isNumber() ? (numberEntered as int) * 1000 : 0
				videoApplicationState.command = "POSITION,${value as int}"
				numberEntered = ''
				videoApplicationState.isPaused = false
				engineState.table.balls.clear()
				engineState.table.shortTermSpots.clear()
				break
			case 83:
				videoApplicationState.command = "POSITION,0"
				videoApplicationState.isPaused = false
				engineState.table.balls.clear()
				engineState.table.shortTermSpots.clear()
				break
			case 32:
				videoApplicationState.isPaused = videoApplicationState.isPaused == false
				break

		}
		videoApplicationState.mainThread.interrupt()
	//println "KEY PRESSED: ${e.keyCode}"
	}

}

