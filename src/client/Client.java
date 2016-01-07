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
import java.util.Scanner;

import tools.Certificate;
import tools.Commit;
import tools.Constants;
import tools.RSA;
import tools.SHA;

class Client {

	private static boolean exit = false;
	private static int stage = 0;
	private static int lastPayInChain = 1;

	private String clientIdentity = "Telnet";
	private static RSA myKeys = new RSA(Constants.RSAsize);
	private static Certificate CU;
	private static String sigCU;
	private static ArrayList<String> chain = new ArrayList<String>();
	private static Socket vendor;
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

		RSA brokerKeys = new RSA(new BigInteger(CU.getBrokerRSAKeyN()), new BigInteger(CU.getBrokerRSAKeyE()));

		String sh = brokerKeys.deSign(sigCU);
		System.out.println("Compare: " + sh + " : " + SHA.sha1(CU.concatAll()));
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
		String n = (String) inFromVendor.readObject();
		String e = (String) inFromVendor.readObject();
		RSA vendorKeys = new RSA(new BigInteger(n), new BigInteger(e));

		Commit commitU = new Commit(vendorIdentity, CU, sigCU, chain.get(0), new Date(), "");

		outToVendor.writeObject(commitU);
		outToVendor.flush();
		outToVendor.writeObject(vendorKeys.encrypt(SHA.sha1(commitU.concatAll())));
		System.out.println(vendorKeys.encrypt(SHA.sha1(commitU.concatAll())));
		outToVendor.flush();

	}

	static void printMenu() {
		if (stage == 1) {
			System.out.println("1.Buy random joke\n" + "0.Exit\n\n");
		} else if (stage == 0) {
			System.out.println("1.Connect to Vendor\n" + "0.Exit\n\n");
		}
	}

	static void runCommand(String command, Socket vendorSocket, ObjectOutputStream outToVendor,
			ObjectInputStream inFromVendor) throws Exception {

		if (command.equals("0")) {
			exit = true;
		}

		if (command.equals("1")) {
			if (stage == 1)
				fetchJoke(vendorSocket, outToVendor, inFromVendor);
			else if (stage == 0) {
				int ret = commitToVendor(vendorSocket, outToVendor, inFromVendor);
				if (ret == -1)
					System.out.println("Couldn't connect to vendor, please try again.\n");
				else if (ret == 0)
					stage = 1;
				else if (ret == -2) {
					System.out.println("Certificate problem.");
				}
			}
		}
	}

	static int commitToVendor(Socket vendorSocket, ObjectOutputStream outToVendor, ObjectInputStream inFromVendor)
			throws Exception {

		if (vendorSocket.isConnected()) {

			outToVendor.writeObject(myKeys.getN().toString());
			outToVendor.flush();
			outToVendor.writeObject(myKeys.getE().toString());
			outToVendor.flush();
			String vendorIdentity = (String) inFromVendor.readObject();
			System.out.println("Vendor ID: " + vendorIdentity + "");
			String n = (String) inFromVendor.readObject();
			String e = (String) inFromVendor.readObject();

			Commit commitU = new Commit(vendorIdentity, CU, sigCU, chain.get(0), new Date(), "");

			outToVendor.writeObject(commitU);
			outToVendor.flush();
			outToVendor.writeObject(myKeys.sign(SHA.sha1(commitU.concatAll())));
			outToVendor.flush();
			System.out.println("1");

			String response = inFromVendor.readUTF();

			System.out.println("2");
			System.out.println("Vendor response: " + response);

			if (response.trim().equals("certificate_fail")) {
				return -2;
			}

			vendor = vendorSocket;

			return 0;
		}

		return -1;
	}

	static void fetchJoke(Socket vendorSocket, ObjectOutputStream outToVendor, ObjectInputStream inFromVendor)
			throws Exception {
		if (lastPayInChain >= chain.size()) {
			System.out.println("Chain of payment limit reached.");
		} else if (vendorSocket.isConnected()) {

			outToVendor.writeUTF("buy_joke");
			outToVendor.flush();

			ArrayList<String> Ci = new ArrayList<>();

			Ci.add(chain.get(lastPayInChain));
			Integer tmp = lastPayInChain;
			Ci.add(tmp.toString());

			outToVendor.writeObject(Ci);
			outToVendor.flush();

			String response = inFromVendor.readUTF();
			
			System.out.println("Response (pay): "+response);

			if (response.trim().equals("payment_success")) {
				lastPayInChain++;
				System.out.println(inFromVendor.readUTF());
			} else
				System.out.println("Payment failed.");
		}
	}

	public static void main(String argv[]) throws Exception {
		Scanner read = new Scanner(System.in);
		String command;

		Client test = new Client();

		test.talkWithBroker();
		test.generateChain(Constants.chainSize);

		Socket vendorSocket = new Socket(Constants.vendorIp, Constants.vendorPort);
		ObjectOutputStream outToVendor = new ObjectOutputStream(vendorSocket.getOutputStream());
		ObjectInputStream inFromVendor = new ObjectInputStream(new BufferedInputStream(vendorSocket.getInputStream()));

		while (!exit) {
			printMenu();
			command = read.next();
			runCommand(command, vendorSocket, outToVendor, inFromVendor);
		}

	}
}