package tools;

import java.io.Serializable;
import java.util.Date;

public class Certificate implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String clientIdentity;
	String brokerIdentity;
	String clientRSAKeyN;
	String clientRSAKeyE;
	String brokerRSAKeyN;
	String brokerRSAKeyE;
	Date dataExp;
	String extraInfo;

	public Certificate(String cI, String bI, String cKN, String cKE, String bKN, String bKE, Date dE, String eI) {
		clientIdentity = cI;
		brokerIdentity = bI;
		clientRSAKeyN = cKN;
		clientRSAKeyE = cKE;
		brokerRSAKeyN = bKN;
		brokerRSAKeyE = bKE;
		dataExp = dE;
		extraInfo = eI;
	}

	public String concatAll() {
		return clientIdentity + brokerIdentity + clientRSAKeyN + clientRSAKeyE + brokerRSAKeyN + brokerRSAKeyE
				+ dataExp.toString() + extraInfo;
	}

	public String toString() {
		return clientIdentity + "\n" + brokerIdentity + "\n" + clientRSAKeyN + "\n" + clientRSAKeyE + "\n"
				+ brokerRSAKeyN + "\n" + brokerRSAKeyE + "\n" + dataExp.toString() + "\n" + extraInfo;
	}

	public String getClientIdentity() {
		return clientIdentity;
	}

	public void setClientIdentity(String clientIdentity) {
		this.clientIdentity = clientIdentity;
	}

	public String getBrokerIdentity() {
		return brokerIdentity;
	}

	public void setBrokerIdentity(String brokerIdentity) {
		this.brokerIdentity = brokerIdentity;
	}

	public String getClientRSAKeyN() {
		return clientRSAKeyN;
	}

	public void setClientRSAKeyN(String clientRSAKeyN) {
		this.clientRSAKeyN = clientRSAKeyN;
	}

	public String getClientRSAKeyE() {
		return clientRSAKeyE;
	}

	public void setClientRSAKeyE(String clientRSAKeyE) {
		this.clientRSAKeyE = clientRSAKeyE;
	}

	public String getBrokerRSAKeyN() {
		return brokerRSAKeyN;
	}

	public void setBrokerRSAKeyN(String brokerRSAKeyN) {
		this.brokerRSAKeyN = brokerRSAKeyN;
	}

	public String getBrokerRSAKeyE() {
		return brokerRSAKeyE;
	}

	public void setBrokerRSAKeyE(String brokerRSAKeyE) {
		this.brokerRSAKeyE = brokerRSAKeyE;
	}

	public Date getDataExp() {
		return dataExp;
	}

	public void setDataExp(Date dataExp) {
		this.dataExp = dataExp;
	}

	public String getExtraInfo() {
		return extraInfo;
	}

	public void setExtraInfo(String extraInfo) {
		this.extraInfo = extraInfo;
	}

}
