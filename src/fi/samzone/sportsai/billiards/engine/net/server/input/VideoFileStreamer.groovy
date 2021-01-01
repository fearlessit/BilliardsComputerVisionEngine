package fi.samzone.sportsai.billiards.engine.net.server.input

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.videoio.VideoCapture

import fi.samzone.sportsai.billiards.engine.net.dto.MatDTO

import static org.opencv.imgcodecs.Imgcodecs.imwrite
import fi.samzone.utils.ConfigurationProperties

class VideoFileStreamer extends Thread {

	final String DEFAULT_BILLIARD_SERVER_HOST = 'localhost'
	final Integer DEFAULT_BILLIARD_SERVER_PORT = 5001
	final String DEFAULT_VIDEO_PATH = "assets/video/${ConfigurationProperties.read('input_video_file_name')}"
	final Integer FRAME_DELAY = 0

	Long startEpoch
	VideoCapture videoCapture

	static {
		println "Load OpenCV Native library: ${Core.NATIVE_LIBRARY_NAME}"
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
	}

	public VideoFileStreamer() {
		super()
		videoCapture = new VideoCapture(DEFAULT_VIDEO_PATH)
		startEpoch = System.currentTimeMillis()
	}

	public void run() {
		Socket socket = new Socket(DEFAULT_BILLIARD_SERVER_HOST, DEFAULT_BILLIARD_SERVER_PORT)
		println "Video stream Connection created to billiard server, using video: ${DEFAULT_VIDEO_PATH.split('/')[-1]}"
		socket.withObjectStreams { objectInputStream, objectOutputStream ->
			while (true) {
				captureAndSendFrame(objectOutputStream)
				Thread.sleep(FRAME_DELAY)
			}
		}
	}

	private void captureAndSendFrame(objectOutputStream) {
		Mat capturedFrame = retrieveFrame()
		MatDTO matFrameDTO = new MatDTO(capturedFrame)
		objectOutputStream << matFrameDTO
	}

	private Mat retrieveFrame() {
		long nowEpoch = System.currentTimeMillis()
		videoCapture.set(0, nowEpoch -startEpoch)
		Mat videoMat = new Mat()
		videoCapture.read(videoMat)
		return videoMat
	}

	static main(args) {
		VideoFileStreamer videoFileStreamer = new VideoFileStreamer()
		videoFileStreamer.start()
	}
}
