package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.Call;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to save/retrieve call records.
 */
@Path("/calls")
public class Calls {

  private static final String SQL_SELECT_CALLS = "SELECT * FROM calls";

  private static final String SQL_CREATE_CALL =
      "INSERT INTO calls (" +
          Call.CALLER_ID + ", " +
          Call.DISTRICT_ID + ", " +
          Call.TALKING_POINT_ID +
          ") VALUES (?, ?, ?)";

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.ANONYMOUS)
  public Response saveCall(Call call) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement insert = conn.prepareStatement(SQL_CREATE_CALL);
      insert.setInt(1, call.getCallerId());
      insert.setInt(2, call.getDistrictId());
      insert.setInt(3, call.getTalkingPointId());
      insert.executeUpdate();
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
