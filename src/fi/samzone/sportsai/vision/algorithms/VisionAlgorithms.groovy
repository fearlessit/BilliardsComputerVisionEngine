package fi.samzone.sportsai.vision.algorithms

import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect2d
import org.opencv.core.Scalar

import fi.samzone.sportsai.vision.datastructures.CappedQueue
import static fi.samzone.sportsai.vision.algorithms.GeometricMath.*

/**
 * From given line finds center point of given target color.
 * 
 * @param img			picture
 * @param targetColor	color to look for
 * @param lineLength	length of line
 * @param x				line start x-coordinate
 * @param y				line start y-coordinate
 * @param xAdd			x increment, f.e. 1
 * @param yAdd			y increment, f.e. 0
 * @return				average center point of found matching points
 */
static Point findColorOnLine(Mat image, Scalar targetColor, int lineLength, int x, int y, int xAdd, int yAdd) {
	List foundX = []
	List foundY = []
	lineLength.times {
		Scalar color = new Scalar(image.get(y, x))
		if (color == targetColor) {
			foundX << x
			foundY << y
		}
		x += xAdd
		y += yAdd
	}
	return foundX ? new Point(foundX.sum() / foundX.size() as int, foundY.sum() / foundY.size() as int) : new Point(0,0)
}

static boolean isContoursOfSameObject(contour1, contour2, double maxDistance) {
	Rect2d boundingRectangle1 = getBoundingRectangle(contour1, maxDistance)
	Rect2d boundingRectangle2 = getBoundingRectangle(contour2, maxDistance)
	
	List<Point> restrictedContour1 = contour1.findAll { boundingRectangle2.contains(it) }
	List<Point> restrictedContour2 = contour2.findAll { boundingRectangle1.contains(it) }
	
	for (Point p1 in restrictedContour1)
		for (Point p2 in restrictedContour2)
			if (rectilinearDistance(p1,p2) <= maxDistance)
				return true
	return false
}

static Point findSimilarPointOnCenterSpiral(Mat image, Number diff = 20, int angleSteps = 100, double radiusIncrement = 0.5, double xFix = 0.57, double yFix = 0.33) {
	double angleIncrement = (Math.PI * 2) / angleSteps
	Scalar lastColor = new Scalar(-diff, -diff, -diff)
	int x, y, countSimilarLastOnes = 0
	double a = 0, radius = 0
	while (countSimilarLastOnes <= 30 || radius > (image.height() / 2)) {
		x = Math.cos(a) * radius +(image.width() * xFix)
		y = Math.sin(a) * radius +(image.height() * yFix)
		Scalar color = averageNeighbourColor(image, x, y)
		double diffToLast = Math.abs(color.val[0] -lastColor.val[0]) +Math.abs(color.val[1] -lastColor.val[1]) +Math.abs(color.val[2] -lastColor.val[2])
		if (diffToLast <= diff)
			countSimilarLastOnes++
		lastColor = color
		a += angleIncrement
		radius += radiusIncrement
	}
	return new Point(x, y)
}

static Scalar averageNeighbourColor(Mat image, int xCoordinate, int yCoordinate, int radius = 1) {
	int pixelCounter = 0
	double b = 0
	double g = 0
	double r = 0
	for (int x = Math.max(xCoordinate -radius, 0); x <= Math.min(xCoordinate +radius, image.width() -1); x++) {
		for (int y = Math.max(yCoordinate -radius, 0); y <= Math.min(yCoordinate +radius, image.height() -1); y++) {
			def color = image.get(y, x)
			if (color) {
				b += color[0]
				g += color[1]
				r += color[2]
				pixelCounter++
			}
		}
	}
	return pixelCounter != 0 ? new Scalar(b / pixelCounter, g / pixelCounter, r / pixelCounter) : new Scalar(0,0,0)
}

static Rect2d getBoundingRectangle(points, padding = 0) {
	double minX, maxX, minY, maxY
	if (points.size() > 0) {
		minX = maxX = points[0].x
		minY = maxY = points[0].y
	}
	for (Point point in points) {
		if (point.x < minX) minX = point.x
		if (point.x > maxX) maxX = point.x
		if (point.y < minY) minY = point.y
		if (point.y > maxY) maxY = point.y
	}
	return new Rect2d(new Point(minX -padding, minY -padding), new Point(maxX +padding, maxY +padding))
}

