package fi.samzone.sportsai.test.sockets

class TestUdpSocketServer {

	static main(args) {
		DatagramSocket socket = new DatagramSocket(5000)
		byte[] buffer = (' ' * 4096) as byte[]
		int i = 0
		while(true) {
			DatagramPacket incoming = new DatagramPacket(buffer, buffer.length)
			socket.receive(incoming)
			String s = new String(incoming.data, 0, incoming.length)
			String reply = "Client said ($i): '$s'"
			DatagramPacket outgoing = new DatagramPacket(reply.bytes, reply.size(),
					incoming.address, incoming.port)
			socket.send(outgoing)
			i++
		}
	}

}
