package fi.samzone.sportsai.vision.algorithms

import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect2d
import org.opencv.core.Scalar

import fi.samzone.sportsai.vision.datastructures.CappedQueue

static Double distance(Point p1, Point p2) {
	Double dx = p2.x -p1.x
	Double dy = p2.y -p1.y
	return Math.sqrt(dx*dx + dy*dy)
}

static double rectilinearDistance(Point point1, Point point2) {
	return Math.abs(point1.x -point2.x) +Math.abs(point1.y -point2.y)
}

static double getRadius(List<Point> points) {
	double maxDistance = 0
	Iterator pointIterator1 = points.iterator()
	while (pointIterator1.hasNext()) {
		Point p1 = pointIterator1.next()
		Iterator pointIterator2 = pointIterator1
		while (pointIterator2.hasNext()) {
			Point p2 = pointIterator2.next()
			maxDistance = Math.max(distance(p1, p2), maxDistance)
		}
	}
	return maxDistance
}

static double getRadiusOptimizedWithMaxValue(points, maxRadius) {
	double maxDistance = 0
	Iterator pointIterator1 = points.iterator()
	while (pointIterator1.hasNext()) {
		Point p1 = pointIterator1.next()
		Iterator pointIterator2 = pointIterator1
		while (pointIterator2.hasNext()) {
			Point p2 = pointIterator2.next()
			maxDistance = Math.max(distance(p1, p2), maxDistance)
			if (maxDistance > maxRadius)
				return maxDistance
		}
	}
	return maxDistance
}

static Point getGeometricAverage(points) {
	new Point(points*.x.sum() / points.size(), points*.y.sum() / points.size())
}

static Point getGeometricMedian(points) {
	double minSum = Double.MAX_VALUE
	Point geometricMedianPoint = new Point(0,0)
	for (Point p in points) {
		double sum = 0
		for (Point pi in points) {
			// TODO: optimointi mahdollisuus: tämä etäisyys lasketaan kaksi kertaa jokaiselle pisteelle.
			// tosin tällä hetkellä optimointi ei ole oikein vaivan arvoista.
			sum += Math.pow(p.x -pi.x, 2) +Math.pow(p.y -pi.y, 2)
		}
		if (sum < minSum) {
			geometricMedianPoint = p
			sum = minSum
		}
	}
	return geometricMedianPoint
}
