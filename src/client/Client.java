package client;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Random;

import tools.Certificate;
import tools.Commit;
import tools.Constants;
import tools.RSA;
import tools.SHA;

class Client {
	private String clientIdentity = "Telnet";
	private RSA myKeys = new RSA(Constants.RSAsize);
	private Certificate CU;
	private String sigCU;
	private ArrayList<String> chain = new ArrayList<String>();
	// private RSA brokerKeys;

	void talkWithBroker() throws Exception {
		Socket brokerSocket = new Socket(Constants.brokerIp, Constants.brokerPort);
		DataOutputStream outToBroker = new DataOutputStream(brokerSocket.getOutputStream());
		ObjectInputStream inFromBroker = new ObjectInputStream(new BufferedInputStream(brokerSocket.getInputStream()));
		outToBroker.writeUTF("new_customer");
		outToBroker.flush();
		outToBroker.writeUTF(clientIdentity);
		outToBroker.flush();
		outToBroker.writeUTF(myKeys.getN().toString());
		outToBroker.flush();
		outToBroker.writeUTF(myKeys.getE().toString());
		outToBroker.flush();

		CU = (Certificate) inFromBroker.readObject();

		sigCU = (String) inFromBroker.readObject();
		String sh = myKeys.decrypt(sigCU);
		System.out.println(sh);
		brokerSocket.close();

		// brokerSocket = new Socket(Constants.brokerIp, Constants.brokerPort);
		// outToBroker = new DataOutputStream(brokerSocket.getOutputStream());
		// inFromBroker = new ObjectInputStream(new
		// BufferedInputStream(brokerSocket.getInputStream()));
		//
		// outToBroker.writeUTF("pk");
		// System.out.println("Broker\' public key: " +
		// inFromBroker.readObject().toString());
		//
		// brokerSocket.close();

		// Daca semnatura nu e buna
		if (!SHA.sha1(CU.concatAll()).equals(sh)) {
			System.err.println("Ceva NU e bine in comunicarea client-broker");
			System.exit(1);
		}
	}

	void generateChain(int size) {
		char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWYZ0123456789".toCharArray();
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < size; i++) {
			char c = chars[random.nextInt(chars.length)];
			sb.append(c);
		}
		chain.add(SHA.sha1(sb.toString()));
		for (int i = 1; i < size; i++)
			chain.add(SHA.sha1(chain.get(i - 1)));
		Collections.reverse(chain);
	}

	void talkWithVendor() throws Exception {
		Socket vendorSocket = new Socket(Constants.vendorIp, Constants.vendorPort);
		ObjectOutputStream outToVendor = new ObjectOutputStream(vendorSocket.getOutputStream());
		ObjectInputStream inFromVendor = new ObjectInputStream(new BufferedInputStream(vendorSocket.getInputStream()));
		outToVendor.writeObject(myKeys.getN().toString());
		outToVendor.flush();
		outToVendor.writeObject(myKeys.getE().toString());
		outToVendor.flush();
		String vendorIdentity = (String) inFromVendor.readObject();
		System.out.println(vendorIdentity);
		String n=(String) inFromVendor.readObject();
		String e=(String) inFromVendor.readObject();
		RSA vendorKeys = new RSA(new BigInteger(n), new BigInteger(e));

		Commit commitU = new Commit(vendorIdentity, CU, sigCU, chain.get(0), new Date(), "");
		outToVendor.writeObject(commitU);
		outToVendor.flush();
		outToVendor.writeObject(vendorKeys.encrypt(SHA.sha1(commitU.concatAll())));
		System.out.println(vendorKeys.encrypt(SHA.sha1(commitU.concatAll())));
		outToVendor.flush();

	}

	public static void main(String argv[]) throws Exception {
		Client test = new Client();
		test.talkWithBroker();
		test.generateChain(Constants.chainSize);
		test.talkWithVendor();

	}
}