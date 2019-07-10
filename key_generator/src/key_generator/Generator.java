package key_generator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Generator {

	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException {
		// Generate an RSA key pair
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.generateKeyPair();
		Key publicKey = kp.getPublic();
		Key privateKey = kp.getPrivate();

		// Convert the keys to strings
		String publicKeyStr = keyToString(publicKey);
		System.out.println("Public key = " + publicKeyStr);
		String privateKeyStr = keyToString(privateKey);
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
	
	// Given a public key and a message, encrypt the message
	public static byte[] encrypt(PublicKey k, byte[] message) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, k);
		return cipher.doFinal(message);
	}
	
	// Given a private key and an encrypted message, decrypt it
	public static byte[] decrypt(PrivateKey k, byte[] encryptedMessage) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, k);
		return cipher.doFinal(encryptedMessage);
	}
	
	// Given a private key and a message, create a signature
	public static byte[] sign(PrivateKey k, byte[] message) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
		Signature rsa = Signature.getInstance("SHA1withRSA"); 
		rsa.initSign(k);
		rsa.update(message);
		return rsa.sign();
	}
	
	// Given a public key, a message, and a signature, verify if the signature is valid
	public static boolean verify(PublicKey k, byte[] message, byte[] signature) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature sig = Signature.getInstance("SHA1withRSA");
		sig.initVerify(k);
		sig.update(message);
		return sig.verify(signature);
	}

}
