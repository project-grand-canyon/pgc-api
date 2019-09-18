package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.Admin;
import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.ReminderHistory;
import com.ccl.grandcanyon.types.ReminderStatus;
import com.fasterxml.jackson.databind.node.BooleanNode;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



@Path("/reminders")
public class Reminders {

  @Context
  ContainerRequestContext requestContext;

  private final static String SQL_SELECT_REMINDER_HISTORY =
      "SELECT * FROM reminder_history";
  /**
   * Generate on-demand reminder, for testing reminder delivery.
   */
  @PUT
  @Path("{callerId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response sendReminder(
      @PathParam("callerId") int callerId)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      Caller caller = Callers.retrieveById(conn, callerId);
      checkPermissions(caller.getDistrictId(), "send a call notification");
      ReminderStatus status = ReminderService.getInstance().sendReminder(conn, caller);
      return Response.ok(BooleanNode.valueOf(status.success())).build();
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


  /**
   * Get reminder history for a specified caller.
   */
  @GET
  @Path("{callerId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRemindersForCaller(
      @PathParam("callerId") int callerId)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      Caller caller = Callers.retrieveById(conn, callerId);
      checkPermissions(caller.getDistrictId(), "retrieve notification history");
      List<ReminderHistory> reminders = new ArrayList<>();
      String whereClause = " WHERE " + ReminderHistory.CALLER_ID + " = ?";
      PreparedStatement statement = conn.prepareStatement(SQL_SELECT_REMINDER_HISTORY + whereClause);
      statement.setInt(1, callerId);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        reminders.add(new ReminderHistory(rs));

      }
      return Response.ok(reminders).build();
    }
    finally {
      conn.close();
    }
  }

}
