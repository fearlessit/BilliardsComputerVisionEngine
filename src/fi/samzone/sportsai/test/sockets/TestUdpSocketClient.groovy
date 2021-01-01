package fi.samzone.sportsai.test.sockets

class TestUdpSocketClient {

	static main(args) {
		byte[] data = "Original Message".getBytes("ASCII")
		InetAddress addr = InetAddress.getByName("localhost")
		int port = 5000
		DatagramPacket packet = new DatagramPacket(data, data.length, addr, port)
		DatagramSocket socket = new DatagramSocket()
		socket.send(packet)
		socket.setSoTimeout(30000) // block for no more than 30 seconds
		byte[] buffer = (' ' * 4096) as byte[]
		DatagramPacket response = new DatagramPacket(buffer, buffer.length)
		socket.receive(response)
		String s = new String(response.data, 0, response.length)
		println "Server said: '$s'"
	}

}
