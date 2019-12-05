import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
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
		String publicKeyStr = keyToString(publicKey);
		System.out.println("Public key = " + publicKeyStr + "\n");
		String privateKeyStr = keyToString(privateKey);
		System.out.println("Private key = " + privateKeyStr);
	}
	
	// Converts a key to a string
	public static String keyToString(Key k) {
		return Base64.getEncoder().encodeToString(k.getEncoded());
	}
}
