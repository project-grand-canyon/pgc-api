package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.*;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Class to save/retrieve call records.
 */
@Path("/calls")
public class Calls {

  @Context
  ContainerRequestContext requestContext;

  private static final String SQL_SELECT_CALLS = "SELECT * FROM calls";

  private static final String SQL_SEARCH_CALL =
      SQL_SELECT_CALLS + " WHERE " +
          Call.CALLER_ID + " = ? AND " +
          Call.MONTH + " = ? AND " +
          Call.YEAR + " = ? AND " +
          Call.DISTRICT_ID + " = ?";

  private static final String SQL_CREATE_CALL =
      "INSERT INTO calls (" +
          Call.CALLER_ID + ", " +
          Call.MONTH + ", " +
          Call.YEAR + ", " +
          Call.DISTRICT_ID +
          ") VALUES (?, ?, ?, ?)";

  private static final Logger logger = Logger.getLogger(Calls.class.getName());


  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.ANONYMOUS)
  public Response saveCall(Call call) throws SQLException {

    logger.info("Request to log call with caller id " + call.getCallerId() + "tracking id: " + call.getTrackingId());

    if (call.getTrackingId() == null) {
      throw new BadRequestException("Missing required tracking Id parameter.");
    }

      // find the caller associated with the tracking Id
      ReminderSQLFetcher fetcher = new ReminderSQLFetcher();
      Reminder reminder = fetcher.getReminderByTrackingId(call.getTrackingId());
      if (reminder == null) {
        throw new BadRequestException("Unknown or expired call tracking Id");
      }

    Connection conn = SQLHelper.getInstance().getConnection();

      try {
      // see if a call was already recorded for this tracking Id and districtId
//      LocalDateTime reminderDateTime = reminder.getLastReminderTimestamp().toLocalDateTime(); //TODO: should this be OffsetDateTime or Instant?
      PreparedStatement query = conn.prepareStatement(SQL_SEARCH_CALL);
      query.setInt(1, reminder.getCallerId());
      query.setInt(2, reminder.getReminderMonth()); //reminderDateTime.getMonthValue()
      query.setInt(3, reminder.getReminderYear()); // reminderDateTime.getYear()
      query.setInt(4, call.getDistrictId());
      ResultSet rs = query.executeQuery();
      if (rs.next()) {
        logger.info("Not logging. Call already exists for caller " + call.getCallerId() + " month: " + reminder.getReminderMonth() );
        // call already recorded for this ID
        // todo:  treat this call as anonymous?  Or just ignore it?
      }
      else {
        logger.info("Logging call for caller " + call.getCallerId() + " month: " + reminder.getReminderMonth());
        checkEligibleDistrict(conn, reminder.getCallerId(), call.getDistrictId());
        // save the call record
        PreparedStatement insert = conn.prepareStatement(SQL_CREATE_CALL);
        int idx = 1;
        insert.setInt(idx++, reminder.getCallerId());
        insert.setInt(idx++, reminder.getReminderMonth()); // was: reminderDateTime.getMonthValue()
        insert.setInt(idx++, reminder.getReminderYear()); // was: reminderDateTime.getYear()
        if (call.getDistrictId() != null) {
          insert.setInt(idx++, call.getDistrictId());
        }
        else {
          insert.setNull(idx++, Types.INTEGER);
        }
        insert.executeUpdate();
      }
      return Response.created(null).build();
    }
    finally {
      conn.close();
    }
  }


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.SUPER_ADMIN_ROLE)
  public Response getCalls() throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      List<Call> calls = new ArrayList<>();
      ResultSet rs = conn.createStatement().executeQuery(SQL_SELECT_CALLS);
      while (rs.next()) {
        calls.add(new Call(rs));
      }
      return Response.ok(calls).build();
    }
    finally {
      conn.close();
    }
  }


  @GET
  @Path("{callerId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCalls(
      @PathParam("callerId") Integer callerId)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      Caller caller = Callers.retrieveById(conn, callerId);
      checkPermissions(caller.getDistrictId(), "retrieve call history");
      List<Call> calls = new ArrayList<>();
      String whereClause = " WHERE " + Call.CALLER_ID + " = ?";
      PreparedStatement statement = conn.prepareStatement(SQL_SELECT_CALLS + whereClause);
      statement.setInt(1, callerId);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        calls.add(new Call(rs));
      }
      return Response.ok(calls).build();
    }
    finally {
      conn.close();
    }
  }


  private void checkPermissions(int districtId, String action) {
    Admin currentUser = (Admin)requestContext.getProperty(GCAuth.CURRENT_PRINCIPAL);
    if (!currentUser.isRoot()) {
      Set<Integer> allowedDistricts = new HashSet<>(currentUser.getDistricts());
      if (!allowedDistricts.contains(districtId)) {
        throw new ForbiddenException(String.format(
            "Permission denied to %s for caller in district %d.", action, districtId));
      }
    }
  }

  private void checkEligibleDistrict(Connection conn, int callerId, int districtId) throws SQLException, ForbiddenException{
    Caller caller = Callers.retrieveById(conn, callerId);
    if(caller.getDistrictId() == districtId){
      return;
    }
    District homeDistrict = Districts.retrieveDistrictById(conn, caller.getDistrictId());
    List<District> senatorDistricts = Districts.retrieveSenatorDistrictsByState(conn, homeDistrict.getState());
    boolean isSenatorDistrict = senatorDistricts.stream().anyMatch(district -> district.getDistrictId() == districtId);
    if(!isSenatorDistrict){
      throw new ForbiddenException(String.format(
              "Caller with caller_id %s is does not have representative or senator with distric_id %d", callerId, districtId));
    }
  }
}
