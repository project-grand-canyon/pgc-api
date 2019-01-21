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
import java.security.SecureRandom;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class AuthenticationService {

  private int refreshIntervalMinutes;
  private int jwtLifetime;
  private static AuthenticationService instance;

  // note that a new HMAC secret is generated each time the service is
  // restarted, which means that all existing tokens are invalidated
  // on restart.
  private JWSSigner signer;
  private JWSVerifier verifier;

  private static final String WWW_AUTHENTICATE_CHALLENGE =
      "Basic realm=\"GrandCanyon\"";


  public static void init(int lifetime, int refreshInterval) {

    assert(instance == null);
    instance = new AuthenticationService(lifetime, refreshInterval);
  }

  public static AuthenticationService getInstance() {
    return instance;
  }

  private AuthenticationService(int lifetime, int refreshInterval) {
    this.jwtLifetime = lifetime;
    this.refreshIntervalMinutes = refreshInterval;


    byte[] secret = new byte[32];
    new SecureRandom().nextBytes(secret);
    try {
      this.signer = new MACSigner(secret);
      this.verifier = new MACVerifier(secret);
    }
    catch (JOSEException e) {
      // todo: log error or shut down
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
      JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().subject(admin.getUserName());
      builder.issuer(uriInfo.getAbsolutePath().toString());
      LocalDateTime currentTime = LocalDateTime.now();
      builder.issueTime(toDate(currentTime));
      builder.expirationTime(toDate(currentTime.plusMinutes(jwtLifetime)));

      SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());

      signedJWT.sign(signer);
      TokenResponse tokenResponse = new TokenResponse();
      tokenResponse.setAccessToken(signedJWT.serialize());
      tokenResponse.setExpiresIn(jwtLifetime*60);
      return tokenResponse;
    }
    catch (JOSEException e) {
      throw new ServerErrorException("Failure generating JWT access token: " + e.getMessage(),
          Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

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

    try {
      if (!signedJWT.verify(verifier)) {
        throw new ForbiddenException("Invalid access token signature");
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
    if (claimsSet.getExpirationTime().before(new Date())) {
      throw new ForbiddenException("Invalid token: expired");
    }
    return admin;
  }


  public void tearDown() {

  }

  private Date toDate(LocalDateTime dateTime) {
    return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
  }
}
