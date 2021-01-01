package fi.samzone.sportsai.billiards.tableobjects.smartvision

import org.opencv.core.Point

import fi.samzone.sportsai.vision.algorithms.GeometricMath
import fi.samzone.sportsai.vision.algorithms.VisionAlgorithms
import fi.samzone.sportsai.vision.datastructures.CappedQueue
import static fi.samzone.sportsai.vision.algorithms.GeometricMath.*
import fi.samzone.utils.ConfigurationProperties

/*
 * Use rough visioning ball detection techniques as base input. Then
 * collecting near history data (second or so) and with use of a fact that
 * balls moves continuously tries to remove false ball detections and make
 * more reliable true detections. With short history data there is better
 * ways to handle problematic situations where balls are very close each
 * other or edge of table ball detection area.
 */
class InferredBall implements Serializable {

	final static Integer SAMPLE_FRAME_RATE = 3 * 25
	final static Integer MOVEMENT_MAX_DISTANCE = (ConfigurationProperties.read('Table_detection_image_width') as int) / 10
	final static Float STILL_MAX_DISTANCE = MOVEMENT_MAX_DISTANCE / 1.5
	final static Integer MIN_FRAMES_FOR_RECOGNIZATION = 25
	final static Float FRAME_CONFIRMATION_PERCENTAGE = 0.25f
	
	CappedQueue lastPoints = new CappedQueue(InferredBall.SAMPLE_FRAME_RATE)
	
	private Boolean isStillInPreviousFrame = false
	private Boolean isStillInCurrentFrame = false
	
	Boolean isLikelyRecognizedAsBall(frameConfirmationPercentage = FRAME_CONFIRMATION_PERCENTAGE) {
		return lastPoints.size() > MIN_FRAMES_FOR_RECOGNIZATION &&
				validPoints.size() / (lastPoints.size() as Float) > frameConfirmationPercentage
	}

	Boolean isStill() {
		List points = validPoints
		return !points.any { p1 ->
			points.find { p2 ->
				distance(p1,p2) > STILL_MAX_DISTANCE
			}
		}
	}
	
	Boolean startMovingNow() {
		return !isStillInCurrentFrame && isStillInPreviousFrame //&& isLikelyRecognizedAsBall()
	}

	Boolean isNewPointPossible(Point newPoint) {
		return distance(newPoint, lastValidPoint) < InferredBall.MOVEMENT_MAX_DISTANCE
	}

	Boolean exists() {
		return lastPoints.elements.any { it }
	}

	Point getAveragePoint() {
		List points = validPoints
		return points.size() > 0 ?
				getGeometricAverage(points) :
				new Point(0, 0)
	}

	Point getLastValidPoint() {
		return lastPoints.elements.find { it } ?: new Point(0,0)
	}

	List<Point> getValidPoints() {
		return lastPoints.elements.findAll { it }
	}

	public String toString() {
		Point ballPoint = lastValidPoint
		return "${ballPoint.x as int},${ballPoint.y as int}"
	}


	public static void main(String[] args) {
		InferredBall lb = new InferredBall()

		lb.lastPoints << new Point(0,0)
		lb.lastPoints << new Point(3,1)
		lb.lastPoints << null
		lb.lastPoints << new Point(12,2)
		lb.lastPoints << null

		println lb.lastPoints.elements
		println lb.lastValidPoint
		println lb.averagePoint
		println lb.isStill()
	}

}
