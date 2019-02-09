package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.Call;
import com.ccl.grandcanyon.types.Reminder;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class to save/retrieve call records.
 */
@Path("/calls")
public class Calls {

  private static final String SQL_SELECT_CALLS = "SELECT * FROM calls";

  private static final String SQL_SEARCH_CALL =
      SQL_SELECT_CALLS + " WHERE " +
          Call.CALLER_ID + " = ? AND " +
          Call.MONTH + " = ? AND " +
          Call.YEAR + " = ?";

  private static final String SQL_CREATE_CALL =
      "INSERT INTO calls (" +
          Call.CALLER_ID + ", " +
          Call.MONTH + ", " +
          Call.YEAR + ", " +
          Call.DISTRICT_ID + ", " +
          Call.TALKING_POINT_ID +
          ") VALUES (?, ?, ?, ?, ?)";

  private static final Logger logger = Logger.getLogger(Calls.class.getName());


  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.ANONYMOUS)
  public Response saveCall(Call call) throws SQLException {

    if (call.getTrackingId() == null) {
      throw new BadRequestException("Missing required tracking Id parameter.");
    }

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      // find the caller associated with the tracking Id
      Reminder reminder = ReminderService.getInstance().getReminderByTrackingId(
          conn, call.getTrackingId());
      if (reminder == null) {
        throw new BadRequestException("Unknown or expired call tracking Id");
      }

      // see if a call was already recorded for this tracking Id
      LocalDateTime callDateTime = reminder.getLastReminderTimestamp().toLocalDateTime();
      PreparedStatement query = conn.prepareStatement(SQL_SEARCH_CALL);
      query.setInt(1, reminder.getCallerId());
      query.setInt(2, callDateTime.getMonthValue());
      query.setInt(3, callDateTime.getYear());
      ResultSet rs = query.executeQuery();
      if (rs.next()) {
        // call already recorded for this ID
        // todo:  treat this call as anonymous?  Or just ignore it?
      }
      else {
        // save the call record
        PreparedStatement insert = conn.prepareStatement(SQL_CREATE_CALL);
        int idx = 1;
        insert.setInt(idx++, reminder.getCallerId());
        insert.setInt(idx++, callDateTime.getMonthValue());
        insert.setInt(idx++, callDateTime.getYear());
        if (call.getDistrictId() != null) {
          insert.setInt(idx++, call.getDistrictId());
        }
        else {
          insert.setNull(idx++, Types.INTEGER);
        }
        if (call.getTalkingPointId() != null) {
          insert.setInt(idx++, call.getTalkingPointId());
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

}
