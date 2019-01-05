package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.Caller;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Management of Grand Canyon callers.
 */
@Path("/callers")
public class Callers {

  private static final String SQL_SELECT_CALLER = "SELECT * FROM callers";

  private static final String SQL_CREATE_CALLER =
      "INSERT INTO callers (" +
          Caller.FIRST_NAME + ", " +
          Caller.LAST_NAME + ", " +
          Caller.CONTACT_METHOD + ", " +
          Caller.PHONE + ", " +
          Caller.EMAIL + ", " +
          Caller.DISTRICT_ID + ", " +
          Caller.ZIPCODE +
          ") VALUES (?, ?, ?, ?, ?, ?, ?)";

  private static final String SQL_UPDATE_CALLER =
      "UPDATE callers SET " +
          Caller.FIRST_NAME + " = ?, " +
          Caller.LAST_NAME + " = ?, " +
          Caller.CONTACT_METHOD + " = ?, " +
          Caller.PHONE + " = ?, " +
          Caller.EMAIL + " = ?, " +
          Caller.DISTRICT_ID + " = ?, " +
          Caller.ZIPCODE + " = ? " +
          "WHERE " + Caller.CALLER_ID + " = ?";

  private static final String SQL_DELETE_CALLER =
      "DELETE from callers " +
          "WHERE " + Caller.CALLER_ID + " = ?";

  @Context
  UriInfo uriInfo;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.ANONYMOUS)
  public Response createCaller(Caller caller) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement insertStatement = conn.prepareStatement(SQL_CREATE_CALLER,
          Statement.RETURN_GENERATED_KEYS);
      insertStatement.setString(1, caller.getFirstName());
      insertStatement.setString(2, caller.getLastName());
      insertStatement.setString(3, caller.getContactMethod().name());
      insertStatement.setString(4, caller.getPhone());
      insertStatement.setString(5, caller.getEmail());
      insertStatement.setInt(6, caller.getDistrictId());
      insertStatement.setString(7, caller.getZipCode());
      insertStatement.executeUpdate();

      // fetch the new Caller and return to client
      int callerId;
      ResultSet rs = insertStatement.getGeneratedKeys();
      if (rs.next()) {
        callerId = rs.getInt(1);
        rs.close();
      }
      else {
        throw new SQLException("Create of Caller failed, no ID obtained.");
      }

      Caller newCaller = retrieveById(conn, callerId);
      URI location = uriInfo.getAbsolutePathBuilder().path(Integer.toString(callerId)).build();
      return Response.created(location).entity(newCaller).build();
    }
    finally {
      conn.close();
    }
  }

  @PUT
  @Path("{callerId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.ANONYMOUS)
  public Response updateCaller(
      @PathParam("callerId") int callerId,
      Caller caller)
      throws SQLException {

    // todo: verify identity of caller

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement(SQL_UPDATE_CALLER);
      statement.setString(1, caller.getFirstName());
      statement.setString(2, caller.getLastName());
      statement.setString(3, caller.getContactMethod().name());
      statement.setString(4, caller.getPhone());
      statement.setString(5, caller.getEmail());
      statement.setInt(6, caller.getDistrictId());
      statement.setString(7, caller.getZipCode());
      statement.setInt(8, callerId);
      statement.executeUpdate();

      return Response.ok(retrieveById(conn, callerId)).build();
    }
    finally {
      conn.close();
    }


  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCallers() throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      List<Caller> callers = new ArrayList<>();
      ResultSet rs = conn.createStatement().executeQuery(SQL_SELECT_CALLER);
      while (rs.next()) {
        callers.add(new Caller(rs));
      }
      return Response.ok(callers).build();
    }
    finally {
      conn.close();
    }
  }

  @GET
  @Path("{callerId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.ANONYMOUS)
  public Response getById(@PathParam("callerId") int callerId)
      throws SQLException {

    // todo: verify identity of caller

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      return Response.ok(retrieveById(conn, callerId)).build();
    }
    finally {
      conn.close();
    }
  }

  @DELETE
  @Path("{callerId}")
  public Response deleteCaller(
      @PathParam("callerId") int callerId)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement delete = conn.prepareStatement(SQL_DELETE_CALLER);
      delete.setInt(1, callerId);
      delete.executeUpdate();
      return Response.noContent().build();
    }
    finally {
      conn.close();
    }
  }



  private Caller retrieveById(
      Connection conn,
      int callerId)
      throws SQLException {

    String whereClause = " WHERE " + Caller.CALLER_ID + " = ?";
    PreparedStatement statement = conn.prepareStatement(SQL_SELECT_CALLER + whereClause);
    statement.setInt(1, callerId);
    ResultSet rs = statement.executeQuery();
    if (!rs.next()) {
      throw new NotFoundException("No caller found with ID '" + callerId + "'");
    }
    return new Caller(rs);
  }


}


