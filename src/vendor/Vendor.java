package vendor;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
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

public class Vendor {

	static private RSA brokerKeys, myKeys = new RSA(Constants.RSAsize);
	static private String brokerIdentity;
	static private String vendorIdentity = "VendorulCelSmecher";

	static Date expDate;
	Commit commitClient;
	static private boolean certified = false;
	static String lastPayment;
	static int lastPaymentSize=0;


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
				
				System.out.println("Command: "+command);

				if (command.trim().equals("buy_joke")) {
					ArrayList<String> lastPay = (ArrayList<String>) inFromClient.readObject();

					System.out.println("C0: "+commitClient.getChainRoot());
					
					String Ci = lastPay.get(0);

					if (expDate.compareTo(Calendar.getInstance().getTime()) >= 0 && lastPaymentSize<Integer.parseInt(lastPay.get(1))) {
						for (int i = 0; i < Integer.parseInt(lastPay.get(1)); i++) {
							Ci = SHA.sha1(Ci);
						}
						
						System.out.println("Chain: "+Ci);
						
						if (Ci.equals(commitClient.getChainRoot())) {
							lastPayment = lastPay.get(0);
							lastPaymentSize++;
							System.out.println("Pay success.");
							outToClient.writeUTF("payment_success");
							outToClient.flush();
							
							outToClient.writeUTF("Random joke " + Math.random() + ".");
							outToClient.flush();
						}else{
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
		Vendor v = new Vendor();
		v.requestBroker(Constants.brokerIp, Constants.brokerPort);

		ServerSocket welcomeSocket = new ServerSocket(Constants.vendorPort);
		while (true) {
			Socket connectionSocket = welcomeSocket.accept();
			ObjectOutputStream outToClient = new ObjectOutputStream(connectionSocket.getOutputStream());
			ObjectInputStream inFromClient = new ObjectInputStream(
					new BufferedInputStream(connectionSocket.getInputStream()));
			v.commitClient(connectionSocket, outToClient, inFromClient);
			v.listenToClient(connectionSocket, outToClient, inFromClient);

		}

	}

}
