package com.ccl.grandcanyon.auth;

import com.ccl.grandcanyon.GCAuth;
import com.ccl.grandcanyon.types.Admin;
import org.apache.http.HttpHeaders;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

public class AuthResponseFilter implements ContainerResponseFilter {

  @Context
  UriInfo uriInfo;

  @Override
  public void filter(
      ContainerRequestContext requestContext,
      ContainerResponseContext responseContext) throws IOException {

    // if the access token presented with the request is valid and its expiration
    // is within the refresh interval, generate a new token and return it as the
    // Authorization header of the HTTP response.

    if (!responseContext.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
      return;
    }

    String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    System.out.println("auth response filter auth HEader");
    System.out.println(authHeader);
    if (authHeader != null && authHeader.toLowerCase().startsWith(GCAuth.BEARER_PREFIX)) {

      Admin admin = (Admin)requestContext.getProperty(GCAuth.CURRENT_PRINCIPAL);
      if (admin != null) {
        String token = authHeader.substring(GCAuth.BEARER_PREFIX.length()).trim();
        AuthenticationService authService = AuthenticationService.getInstance();

        if (authService.shouldRefresh(token)) {
          TokenResponse tokenResponse = authService.generateToken(admin, uriInfo);
          responseContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION,
              "Bearer " + tokenResponse.getAccessToken());
        }
      }
    }
  }
}
