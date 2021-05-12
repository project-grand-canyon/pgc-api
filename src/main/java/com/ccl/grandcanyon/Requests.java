package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.Admin;
import com.ccl.grandcanyon.types.Request;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Path("/requests")
public class Requests {

  private static final String SQL_SELECT_REQUEST = "SELECT * FROM requests";

  private static final String SQL_CREATE_REQUEST =
      "INSERT INTO requests (" +
          Request.DISTRICT_ID + ", " +
          Request.CONTENT +
          ") VALUES (?, ?)";

  private static final String SQL_UPDATE_REQUEST =
      "UPDATE requests SET " +
          Request.DISTRICT_ID + " = ?, " +
          Request.CONTENT + " = ? " +
          "WHERE " + Request.REQUEST_ID + " = ?";

  private static final String SQL_DELETE_REQUEST =
      "DELETE from requests " +
          "WHERE " + Request.REQUEST_ID + " = ?";

  @Context
  UriInfo uriInfo;

  @Context
  ContainerRequestContext requestContext;


  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createRequest(Request request) throws SQLException {

    checkPermissions(request, null, "create");
    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement insertStatement = conn.prepareStatement(SQL_CREATE_REQUEST,
          Statement.RETURN_GENERATED_KEYS);
      insertStatement.setInt(1, request.getDistrictId());
      insertStatement.setString(2, request.getContent());
      insertStatement.executeUpdate();

      int requestId;
      ResultSet rs = insertStatement.getGeneratedKeys();
      if (rs.next()) {
        requestId = rs.getInt(1);
        rs.close();
      }
      else {
        throw new SQLException("Create of request failed, no ID obtained.");
      }

      Request newRequest = retrieveById(conn, requestId);
      URI location = uriInfo.getAbsolutePathBuilder().path(Integer.toString(newRequest.getRequestId())).build();
      return Response.created(location).entity(newRequest).build();
    }
    finally {
      conn.close();
    }
  }


  @PUT
  @Path("{requestId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateRequest(
      @PathParam("requestId") int requestId,
      Request request)
    throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      Request previous = retrieveById(conn, requestId);
      checkPermissions(request, previous, "update");
      PreparedStatement statement = conn.prepareStatement(SQL_UPDATE_REQUEST);
      statement.setInt(1, request.getDistrictId());
      statement.setString(2, request.getContent());
      statement.setInt(3, requestId);
      statement.executeUpdate();

      return Response.ok(retrieveById(conn, requestId)).build();
    }
    finally {
      conn.close();
    }
  }


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRequests() throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      List<Request> requests = new ArrayList<>();
      ResultSet rs = conn.createStatement().executeQuery(SQL_SELECT_REQUEST);
      while (rs.next()) {
        requests.add(new Request(rs));
      }
      return Response.ok(requests).build();
    }
    finally {
      conn.close();
    }
  }


  @GET
  @Path("{requestId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getById(@PathParam("requestId") int requestId) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      return Response.ok(retrieveById(conn, requestId)).build();
    }
    finally {
      conn.close();
    }

  }


  @DELETE
  @Path("{requestId}")
  public Response deleteRequest(
      @PathParam("requestId") int requestId)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      Request request = retrieveById(conn, requestId);
      checkPermissions(request, null, "delete");
      PreparedStatement delete = conn.prepareStatement(SQL_DELETE_REQUEST);
      delete.setInt(1, requestId);
      delete.executeUpdate();
      return Response.noContent().build();
    }
    finally {
      conn.close();
    }
  }


  public static List<Request> getRequestsForDistrict(
      Connection conn,
      int districtId)
      throws SQLException {

    String whereClause = " WHERE " + Request.DISTRICT_ID + " = ?";
    String orderBy = " ORDER BY " + Request.LAST_MODIFIED;
    PreparedStatement statement = conn.prepareStatement(SQL_SELECT_REQUEST + whereClause + orderBy);
    statement.setInt(1, districtId);
    ResultSet rs = statement.executeQuery();
    List<Request> requests = new ArrayList<>();
    while (rs.next()) {
      requests.add(new Request(rs));
    }
    return requests;
  }



  private Request retrieveById(
      Connection conn,
      int requestId)
    throws SQLException {

    String whereClause = " WHERE " + Request.REQUEST_ID + " = ?";
    PreparedStatement statement = conn.prepareStatement(SQL_SELECT_REQUEST + whereClause);
    statement.setInt(1, requestId);
    ResultSet rs = statement.executeQuery();
    if (!rs.next()) {
      throw new NotFoundException("No request found with ID '" + requestId + "'");
    }
    return new Request(rs);
  }


  private void checkPermissions(Request request, Request previousRequest, String action) {

    Admin currentUser = (Admin) requestContext.getProperty(GCAuth.CURRENT_PRINCIPAL);
    if (!currentUser.isRoot()) {
      if (!currentUser.getDistricts().contains(request.getDistrictId())) {
        throw new ForbiddenException(String.format(
            "District admin cannot %s a request for a district not managed by the admin.", action));
      }
      if (previousRequest != null && !currentUser.getDistricts().contains(previousRequest.getDistrictId())) {
        throw new ForbiddenException(
            "District admin cannot modify the associated district to a district not managed by the admin.");
      }
    }
  }

}


