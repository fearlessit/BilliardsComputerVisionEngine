package fi.samzone.sportsai.billiards.engine.net.listener

final Integer BILLIARD_VISION_ENGINE_CONSOLE_LISTENER_PORT = 5001

println "Console billiards engine listener started."
ServerSocket serverSocket = new ServerSocket(BILLIARD_VISION_ENGINE_CONSOLE_LISTENER_PORT)
while (true) {
	serverSocket.accept() { socket ->
		socket.withObjectStreams { ois, oos ->
			def object = ois.readObject()
			println "Recieved balls: $object"
		}
	}
}
