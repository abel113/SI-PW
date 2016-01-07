package vendor;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import tools.Commit;
import tools.Constants;
import tools.RSA;
import tools.SHA;

public class Vendor implements Runnable {

	private RSA brokerKeys, myKeys = new RSA(Constants.RSAsize);
	private String brokerIdentity;
	private String vendorIdentity = "VendorulCelSmecher";
	public Socket connectionSocket;

	Date expDate;
	Commit commitClient;
	private boolean certified = false;
	String lastPayment;
	int lastPaymentSize = 0;

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

	void commitClient(Socket clientSocket, ObjectOutputStream outToClient, ObjectInputStream inFromClient)
			throws Exception {

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

		System.out.println("Sig: " + sigCommitU);
		System.out.println("verifySign: " + clientKeys.deSign(sigCommitU));
		System.out.println("equals?: " + SHA.sha1(commitU.concatAll()));

		System.out.println("Broker equals?: " + brokerKeys.deSign(commitU.getSigCU()) + " : "
				+ SHA.sha1(commitU.getCU().concatAll()));

		if (brokerKeys.deSign(commitU.getSigCU()).equals(SHA.sha1(commitU.getCU().concatAll()))
				&& clientKeys.deSign(sigCommitU).equals(SHA.sha1(commitU.concatAll()))
				&& commitU.getCU().getDataExp().compareTo(Calendar.getInstance().getTime()) >= 0) {
			outToClient.writeUTF("certificate_ok");
			outToClient.flush();
			certified = true;
		} else
			outToClient.writeUTF("certificare_fail");

		outToClient.flush();

		expDate = commitU.getCU().getDataExp();

		commitClient = commitU;

		// outToClient.close();
		// inFromClient.close();
	}

	void listenToClient(Socket clientSocket, ObjectOutputStream outToClient, ObjectInputStream inFromClient)
			throws Exception {
		if (clientSocket.isConnected()) {

			while (clientSocket.isConnected()) {
				String command = inFromClient.readUTF();

				System.out.println("Command: " + command);

				if (command.trim().equals("buy_joke")) {
					ArrayList<String> lastPay = (ArrayList<String>) inFromClient.readObject();

					System.out.println("C0: " + commitClient.getChainRoot());

					String Ci = lastPay.get(0);

					if (expDate.compareTo(Calendar.getInstance().getTime()) >= 0
							&& lastPaymentSize < Integer.parseInt(lastPay.get(1))) {
						for (int i = 0; i < Integer.parseInt(lastPay.get(1)); i++) {
							Ci = SHA.sha1(Ci);
						}

						System.out.println("Chain: " + Ci);

						if (Ci.equals(commitClient.getChainRoot())) {
							lastPayment = lastPay.get(0);
							lastPaymentSize++;
							System.out.println("Pay success.");
							outToClient.writeUTF("payment_success");
							outToClient.flush();

							outToClient.writeUTF("Random joke " + Math.random() + ".");
							outToClient.flush();
						} else {
							System.out.println("Pay fail.");
							outToClient.writeUTF("payment_fail");
							outToClient.flush();
						}
					} else {
						System.out.println("Pay fail.");
						outToClient.writeUTF("payment_fail");
						outToClient.flush();
					}

				}
			}
		}

	}

	public static void main(String argv[]) throws Exception {
		ArrayList<Vendor> v = new ArrayList<>();
		ArrayList<Thread> t = new ArrayList<>();

		ServerSocket welcomeSocket = new ServerSocket(Constants.vendorPort);

		while (true) {

			v.add(new Vendor());
			v.get(v.size() - 1).requestBroker(Constants.brokerIp, Constants.brokerPort);

			Socket connectionSocket = welcomeSocket.accept();

			v.get(v.size() - 1).connectionSocket = connectionSocket;

			t.add(new Thread(v.get(v.size() - 1)));

			t.get(t.size() - 1).start();

		}

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		ObjectOutputStream outToClient;
		try {
			outToClient = new ObjectOutputStream(connectionSocket.getOutputStream());
			ObjectInputStream inFromClient = new ObjectInputStream(
					new BufferedInputStream(connectionSocket.getInputStream()));
			commitClient(connectionSocket, outToClient, inFromClient);
			listenToClient(connectionSocket, outToClient, inFromClient);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
