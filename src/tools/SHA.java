package tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA {
	
	static public String sha1(String input) //throws NoSuchAlgorithmException 
	{
		MessageDigest mDigest;
		StringBuffer sb = new StringBuffer();
		try {
			mDigest = MessageDigest.getInstance("SHA1");
			byte[] result = mDigest.digest(input.getBytes());			
			for (int i = 0; i < result.length; i++) {
				sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
			}	
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sb.toString();
	}
}