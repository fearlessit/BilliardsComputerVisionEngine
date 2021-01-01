package fi.samzone.sportsai.billiards.engine.net.listener

import fi.samzone.sportsai.billiards.engine.net.dto.BilliardEngineStateDTO

class BilliardEngineListenerThread extends Thread {

	Closure newStateClosure
	ServerSocket serverSocket

	BilliardEngineListenerThread(Integer socketPort, Closure newStateClosure) {
		super()
		this.newStateClosure = newStateClosure
		serverSocket = new ServerSocket(socketPort)
	}

	void run() {
		println "Billiard engine listener started."
		while (true) {
			serverSocket.accept() { socket ->
				socket.withObjectStreams { inputStream, outputStream ->
					BilliardEngineStateDTO billiarEngineStateDTO = inputStream.readObject()
					newStateClosure.call(billiarEngineStateDTO)
				}
			}
		}
	}

	static void main (args) {
		(new BilliardEngineListenerThread(5001, { println "New balls: $it" } )).start()
	}
}
