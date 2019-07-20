package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.Admin;
import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.ReminderStatus;
import com.fasterxml.jackson.databind.node.BooleanNode;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;


@Path("/reminders")
public class Reminders {

  @Context
  ContainerRequestContext requestContext;

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
                "Permission denied to %s for this district.", action));
      }
    }
  }
}
