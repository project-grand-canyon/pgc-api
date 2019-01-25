package com.ccl.grandcanyon.auth;

import com.ccl.grandcanyon.Admins;
import com.ccl.grandcanyon.types.Admin;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.*;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Singleton service responsible for administrator authentication.
 */
public class AuthenticationService {

  // the lifetime of an issued JWT access token
  private static int jwtLifetimeMinutes;

  // if an access token is presented within this interval before
  // the token's expiration, a new token is issued and returned with the
  // response.
  private static int refreshIntervalMinutes;

  private static AuthenticationService instance;

  private JWSSigner signer;
  private JWSVerifier verifier;
  private JWSVerifier previousVerifier;
  private SecretKeys keys;

  private static final Logger logger = Logger.getLogger(AuthenticationService.class.getName());

  private static final String WWW_AUTHENTICATE_CHALLENGE =
      "Basic realm=\"GrandCanyon\"";

  private static final String SAVED_STATE_FILE = "saved_state";


  public static void init(
      int lifetime,
      int refreshInterval) throws JOSEException {

    assert(instance == null);
    jwtLifetimeMinutes = lifetime;
    refreshIntervalMinutes = refreshInterval;
    instance = new AuthenticationService();
  }


  /**
   * return the singleton Authentication Service instance.
   */
  public static AuthenticationService getInstance() {
    return instance;
  }


  private AuthenticationService() throws JOSEException {

    // if secret keys were saved at last shutdown of the web service, retrieve
    // those keys use them if they are still valid. This allows existing access
    // tokens to still be accepted after a restart of the web service.
    File savedStateFile = new File(SAVED_STATE_FILE);
    if (savedStateFile.exists()) {
      try {
        ObjectInputStream savedStateIn = new ObjectInputStream(
            new FileInputStream(savedStateFile));
        keys = (SecretKeys)savedStateIn.readObject();
        savedStateIn.close();
        savedStateFile.delete();
      }
      catch (Exception e) {
        logger.warning("Error reading saved state, generating new JWT keys. " +
            "Previously issued access tokens will not be accepted. " +
            "Exception message: " + e.getMessage() );
      }
    }

    if (keys == null) {
      keys = new SecretKeys();
    }
    updateKeys();
    if (this.signer == null) {
      installKeys();
    }
  }

  /**
   * Authenticate an admin user.
   * @param userName user name
   * @param password password
   * @return the authenticated Admin user object, never null
   * @throws NotAuthorizedException if authentication fails
   * @throws ServerErrorException on database error.
   */
  public Admin authenticate(
      String userName,
      String password)  {

    Admin admin;
    try {
      admin = Admins.getAdminByName(userName);
    }
    catch (SQLException e) {
      throw new ServerErrorException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }

    if (admin != null && !(admin.isLoginEnabled())) {
      throw new NotAuthorizedException("Account not enabled for login",
          WWW_AUTHENTICATE_CHALLENGE);
    }

    if (admin == null ||
        !PasswordUtil.check(password.toCharArray(), admin.getToken())) {
      throw new NotAuthorizedException("Invalid Credentials",
          WWW_AUTHENTICATE_CHALLENGE);
    }
    return admin;
  }


  /**
   * Generate a JWT token for the authenticated administrator.
   * @param admin Admin user
   * @param uriInfo URI info used to set token issuer value
   * @return serialized token value
   */
  public TokenResponse generateToken(Admin admin, UriInfo uriInfo) {

    try {
      updateKeys();

      JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().subject(admin.getUserName());
      builder.issuer(uriInfo.getAbsolutePath().getHost());
      LocalDateTime currentTime = LocalDateTime.now();
      builder.issueTime(toDate(currentTime));
      builder.expirationTime(toDate(currentTime.plusMinutes(jwtLifetimeMinutes)));

      SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());

      signedJWT.sign(signer);
      TokenResponse tokenResponse = new TokenResponse();
      tokenResponse.setAccessToken(signedJWT.serialize());
      tokenResponse.setExpiresIn(jwtLifetimeMinutes *60);
      return tokenResponse;
    }
    catch (JOSEException e) {
      throw new ServerErrorException("Failure generating JWT access token: " + e.getMessage(),
          Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Validate a JWT access token.
   * @param token the token value
   * @return the Admin user represented by the token
   * @throws ForbiddenException if the token is not valid
   * @throws ServerErrorException if an internal error occurs while inspecting
   * the token.
   */
  public Admin validateToken(String token) {

    SignedJWT signedJWT;
    JWTClaimsSet claimsSet;
    try {
      signedJWT = SignedJWT.parse(token);
      claimsSet = signedJWT.getJWTClaimsSet();
    }
    catch (ParseException e) {
      throw new ForbiddenException("Invalid access token", e);
    }

    if (claimsSet.getExpirationTime().before(new Date())) {
      throw new ForbiddenException("Invalid token: expired");
    }

    try {
      if (!signedJWT.verify(verifier)) {
        if (previousVerifier == null || !signedJWT.verify(previousVerifier)) {
          throw new ForbiddenException("Invalid access token signature");
        }
      }
    }
    catch (JOSEException e) {
      throw new ServerErrorException("Internal error verifying token",
          Response.Status.INTERNAL_SERVER_ERROR, e);
    }
    String subject = claimsSet.getSubject();
    Admin admin;
    try {
      admin = Admins.getAdminByName(subject);
    }
    catch (SQLException e) {
      throw new ServerErrorException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (admin == null) {
      throw new ForbiddenException("Invalid token: unknown subject");
    }

    return admin;
  }


  /**
   * Return true if the specified token should be refreshed.
   * @param token JWT access token
   * @return true if the token expiration is within the JWT refresh interval
   */
  public boolean shouldRefresh(String token) {
    SignedJWT signedJWT;
    JWTClaimsSet claimsSet;
    try {
      signedJWT = SignedJWT.parse(token);
      claimsSet = signedJWT.getJWTClaimsSet();

      LocalDateTime expireTime = toLocalDateTime(claimsSet.getExpirationTime());
      LocalDateTime now = LocalDateTime.now();
      return expireTime.minusMinutes(refreshIntervalMinutes).isBefore(now) &&
          expireTime.isAfter(now);
    }
    catch (ParseException e) {
      logger.warning("JWT Refresh internval not honored due to error: " + e.getMessage());
      return false;
    }
  }


  public void tearDown() {

    try {
      if (keys != null) {
        // save the in-use secret keys to a file so that they may continue to be used
        // at next service restart.
        ObjectOutputStream savedStateOut = new ObjectOutputStream(
            new FileOutputStream(SAVED_STATE_FILE));
        savedStateOut.writeObject(keys);
        savedStateOut.close();
      }
    }
    catch (IOException e) {
      logger.warning("Failed to save state: " + e.getMessage());
    }
  }


  // Rotate keys periodically.  If jwtLifetimeMinutes is "N", then:
  //
  // A key is used for generating tokens for N minutes.

  // A key can continue to be used for verification for N mintues after the
  // key is no longer used for generating tokens.
  private void updateKeys() throws JOSEException {

    if (keys.getCurrent() == null ||
        keys.getCurrent().getLastTokenGenTime().isBefore(LocalDateTime.now())) {
      HashKey previousKey = keys.getCurrent();
      byte[] newKey = new byte[32];
      new SecureRandom().nextBytes(newKey);
      keys.setCurrent(new HashKey(newKey));

      if (previousKey != null &&
          previousKey.getLastTokenVerifyTime().isAfter(LocalDateTime.now())) {
        keys.setPrevious(previousKey);
      }

      installKeys();
    }
  }


  private void installKeys() throws JOSEException {

    this.signer = new MACSigner(keys.getCurrent().getValue());
    this.verifier = new MACVerifier(keys.getCurrent().getValue());
    if (keys.getPrevious() != null) {
      this.previousVerifier = new MACVerifier(keys.getPrevious().getValue());
    }

  }


  private Date toDate(LocalDateTime dateTime) {
    return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
  }


  private LocalDateTime toLocalDateTime(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
  }


  /**
   * Object used for saving secret keys to the saved state file.
   */
  private static class SecretKeys implements Serializable {
    private HashKey current;
    private HashKey previous;

    public HashKey getCurrent() {
      return current;
    }

    public void setCurrent(HashKey current) {
      this.current = current;
    }

    public HashKey getPrevious() {
      return previous;
    }

    public void setPrevious(HashKey previous) {
      this.previous = previous;
    }
  }


  private static class HashKey implements Serializable {

    private byte[] value;
    private LocalDateTime lastTokenGenTime;
    private LocalDateTime lastTokenVerifyTime;

    public HashKey(byte[] value) {
      this.value = value;
      this.lastTokenGenTime = LocalDateTime.now().plusMinutes(jwtLifetimeMinutes);
      this.lastTokenVerifyTime = this.lastTokenGenTime.plusMinutes(jwtLifetimeMinutes);
    }

    public byte[] getValue() {
      return value;
    }

    public LocalDateTime getLastTokenGenTime() {
      return lastTokenGenTime;
    }

    public LocalDateTime getLastTokenVerifyTime() {
      return lastTokenVerifyTime;
    }

  }


}
