package com.ccl.grandcanyon.auth;

import com.ccl.grandcanyon.GCAuth;
import com.ccl.grandcanyon.types.Admin;
import org.apache.http.HttpHeaders;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.util.*;

import static com.ccl.grandcanyon.GCAuth.BEARER_PREFIX;
import static com.ccl.grandcanyon.GCAuth.BASIC_PREFIX;

public class AuthFilter implements ContainerRequestFilter {

  // default role required is basic Admin
  private final static List<String> defaultRoles =
      Collections.singletonList(GCAuth.ADMIN_ROLE);

  private static final String WWW_AUTHENTICATE_CHALLENGE =
      "Basic realm=\"GrandCanyon\"";

  @Context
  private ResourceInfo resourceInfo;

  @Override
  public void filter(ContainerRequestContext context) throws IOException {

    RolesAllowed rolesAllowed = resourceInfo.getResourceMethod().getAnnotation(
        RolesAllowed.class);
    List<String> roles = (rolesAllowed == null) ? defaultRoles :
        Arrays.asList(rolesAllowed.value());

    String authHeader = context.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authHeader != null) {
      Admin admin;
      if (authHeader.toLowerCase().startsWith(BASIC_PREFIX)) {

        // client using Basic Auth
        String credentials = authHeader.substring(BASIC_PREFIX.length()).trim();
        byte[] decodedCredentials = Base64.getDecoder().decode(credentials);
        String[] parts = new String(decodedCredentials).split(":", 2);

        String userName = parts[0];
        String password = parts[1];
        admin = AuthenticationService.getInstance().authenticate(userName, password);
      }
      else if (authHeader.toLowerCase().startsWith(BEARER_PREFIX)) {

        // client using JWT token issued from login method
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        admin = AuthenticationService.getInstance().validateToken(token);
      }
      else {
        // unsupported auth header type
        throw new NotAuthorizedException(
            "Unsupported Authorization Header type: '" + authHeader + "'",
            WWW_AUTHENTICATE_CHALLENGE);
      }

      if (!admin.isLoginEnabled()) {
        throw new ForbiddenException("Token bearer's account has been disabled.");
      }

      if (!admin.isRoot() && roles.contains(GCAuth.SUPER_ADMIN_ROLE)) {
        throw new ForbiddenException("Operation requires super-admin privilege.");
      }
      context.setProperty(GCAuth.CURRENT_PRINCIPAL, admin);
    }

    else {
      // no auth header, which is OK only if the method allows anonymous access
      if (!roles.contains(GCAuth.ANONYMOUS)) {
        throw new NotAuthorizedException("Authentication Required",
            WWW_AUTHENTICATE_CHALLENGE);
      }
    }
  }
}
