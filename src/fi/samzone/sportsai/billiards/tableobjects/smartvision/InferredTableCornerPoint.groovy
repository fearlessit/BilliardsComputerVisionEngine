package fi.samzone.sportsai.billiards.tableobjects.smartvision

import org.opencv.core.Core
import org.opencv.core.Point

import fi.samzone.sportsai.vision.algorithms.VisionAlgorithms
import fi.samzone.sportsai.vision.datastructures.CappedQueue

import static fi.samzone.sportsai.vision.algorithms.GeometricMath.*
import fi.samzone.utils.ConfigurationProperties


/*
 * This table's corner point uses AI visioning techniques to return constant point with
 * situations where observed corner point has some random interference with signal or even
 * some totally false, but not long lasting, false values.
 * 
 * This is achieved by taking average value for short time window and use geometric
 * median from those averaged values.
 */
class InferredTableCornerPoint extends Point {

	private int shortTimePointsSize
	private int counter = 0
	private CappedQueue shortTimePoints
	private CappedQueue longTimePoints

	public InferredTableCornerPoint() {
		this(ConfigurationProperties.readInt('Table_short_time_corner_points_size'), ConfigurationProperties.readInt('Table_short_time_corner_points_size'))
	}

	public InferredTableCornerPoint(int shortTimePointsSize, int longTimePointsSize) {
		this.shortTimePointsSize = shortTimePointsSize
		this.shortTimePoints = new CappedQueue(shortTimePointsSize)
		this.longTimePoints = new CappedQueue(longTimePointsSize)
	}

	public void recordPoint(Point newPoint) {
		this.shortTimePoints.push(newPoint)
		Point meanPoint = getMeanPoint()
		if (counter++ % shortTimePointsSize == 0)
			this.longTimePoints.push(meanPoint)
		Point geometricMedianPoint = getGeometricMedian(this.longTimePoints.elements)
		this.x = geometricMedianPoint.x
		this.y = geometricMedianPoint.y
	}

	public Point getMeanPoint() {
		double size = this.shortTimePoints.elements.size()
		if (size == 0)
			return new Point(0,0)
		double meanX = this.shortTimePoints.elements*.x.sum() / size
		double meanY = this.shortTimePoints.elements*.y.sum() / size
		return new Point(meanX, meanY)
	}

}
