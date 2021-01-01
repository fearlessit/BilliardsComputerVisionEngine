package fi.samzone.sportsai.billiards.engine.net.dto

import fi.samzone.sportsai.billiards.engine.BilliardVisionEngineState
import fi.samzone.sportsai.billiards.tableobjects.smartvision.InferredBall

class BilliardEngineStateDTO implements Serializable {

	List<BallDTO> balls = []
	List<BallDTO> detectedBalls = []
	List<TableSpotDTO> tableSpots = []
	

	public BilliardEngineStateDTO() {
	}

	public BilliardEngineStateDTO(BilliardVisionEngineState engineState) {
		balls = engineState.table.balls.findAll { it.isLikelyRecognizedAsBall()}.collect { InferredBall likelyBall ->
			return new BallDTO(likelyBall.lastValidPoint)
		}
		detectedBalls = engineState.detectedBallsInFrame.collect { new BallDTO(it) }
		tableSpots = engineState.table.shortTermSpots.collect { new TableSpotDTO(it) }
	
	}

	public String toString() {
		return balls.toString()
	}
}
