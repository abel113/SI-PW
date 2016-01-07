package tools;

import java.io.Serializable;
import java.util.Date;

public class Commit implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String vendorIdentity;
	Certificate CU;
	String sigCU;
	String chainRoot;
	Date currentDate;
	String info;

	public Commit(String vI, Certificate c, String sCU, String cR, Date cD, String inf) {
		vendorIdentity = vI;
		CU = c;
		sigCU = sCU;
		chainRoot = cR;
		currentDate = cD;
		info = inf;
	}

	public String getSigCU() {
		return sigCU;
	}

	public void setSigCU(String sigCU) {
		this.sigCU = sigCU;
	}

	public String concatAll() {
		return vendorIdentity + CU.concatAll() + chainRoot + currentDate.toString() + info;
	}

	public String getVendorIdentity() {
		return vendorIdentity;
	}

	public void setVendorIdentity(String vendorIdentity) {
		this.vendorIdentity = vendorIdentity;
	}

	public Certificate getCU() {
		return CU;
	}

	public void setCU(Certificate cU) {
		CU = cU;
	}

	public String getChainRoot() {
		return chainRoot;
	}

	public void setChainRoot(String chainRoot) {
		this.chainRoot = chainRoot;
	}

	public Date getCurrentDate() {
		return currentDate;
	}

	public void setCurrentDate(Date currentDate) {
		this.currentDate = currentDate;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

}
