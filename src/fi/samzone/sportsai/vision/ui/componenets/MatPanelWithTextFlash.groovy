package fi.samzone.sportsai.vision.ui.componenets

import java.awt.Graphics

class MatPanelWithTextFlash extends MatPanel {
	
	MatPanelWithTextFlash() {
		super()
	}
	
	String flashText = ''
	int xPosition = 0
	int yPosition = 0
	boolean isTextShowing
	
	void flashText(flashText, xPosition, yPosition, int framesToFlash = 2000) {
		this.flashText = flashText
		this.xPosition = xPosition
		this.yPosition = yPosition
		this.isTextShowing = true
		repaint()
		
		new Thread( {
			sleep(framesToFlash) 
			this.isTextShowing = false
			repaint() 
		}).start()
	}
	
	public void paint(Graphics g) {
		g.drawImage(matToBufferedImage(mat), 0, 0, this);
		
		if (this.isTextShowing) {
			g.drawString(flashText, xPosition, yPosition)
		}
		
	}

}
