package com.ccl.grandcanyon;

import com.ccl.grandcanyon.auth.AuthenticationService;
import com.ccl.grandcanyon.auth.LoginRequest;
import com.ccl.grandcanyon.auth.TokenResponse;
import com.ccl.grandcanyon.types.Admin;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/login")
public class Login {

  @Context
  UriInfo uriInfo;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.ANONYMOUS)
  public Response login(LoginRequest request) {

    if (request.getUserName() == null || request.getPassword() == null) {
      throw new BadRequestException("Missing required form parameter username or password");
    }

    AuthenticationService authService = AuthenticationService.getInstance();
    Admin admin = authService.authenticate(request.getUserName(), request.getPassword());
    TokenResponse tokenResponse = authService.generateToken(admin, uriInfo);

    return Response.ok(tokenResponse).build();
  }

}
