package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.ContactMethod;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Path("/callers")
public class Callers {

  private static final String SQL_SELECT_ALL = "SELECT * FROM callers";
  private static final String SQL_CREATE_CALLER =
      "INSERT INTO callers (" + Caller.FIRST_NAME +
          ", " +
          Caller.LAST_NAME +
          ", " +
          Caller.CONTACT_METHOD +
          ", " +
          Caller.PHONE +
          ", " +
          Caller.EMAIL +
          ", " +
          Caller.DISTRICT_ID +
          ", " +
          Caller.ZIPCODE +
          ") VALUES (?, ?, ?, ?, ?, ?, ?)";


  @Context
  ServletContext context;

  @Context
  UriInfo uriInfo;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createCaller(Caller caller) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    PreparedStatement statement = conn.prepareStatement(SQL_CREATE_CALLER);
    statement.setString(1, caller.getFirstName());
    statement.setString(2, caller.getLastName());
    statement.setString(3, caller.getContactMethod().name());
    statement.setString(4, caller.getPhone());
    statement.setString(5, caller.getEmail());
    statement.setInt(6, caller.getDistrictId());
    statement.setString(7, caller.getZipCode());
    int result = statement.executeUpdate();
    if (result != 1) {
      // "shouldn't happen"
      throw new ServerErrorException("Insert did not create a new row",
          Response.Status.INTERNAL_SERVER_ERROR);
    }

    // fetch the new Caller and return to client
    StringBuilder whereClause = new StringBuilder(" WHERE contact_method = '");
    whereClause.append(caller.getContactMethod().name());
    whereClause.append("' AND ");
    if (caller.getContactMethod() == ContactMethod.email) {
      whereClause.append("email = '");
      whereClause.append(caller.getEmail());
    }
    else {
      whereClause.append("phone = '");
      whereClause.append(caller.getPhone());
    }
    whereClause.append("'");

    ResultSet rs = conn.prepareStatement(SQL_SELECT_ALL + whereClause).executeQuery();
    rs.first();
    Caller newCaller = new Caller(rs);
    URI location = uriInfo.getAbsolutePathBuilder().path(Integer.toString(newCaller.getCallerId())).build();
    return Response.created(location).entity(new Caller(rs)).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCallers() throws SQLException {

    List<Caller> callers = new ArrayList<>();

    Connection conn = SQLHelper.getInstance().getConnection();
    ResultSet rs = conn.createStatement().executeQuery(SQL_SELECT_ALL);
    while (rs.next()) {
      callers.add(new Caller(rs));
    }
    return Response.ok(callers, MediaType.APPLICATION_JSON_TYPE).build();
  }

  @GET
  @Path("{callerId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getById(@PathParam("callerId") String callerId) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();

    StringBuilder whereClause = new StringBuilder(" WHERE caller_id = '");
    whereClause.append(callerId);
    whereClause.append("'");
    ResultSet rs = conn.prepareStatement(SQL_SELECT_ALL + whereClause).executeQuery();
    if (!rs.next()) {
      throw new NotFoundException("No caller found with ID '" + callerId + "'");
    }
    return Response.ok(new Caller(rs)).build();
  }
}


