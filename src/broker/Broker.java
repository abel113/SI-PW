package broker;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;

import tools.Certificate;
import tools.Constants;
import tools.RSA;
import tools.SHA;

class Broker {
	static private String brokerIdentity = "BrokerSef";
	static private RSA myKeys = new RSA(Constants.RSAsize);
	private static ServerSocket welcomeSocket;

	static void acceptRequest(Socket connectionSocket) throws Exception {
		String clientIdentity, request, e;
		DataInputStream inFromClient = new DataInputStream(new BufferedInputStream(connectionSocket.getInputStream()));
		ObjectOutputStream outToClient = new ObjectOutputStream(connectionSocket.getOutputStream());

		request = inFromClient.readUTF();

		System.out.println(request);

		if (request.trim().equals("new_customer"))
			acceptClient(connectionSocket, inFromClient, outToClient);

		if (request.trim().equals("pk")) {
			outToClient.writeObject(brokerIdentity);
			outToClient.flush();
			outToClient.writeObject(myKeys.getN().toString());
			outToClient.flush();
			outToClient.writeObject(myKeys.getE().toString());
			outToClient.flush();
		}

		inFromClient.close();
		outToClient.close();
		connectionSocket.close();
	}

	static void acceptClient(Socket connectionSocket, DataInputStream inFromClient, ObjectOutputStream outToClient)
			throws Exception {
		String clientIdentity, n, e;

		// Primim identitate+cheia publica RSA

		clientIdentity = inFromClient.readUTF();
		n = inFromClient.readUTF();
		e = inFromClient.readUTF();

		// Pregatim data pentru certificat
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.HOUR, Constants.ceritficateTime);
		Date dataExp = cal.getTime();

		// Cream certificat si trimitem
		Certificate CU = new Certificate(clientIdentity, brokerIdentity, n, e, myKeys.getN().toString(),
				myKeys.getE().toString(), dataExp, "");
		outToClient.writeObject(CU);
		RSA clientKeys = new RSA(new BigInteger(n), new BigInteger(e));

		String sh = myKeys.sign(SHA.sha1(CU.concatAll()));
		outToClient.writeObject(sh);
	}

	public static void main(String argv[]) throws Exception {

		welcomeSocket = new ServerSocket(Constants.brokerPort);

		while (true) {
			Socket connectionSocket = welcomeSocket.accept();
			acceptRequest(connectionSocket);
		}
	}
}
