package fi.samzone.sportsai.billiards.engine.net.server

import org.opencv.core.Core

import fi.samzone.sportsai.billiards.engine.BilliardVisionEngine
import fi.samzone.sportsai.billiards.engine.BilliardVisionEngineState
import fi.samzone.sportsai.billiards.engine.net.dto.BilliardEngineStateDTO
import fi.samzone.sportsai.billiards.engine.net.dto.MatDTO
import fi.samzone.sportsai.billiards.engine.net.server.input.VideoFileStreamer
import fi.samzone.sportsai.billiards.ui.videosource.VideoInputApplicationState

import org.opencv.core.Mat
import org.opencv.videoio.VideoCapture


class BilliardEngineServer extends Thread {

	final Integer DEFAULT_INPUT_STREAM_PORT = 5001

	static final String DEFAULT_LISTENER_HOST = 'localhost'
	static final Integer DEFAULT_LISTENER_PORT = 5002

	static {
		println "Load OpenCV Native library: ${Core.NATIVE_LIBRARY_NAME}"
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
	}

	BilliardVisionEngine engine
	List<Map> engineStateListeners

	private Long frameStartEpoch
	private Long frameCounter = 0

	public BilliardEngineServer(engineStateListeners) {
		this.engineStateListeners = engineStateListeners
		engine = new BilliardVisionEngine()
	}

	public void run() {
		ServerSocket serverSocket = new ServerSocket(DEFAULT_INPUT_STREAM_PORT)
		println "Billiard Vision Server is started and is waiting client to connect."
		while (true) {
			serverSocket.accept() { inputSocket ->
				println "New video source connected ($inputSocket). Start processing it and send detected balls to listeners."
				processInputSocket(inputSocket)
			}
		}
	}

	private void processInputSocket(Socket inputSocket) {
		inputSocket.withObjectStreams { ois, oos ->
			while (true) {
				engine.engineState.cameraMat = ((MatDTO) ois.readObject()).mat 
				engine.processFrame()
				informAllClientListeners(engine.engineState)
			}
		}
	}

	private void informAllClientListeners(BilliardVisionEngineState engineState)  {
		BilliardEngineStateDTO billardEngineStateDTO = new BilliardEngineStateDTO(engineState)
		for (inetAddress in engineStateListeners) {
			try {
				Socket socket = new Socket(inetAddress.host, inetAddress.port)
				socket.withObjectStreams { inputStream, outputStream ->
					outputStream << billardEngineStateDTO
				}
			} catch (e) {
				println "WARNING: Error when sending state to listener ${inetAddress.host}:${inetAddress.port}: ${e.message}."
			}
		}
	}

	
	static main(args) {
		List clientSocketListeners = []
		clientSocketListeners << [host: DEFAULT_LISTENER_HOST, port: DEFAULT_LISTENER_PORT]
		BilliardEngineServer server = new BilliardEngineServer(clientSocketListeners)
		server.start()
	}
	
}