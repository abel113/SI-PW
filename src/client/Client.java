package client;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import rsa.RSA;
import tools.Certificate;
import tools.Constants;
import tools.SHA;

class Client {
	private String clientIdentity = "Telnet";
	private RSA myKeys = new RSA(Constants.RSAsize);
	private Certificate CU;
	private ArrayList<String> chain = new ArrayList<String>();
	// private RSA brokerKeys;

	void talkWithBroker() throws Exception {
		Socket clientSocket = new Socket(Constants.brokerIp, Constants.brokerPort);
		DataOutputStream outToBroker = new DataOutputStream(clientSocket.getOutputStream());
		ObjectInputStream inFromBroker = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));
		outToBroker.writeUTF("new_customer");
		outToBroker.flush();
		outToBroker.writeUTF(clientIdentity);
		outToBroker.flush();
		outToBroker.writeUTF(myKeys.getN().toString());
		outToBroker.flush();
		outToBroker.writeUTF(myKeys.getE().toString());
		outToBroker.flush();

		
		CU = (Certificate) inFromBroker.readObject();

		String sh = (String) inFromBroker.readObject();
		sh = myKeys.decrypt(sh);
		
		
		clientSocket.close();
		
		clientSocket = new Socket(Constants.brokerIp, Constants.brokerPort);
		outToBroker = new DataOutputStream(clientSocket.getOutputStream());
		inFromBroker = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));
		
		outToBroker.writeUTF("pk");
		System.out.println("Broker\' public key: "+inFromBroker.readObject().toString());
		
		
		clientSocket.close();
		

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

	public static void main(String argv[]) throws Exception {
		Client test = new Client();
		test.talkWithBroker();
		test.generateChain(Constants.chainSize);

	}
}