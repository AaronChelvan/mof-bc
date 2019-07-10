package key_generator;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Generator {

	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException {
		// Generate an RSA key pair
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.generateKeyPair();
		Key publicKey = kp.getPublic();
		Key privateKey = kp.getPrivate();

		// Convert the keys to strings
		String publicKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());
		System.out.println("Public key = " + publicKeyStr);
		String privateKeyStr = Base64.getEncoder().encodeToString(privateKey.getEncoded());
		System.out.println("Private key = " + privateKeyStr);
		
		// Convert the strings back to keys
        if (publicKey.equals(stringToPublicKey(publicKeyStr))) {
        	System.out.println("Public keys are equal!");
        }
        
        if (privateKey.equals(stringToPrivateKey(privateKeyStr))) {
        	System.out.println("Private keys are equal!");
        } 
        
	}
	
	// Accepts a key that has been encoded as a string and returns a public key
	public static Key stringToPublicKey(String keyStr) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] keyBytes = Base64.getDecoder().decode(keyStr);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(keySpec);
	}
	
	// Accepts a key that has been encoded as a string and returns a public key
	public static Key stringToPrivateKey(String keyStr) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] keyBytes = Base64.getDecoder().decode(keyStr);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
	}
	
	// Converts a key to a string
	public static String keyToString(Key k) {
		return Base64.getEncoder().encodeToString(k.getEncoded());
	}

}
