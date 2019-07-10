package key_generator;

import static org.junit.jupiter.api.Assertions.*;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.junit.jupiter.api.Test;

class Tests {

	@Test
	void keyToStrConversion() throws NoSuchAlgorithmException, InvalidKeySpecException {
		// Generate an RSA key pair
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.generateKeyPair();
		PublicKey publicKey = kp.getPublic();
		PrivateKey privateKey = kp.getPrivate();
		
		// Convert the keys to strings
		String publicKeyStr = Generator.keyToString(publicKey);
		String privateKeyStr = Generator.keyToString(privateKey);
		
		// Check that the conversion occurred correctly
		assertEquals(publicKey, Generator.stringToPublicKey(publicKeyStr));
		assertEquals(privateKey, Generator.stringToPrivateKey(privateKeyStr));
	}
	
	@Test
	// Encrypt with public key and decrypt with private key
	void encryptPubKey() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		// Generate an RSA key pair
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.generateKeyPair();
		PublicKey publicKey = kp.getPublic();
		PrivateKey privateKey = kp.getPrivate();
		
		// Create a message
		byte[] message = new byte[20];
		new Random().nextBytes(message);
		
		// Encrypt a message with the public key
		byte[] encryptedText = Generator.encrypt(publicKey, message);
		
		// Decrypt it with the private key
		byte[] unencryptedText = Generator.decrypt(privateKey, encryptedText);
		
		assertEquals(Arrays.equals(message, unencryptedText), true);
		assertEquals(Arrays.equals(message, encryptedText), false);
		assertEquals(Arrays.equals(encryptedText, unencryptedText), false);
	}
	
	@Test
	// Sign with private key, and verify with public key
	void encryptPrivKey() throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {
		Generator g = new Generator();
		
		// Generate an RSA key pair
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.generateKeyPair();
		PublicKey publicKey = kp.getPublic();
		PrivateKey privateKey = kp.getPrivate();
		
		// Create a message
		byte[] message = new byte[20];
		new Random().nextBytes(message);
		
		// Encrypt a message with the private key
		byte[] retSig = Generator.sign(privateKey, message);
		
		// Verify the signature		
		assertEquals(Generator.verify(publicKey, message, retSig), true);
		
	}

}
