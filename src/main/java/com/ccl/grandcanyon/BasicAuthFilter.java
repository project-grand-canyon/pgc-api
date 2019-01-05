package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.Admin;
import org.apache.http.HttpHeaders;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class BasicAuthFilter implements ContainerRequestFilter {

  private final static String BASIC_PREFIX = "basic";

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
      if (authHeader.toLowerCase().startsWith(BASIC_PREFIX)) {
        String credentials = authHeader.substring(BASIC_PREFIX.length()).trim();
        byte[] decodedCredentials = Base64.getDecoder().decode(credentials);
        String[] parts = new String(decodedCredentials).split(":", 2);

        String userName = parts[0];
        String password = parts[1];
        Admin admin;

        try {
          admin = Admins.getAdminByName(userName);
        }
        catch (SQLException e) {
          context.abortWith(Response.status(
              Response.Status.INTERNAL_SERVER_ERROR).
              entity(e.getMessage()).build());
          return;
        }

        if (admin == null ||
            !PasswordUtil.check(password.toCharArray(), admin.getToken())) {
          throw new NotAuthorizedException("Invalid Credentials",
              WWW_AUTHENTICATE_CHALLENGE);
        }

        if (!admin.isRoot() && roles.contains(GCAuth.SUPER_ADMIN_ROLE)) {
          throw new ForbiddenException("Operation requires super-admin privilege.");
        }

        context.setProperty(GCAuth.CURRENT_PRINCIPAL, admin);
      }
      else {
        // unsupported auth header type
        throw new NotAuthorizedException(
            "Unsupported Authorization Header type: '" + authHeader + "'",
            WWW_AUTHENTICATE_CHALLENGE);
      }
    }

    else {
      // no auth header
      if (!roles.contains(GCAuth.ANONYMOUS)) {
        throw new NotAuthorizedException("Authentication Required",
            WWW_AUTHENTICATE_CHALLENGE);
      }
    }
  }
}
