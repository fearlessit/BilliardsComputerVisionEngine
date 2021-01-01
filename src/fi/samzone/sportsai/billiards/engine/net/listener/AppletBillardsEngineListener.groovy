package fi.samzone.sportsai.billiards.engine.net.listener

import java.applet.Applet

import java.awt.Graphics

import fi.samzone.sportsai.billiards.engine.net.dto.*
import groovyjarjarasm.asm.util.TraceAnnotationVisitor

import java.awt.Color
import static fi.samzone.sportsai.vision.constants.Color.*

class AppletBillardsEngineListener extends Applet {

	final Integer SCREEN_SIZE_WIDTH = 1000
	final Integer SCREEN_SIZE_HEIGHT = 500

	final Integer TABLE_SIZE_WIDTH = 400
	final Integer TABLE_SIZE_HEIGHT = 200
	
	final Integer BALL_RADIUS = 10
	final Integer SPOT_RADIUS = 5
	

	final Integer BILLIARD_VISION_ENGINE_APPLET_LISTENER_PORT = 5002

	List<BallDTO> balls = []
	List<BallDTO> detectedBalls = []
	List<TableSpotDTO> tableSpots = []
	
	public void start() {
		resize(SCREEN_SIZE_WIDTH +20, SCREEN_SIZE_HEIGHT +20)
		BilliardEngineListenerThread listener = new BilliardEngineListenerThread(BILLIARD_VISION_ENGINE_APPLET_LISTENER_PORT, { BilliardEngineStateDTO state ->
			this.balls = state.balls
			this.detectedBalls = state.detectedBalls
			this.tableSpots = state.tableSpots 
			this.repaint()
		})
		listener.start()
		println "Billiard listener applet created."
	}

	public void paint(Graphics g) {
		paintBalls(g)
		paintTableSpots(g)
	}
	
	private void paintBalls(Graphics g) {
		g.setColor(java.awt.Color.GREEN)
		for (ball in balls) {
			float scaleX = SCREEN_SIZE_WIDTH / TABLE_SIZE_WIDTH
			float scaleY = SCREEN_SIZE_HEIGHT / TABLE_SIZE_HEIGHT
			g.fillOval(
					((ball.x*scaleX) as int) -BALL_RADIUS,
					((ball.y*scaleY) as int) -BALL_RADIUS,
					BALL_RADIUS*2, BALL_RADIUS*2)
		}
		
		g.setColor(java.awt.Color.BLUE)
		for (ball in detectedBalls) {
			float scaleX = SCREEN_SIZE_WIDTH / TABLE_SIZE_WIDTH
			float scaleY = SCREEN_SIZE_HEIGHT / TABLE_SIZE_HEIGHT
			g.drawOval(
					((ball.x*scaleX) as int) -BALL_RADIUS,
					((ball.y*scaleY) as int) -BALL_RADIUS,
					BALL_RADIUS*2 +3, BALL_RADIUS*2 +3)
		}

	}

	private void paintTableSpots(Graphics g) {
		g.setColor(java.awt.Color.BLACK)
		for (spot in tableSpots) {
			float scaleX = SCREEN_SIZE_WIDTH / TABLE_SIZE_WIDTH
			float scaleY = SCREEN_SIZE_HEIGHT / TABLE_SIZE_HEIGHT
			g.fillOval(
					((spot.x*scaleX) as int) -SPOT_RADIUS,
					((spot.y*scaleY) as int) -SPOT_RADIUS,
					SPOT_RADIUS*2, SPOT_RADIUS*2)
		}
		
	}

}
