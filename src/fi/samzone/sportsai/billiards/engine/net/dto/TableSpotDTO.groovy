package fi.samzone.sportsai.billiards.engine.net.dto

import org.opencv.core.Point

import fi.samzone.sportsai.billiards.tableobjects.ShortTermSpot
import fi.samzone.sportsai.billiards.tableobjects.smartvision.InferredBall

class TableSpotDTO implements Serializable {
	
	int x
	int y

	public TableSpotDTO() {
	}

	public TableSpotDTO(ShortTermSpot shortTermSpot) {
		this.x = shortTermSpot.tablePoint.x as int
		this.y = shortTermSpot.tablePoint.y as int
	}


	public String toString() {
		return "($x,$y)"
	}

}
