package fi.samzone.sportsai.vision.ui.componenets

import java.awt.Graphics
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.awt.image.WritableRaster
import javax.swing.JPanel
import org.opencv.core.Mat

class MatPanel extends JPanel {

	Mat mat
	
	public void paint(Graphics g) {
		g.drawImage(matToBufferedImage(mat), 0, 0, this);
	}
	
	public void repaint(Mat mat) {
		this.mat = mat
		super.repaint()
	}
	
	protected BufferedImage matToBufferedImage(Mat frame) {
		if (!frame) return null
		int colorType = frame.channels() == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR  
		BufferedImage image = new BufferedImage(frame.width(), frame.height(), colorType)
		frame.get(0, 0, image.raster.dataBuffer.data)
		return image
	}
	
	
}
