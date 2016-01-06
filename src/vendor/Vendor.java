package vendor;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;

import tools.Commit;
import tools.Constants;
import tools.RSA;
import tools.SHA;

public class Vendor {

	static private RSA brokerKeys, myKeys = new RSA(Constants.RSAsize);
	static private String brokerIdentity;
	static private String vendorIdentity = "VendorulCelSmecher";

	void requestBroker(String brokerIp, int brokerPort) throws Exception {
		Socket vendorToBrokerSocket = new Socket(brokerIp, brokerPort);
		DataOutputStream outToBroker = new DataOutputStream(vendorToBrokerSocket.getOutputStream());
		ObjectInputStream inFromBroker = new ObjectInputStream(
				new BufferedInputStream(vendorToBrokerSocket.getInputStream()));
		outToBroker.writeUTF("pk");
		outToBroker.flush();
		String n, e;
		brokerIdentity = (String) inFromBroker.readObject();
		n = (String) inFromBroker.readObject();
		e = (String) inFromBroker.readObject();
		brokerKeys = new RSA(new BigInteger(n), new BigInteger(e));

		outToBroker.close();
		inFromBroker.close();
		vendorToBrokerSocket.close();

	}

	void acceptClient(Socket clientSocket) throws Exception {
		ObjectOutputStream outToClient = new ObjectOutputStream(clientSocket.getOutputStream());
		ObjectInputStream inFromClient = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));

		String n, e;
		n = (String) inFromClient.readObject();
		e = (String) inFromClient.readObject();
		RSA clientKeys = new RSA(new BigInteger(n), new BigInteger(e));

		outToClient.writeObject(vendorIdentity);
		outToClient.flush();
		outToClient.writeObject(myKeys.getN().toString());
		outToClient.flush();
		outToClient.writeObject(myKeys.getE().toString());
		outToClient.flush();

		Commit commitU = (Commit) inFromClient.readObject();
		String sigCommitU = (String) inFromClient.readObject();
		System.out.println(sigCommitU);
		System.out.println(myKeys.decrypt(sigCommitU));
		System.out.println(SHA.sha1(commitU.concatAll()));

		outToClient.close();
		inFromClient.close();
	}

	public static void main(String argv[]) throws Exception {
		Vendor v = new Vendor();
		v.requestBroker(Constants.brokerIp, Constants.brokerPort);

		ServerSocket welcomeSocket = new ServerSocket(Constants.vendorPort);
		while (true) {
			Socket connectionSocket = welcomeSocket.accept();
			v.acceptClient(connectionSocket);
		}

	}

}
