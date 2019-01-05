package com.ccl.grandcanyon;


// acknowledgement: most of this borrowed from com/baeldung/passwordhashing/PBKDF2Hasher.java (github)

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;


public class PasswordUtil {

  private static final int ITERATIONS = 1 << 16;
  private static final int SIZE = 128;
  private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
  private static final SecureRandom random = new SecureRandom();


  private PasswordUtil() { }

  public static String hash(char[] password) {
    byte[] salt = new byte[SIZE/8];
    random.nextBytes(salt);
    byte[] dk = pbkdf2(password, salt);
    byte[] hash = new byte[salt.length + dk.length];
    System.arraycopy(salt, 0, hash, 0, salt.length);
    System.arraycopy(dk, 0, hash, salt.length, dk.length);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
  }


  public static boolean check(char[] password, String token) {
    byte[] hash = Base64.getUrlDecoder().decode(token);
    byte[] salt = Arrays.copyOfRange(hash, 0, SIZE/8);
    byte[] check = pbkdf2(password, salt);
    int zero = 0;
    for (int idx = 0; idx < check.length; idx++) {
      zero |= hash[salt.length + idx] ^ check[idx];
    }
    return zero == 0;
  }


  private static byte[] pbkdf2(char[] password, byte[] salt)
  {
    KeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, SIZE);
    try {
      SecretKeyFactory f = SecretKeyFactory.getInstance(ALGORITHM);
      return f.generateSecret(spec).getEncoded();
    }
    catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("Missing algorithm: " + ALGORITHM, ex);
    }
    catch (InvalidKeySpecException ex) {
      throw new IllegalStateException("Invalid SecretKeyFactory", ex);
    }
  }
}
