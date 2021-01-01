package fi.samzone.sportsai.billiards.tableobjects

import static fi.samzone.sportsai.vision.algorithms.GeometricMath.*

import org.opencv.core.Point

import fi.samzone.sportsai.billiards.tableobjects.smartvision.InferredBall
import fi.samzone.sportsai.billiards.tableobjects.smartvision.InferredTableCornerPoint
import fi.samzone.sportsai.billiards.ui.videosource.VideoInputApplicationState

class Table {

	InferredTableCornerPoint backRightCornerPoint = new InferredTableCornerPoint()
	InferredTableCornerPoint backLeftCornerPoint = new InferredTableCornerPoint()
	InferredTableCornerPoint frontRightCornerPoint = new InferredTableCornerPoint()
	InferredTableCornerPoint frontLeftCornerPoint = new InferredTableCornerPoint()
	List<InferredBall> balls = new LinkedList<InferredBall>()
	List<ShortTermSpot> shortTermSpots = new LinkedList<ShortTermSpot>()

	public void inferBalls(List<Point> detectedBallsInFrame) {
		for (ball in balls) {
			ball.isStillInPreviousFrame = ball.isStill()
		}
		
		updateInferredBalls(detectedBallsInFrame)
		
		for (ball in balls) {
			ball.isStillInCurrentFrame = ball.isStill()
		}
		
		removeNonExistingBalls()
		fadeShortTermSpots()
	}

	void updateInferredBalls(List<Point> detectedBallsInFrame) {
		Set matchedBalls = []
		for (Point detectedBallInFrame in detectedBallsInFrame) {
			InferredBall nearestBall = (balls -matchedBalls).min { distance(detectedBallInFrame, it.lastValidPoint) }
			if (isBallMovementPlausible(nearestBall, detectedBallInFrame)) {
				nearestBall.lastPoints << detectedBallInFrame
				matchedBalls << nearestBall
			}
			else {
				InferredBall newBall = new InferredBall()
				newBall.lastPoints << detectedBallInFrame
				balls << newBall
			}
		}
		(balls -matchedBalls).each { it.lastPoints << null }
	}

	void removeNonExistingBalls() {
		balls.removeAll { !it.exists() }
	}

	void fadeShortTermSpots() {

		for (ball in balls) {
			if (ball.startMovingNow()) {
				shortTermSpots.add(new ShortTermSpot(ball.averagePoint))
			}
		}
		
		shortTermSpots.each { it.decreaseLivingTime() }
		shortTermSpots.removeAll { !it.isAlive() }
	}

	private Boolean isBallMovementPlausible(InferredBall nearestBall, Point detectedBallInFrame) {
		if (!nearestBall)
			return false
		else {
			Double distanceToNearest = distance(nearestBall.lastValidPoint, detectedBallInFrame)
			return distanceToNearest < InferredBall.MOVEMENT_MAX_DISTANCE
		}
	}

	Boolean isOngoingTurn() {
		return balls.every { it.isStill() }

	}
	
	Boolean tableNeedSideReverse() {
		return
				distance(backLeftCornerPoint, backRightCornerPoint) <
				distance(backLeftCornerPoint, frontLeftCornerPoint)
	}

}

