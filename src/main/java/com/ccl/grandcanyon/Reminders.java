package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.Caller;
import com.fasterxml.jackson.databind.node.BooleanNode;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;


@Path("/reminders")
public class Reminders {

  /**
   * Generate on-demand reminder, for testing reminder delivery.
   */
  @PUT
  @Path("{callerId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.SUPER_ADMIN_ROLE)
  public Response sendReminder(
      @PathParam("callerId") int callerId)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      Caller caller = Callers.retrieveById(conn, callerId);
      boolean success = ReminderService.getInstance().sendReminder(caller, "this is a test");
      return Response.ok(BooleanNode.valueOf(success)).build();
    }
    finally {
      conn.close();
    }
  }
}
