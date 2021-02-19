package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.Admin;
import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.ContactMethod;
import com.ccl.grandcanyon.types.Message;
import org.apache.http.HttpStatus;

import javax.annotation.security.RolesAllowed;
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
import java.util.logging.Logger;


/**
 * Management of Grand Canyon callers.
 */
@Path("/callers")
public class Callers {

  private static final String SQL_SELECT_CALLER =
      "SELECT c.*, ccm.contact_method, r.day_of_month, r.last_reminder_timestamp, last_call_timestamp, notes FROM callers c " +
      "LEFT JOIN callers_contact_methods AS ccm ON c.caller_id = ccm.caller_id " +
      "LEFT JOIN reminders AS r ON c.caller_id = r.caller_id " +
      "LEFT JOIN (SELECT caller_id,  MAX(created) as last_call_timestamp FROM calls GROUP by caller_id) cls ON c.caller_id = cls.caller_id";

  private static final String SQL_CREATE_CALLER =
      "INSERT INTO callers (" +
          Caller.FIRST_NAME + ", " +
          Caller.LAST_NAME + ", " +
          Caller.PHONE + ", " +
          Caller.EMAIL + ", " +
          Caller.DISTRICT_ID + ", " +
          Caller.ZIPCODE + ", " +
          Caller.CCL_ID + ", " +
          Caller.REFERRER + ", " +
          Caller.PAUSED +
          ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private static final String SQL_INSERT_CALLER_CONTACT_METHODS =
      "INSERT INTO callers_contact_methods (" +
          Caller.CALLER_ID + ", " +
          Caller.CONTACT_METHOD + ") VALUES ";

  private static final String SQL_UPDATE_CALLER =
      "UPDATE callers SET " +
          Caller.FIRST_NAME + " = ?, " +
          Caller.LAST_NAME + " = ?, " +
          Caller.PHONE + " = ?, " +
          Caller.EMAIL + " = ?, " +
          Caller.DISTRICT_ID + " = ?, " +
          Caller.ZIPCODE + " = ?, " +
          Caller.CCL_ID + " = ?, " +
          Caller.REFERRER + " = ?, " +
          Caller.PAUSED + " = ?, " +
          Caller.NOTES + " = ? " +
          "WHERE " + Caller.CALLER_ID + " = ?";

  private static final String SQL_DELETE_CALLER_CONTACT_METHODS =
      "DELETE from callers_contact_methods " +
          "WHERE " + Caller.CALLER_ID + " = ?";

  private static final String SQL_DELETE_CALLER =
      "DELETE from callers " +
          "WHERE " + Caller.CALLER_ID + " = ?";

  private static final Logger logger = Logger.getLogger(Callers.class.getName());

  @Context
  UriInfo uriInfo;

  @Context
  ContainerRequestContext requestContext;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.ANONYMOUS)
  public Response createCaller(Caller caller) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      // use transaction
      conn.setAutoCommit(false);

      PreparedStatement insertStatement = conn.prepareStatement(SQL_CREATE_CALLER,
          Statement.RETURN_GENERATED_KEYS);
      int idx = 1;
      insertStatement.setString(idx++, caller.getFirstName());
      insertStatement.setString(idx++, caller.getLastName());
      insertStatement.setString(idx++, caller.getPhone());
      insertStatement.setString(idx++, caller.getEmail());
      insertStatement.setInt(idx++, caller.getDistrictId());
      insertStatement.setString(idx++, caller.getZipCode());
      insertStatement.setString(idx++, caller.getCclId());
      insertStatement.setString(idx++, caller.getReferrer());
      insertStatement.setBoolean(idx++, caller.isPaused());
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

      insertContactMethods(conn, callerId, caller);
      ReminderSQLFetcher.createInitialReminder(conn, callerId);
      Caller newCaller = retrieveById(conn, callerId);
      conn.commit();

      WelcomeService.getInstance().handleNewCaller(newCaller);

      URI location = uriInfo.getAbsolutePathBuilder().path(Integer.toString(callerId)).build();
      return Response.created(location).entity(newCaller).build();
    }
    catch (SQLException e) {
      conn.rollback();
      throw e;
    }
    finally {
      conn.setAutoCommit(true);
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
      // use transaction
      conn.setAutoCommit(false);
      PreparedStatement statement = conn.prepareStatement(SQL_UPDATE_CALLER);
      int idx = 1;
      statement.setString(idx++, caller.getFirstName());
      statement.setString(idx++, caller.getLastName());
      statement.setString(idx++, caller.getPhone());
      statement.setString(idx++, caller.getEmail());
      statement.setInt(idx++, caller.getDistrictId());
      statement.setString(idx++, caller.getZipCode());
      statement.setString(idx++, caller.getCclId());
      statement.setString(idx++, caller.getReferrer());
      statement.setBoolean(idx++, caller.isPaused());
      statement.setString(idx++, caller.getNotes());
      statement.setInt(idx++, callerId);
      statement.executeUpdate();

      deleteContactMethods(conn, callerId);
      insertContactMethods(conn, callerId, caller);
      conn.commit();
      return Response.ok(retrieveById(conn, callerId)).build();
    }
    catch (SQLException e) {
      conn.rollback();
      throw e;
    }
    finally {
      conn.setAutoCommit(true);
      conn.close();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCallers(
      @QueryParam("districtId") Integer districtId)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      return Response.ok(getCallers(conn, districtId)).build();
    }
    finally {
      conn.close();
    }
  }

  @GET
  @Path("{callerId}")
  @Produces(MediaType.APPLICATION_JSON)
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


  @POST
  @Path("onetimemessage")
  @RolesAllowed(GCAuth.SUPER_ADMIN_ROLE)
  public Response messageAllCallers(
      Message message)
      throws Exception {

    int statusCode = HttpStatus.SC_OK;
    Connection conn = SQLHelper.getInstance().getConnection();
    int numberSent = 0;
    try {
      for (Caller caller : getCallers(conn, null)) {
        if (!caller.isPaused()) {
          if (sendOneTimeMessage(caller, message)) {
            numberSent++;
          }
          else {
            statusCode = HttpStatus.SC_MULTI_STATUS;
          }
        }
      }
      String responseMessage = "\"messagesSent\": " + numberSent;
      return Response.status(statusCode).entity(responseMessage).build();
    }
    finally {
      conn.close();
    }
  }


  @POST
  @Path("onetimemessage/{districtId}")
  public Response messageDistrictCallers(
      @PathParam("districtId") int districtId,
      Message message)
      throws Exception {

    Admin currentUser = (Admin)requestContext.getProperty(GCAuth.CURRENT_PRINCIPAL);
    if (!currentUser.isRoot() && !currentUser.getDistricts().contains(districtId)) {
      throw new ForbiddenException("Not an admin for district " + districtId + ".");
    }

    int statusCode = HttpStatus.SC_OK;
    Connection conn = SQLHelper.getInstance().getConnection();
    int numberSent = 0;
    try {
      for (Caller caller : getCallers(conn, districtId)) {
        if (!caller.isPaused()) {
          if (sendOneTimeMessage(caller, message)) {
            numberSent++;
          }
          else {
            statusCode = HttpStatus.SC_MULTI_STATUS;
          }
        }
      }
      String responseMessage = "\"messagesSent\": " + numberSent;
      return Response.status(statusCode).entity(responseMessage).build();
    }
    finally {
      conn.close();
    }
  }

  private boolean sendOneTimeMessage(
      Caller caller,
      Message message) {

    boolean smsMessageSent = false;
    boolean emailMessageSent = false;

    if (caller.getContactMethods().contains(ContactMethod.sms)) {
      try {
        smsMessageSent = ReminderService.getInstance().getSmsDeliveryService().sendTextMessage(
            caller, message);
      }
      catch (Exception e) {
        logger.warning(String.format(
            "Failed to send one-time SMS message to caller {id: %d name %s %s}: %s",
            caller.getCallerId(), caller.getFirstName(), caller.getLastName(), e.getMessage()));
        smsMessageSent = false;
      }
    }
    if (caller.getContactMethods().contains(ContactMethod.email)) {
      try {
        emailMessageSent = ReminderService.getInstance().getEmailDeliveryService().sendTextMessage(
            caller, message);
      }
      catch (Exception e) {
        logger.warning(String.format(
            "Failed to send one-time email message to caller {id: %d name %s %s}: %s",
            caller.getCallerId(), caller.getFirstName(), caller .getLastName(), e.getMessage()));
        emailMessageSent = false;
      }
    }
    return smsMessageSent | emailMessageSent;

  }


  static Caller retrieveById(
      Connection conn,
      int callerId)
      throws SQLException {

    String whereClause = " WHERE c." + Caller.CALLER_ID + " = ?";
    PreparedStatement statement = conn.prepareStatement(SQL_SELECT_CALLER + whereClause);
    statement.setInt(1, callerId);
    ResultSet rs = statement.executeQuery();
    if (!rs.next()) {
      throw new NotFoundException("No caller found with ID '" + callerId + "'");
    }
    return new Caller(rs);
  }



  static List<Caller> getCallers(
      Connection conn,
      Integer districtId) throws SQLException {

    List<Caller> callers = new ArrayList<>();
    ResultSet rs;
    if (districtId != null) {
      String whereClause =  " WHERE c." + Caller.DISTRICT_ID + " = ?";
      PreparedStatement statement = conn.prepareStatement(SQL_SELECT_CALLER + whereClause);
      statement.setInt(1, districtId);
      rs = statement.executeQuery();
    }
    else {
      rs = conn.createStatement().executeQuery(SQL_SELECT_CALLER);
    }
    while (rs.next()) {
      callers.add(new Caller(rs));
    }
    return callers;
  }



  private void insertContactMethods(
      Connection conn,
      int callerId,
      Caller caller) throws SQLException {

    if (!caller.getContactMethods().isEmpty()) {
      StringBuilder cmInsert = new StringBuilder(SQL_INSERT_CALLER_CONTACT_METHODS);
      for (ContactMethod contactMethod : caller.getContactMethods()) {
        cmInsert.append("(").
            append(callerId).
            append(",'").
            append(contactMethod.name()).
            append("'),");
      }
      cmInsert.deleteCharAt(cmInsert.length()-1);
      conn.createStatement().executeUpdate(cmInsert.toString());
    }
  }


  private void deleteContactMethods(
      Connection conn,
      int callerId) throws SQLException {

    PreparedStatement delete = conn.prepareStatement(SQL_DELETE_CALLER_CONTACT_METHODS);
    delete.setInt(1, callerId);
    delete.executeUpdate();
  }


}


