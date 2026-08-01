import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class TestDecrypt {
    public static void main(String[] args) throws Exception {
        String encrypted = "ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyVKgl13rws6qHcUYdvKIXq5NxqsjCTVtbGtKlT4iY2hp+X6Hy8U65URw7tS9a8Gtq";
        String key = "38346591";
        
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "DES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encrypted));
        String url = new String(decryptedBytes);
        
        // Replace _96.mp4 with _320.mp4 for high quality
        url = url.replace("_96.mp4", "_320.mp4").replace(".mp4", ".m4a");
        System.out.println("Decrypted URL: " + url);
    }
}
