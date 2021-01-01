package fi.samzone.sportsai.billiards.engine.net.dto

import org.opencv.core.Point

import fi.samzone.sportsai.billiards.tableobjects.smartvision.InferredBall

class BallDTO implements Serializable {
	
	int x
	int y

	public BallDTO() {
	}

	public BallDTO(Point point) {
		this.x = point.x as int
		this.y = point.y as int
	}

	public BallDTO(InferredBall ball) {
		this.x = ball.lastValidPoint.x as int
		this.y = ball.lastValidPoint.y as int
	}

	public String toString() {
		return "($x,$y)"
	}

}
