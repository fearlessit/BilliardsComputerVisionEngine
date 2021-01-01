package fi.samzone.sportsai.billiards.tableobjects

import org.opencv.core.Point

class ShortTermSpot {
	
	final static Integer DEFAULT_LIVE_TIME = 500
	
	Point tablePoint
	Integer framesToLive
	
	ShortTermSpot(Point tablePoint = new Point(200, 100), Integer timeToLive = ShortTermSpot.DEFAULT_LIVE_TIME) {
		this.framesToLive = timeToLive
		this.tablePoint = tablePoint
	}
	
	void decreaseLivingTime() {
		if (framesToLive > 0)
			framesToLive--
	}
	
	boolean isAlive() {
		return framesToLive > 0
	}
}
