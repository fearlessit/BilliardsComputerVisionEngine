package fi.samzone.sportsai.billiards.engine.net.dto

import org.opencv.core.Mat

class MatDTO implements Serializable {

	byte[] data
	int rows
	int cols
	int type

	MatDTO(Mat mat) {
		data = new byte[mat.total() * mat.elemSize()]
		mat.get(0, 0, data)
		rows = mat.rows()
		cols = mat.cols()
		type = mat.type()

	}

	public getMat() {
		Mat mat = new Mat(rows, cols, type)
		mat.put(0,0, data)
		return mat
	}

}
