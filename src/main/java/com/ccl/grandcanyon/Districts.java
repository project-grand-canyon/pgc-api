package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.District;
import com.ccl.grandcanyon.types.DistrictHydrated;
import com.ccl.grandcanyon.types.Scope;
import com.ccl.grandcanyon.types.TalkingPoint;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.sql.*;
import java.util.*;

@Path("/districts")
public class Districts {

  private static final String ORDERING = "ordering";

  private static final String SQL_SELECT_DISTRICT = "SELECT * FROM districts";

  private static final String SQL_CREATE_DISTRICT =
      "INSERT INTO districts (" +
          District.STATE + ", " +
          District.DISTRICT_NUMBER + ", " +
          District.REP_FIRST_NAME + ", " +
          District.REP_LAST_NAME + ", " +
          District.REP_IMAGE_URL + ", " +
          District.INFO +
          ") VALUES (?, ?, ?, ?, ?, ?)";

  private static final String SQL_UPDATE_DISTRICT =
      "UPDATE districts SET " +
          District.STATE + " = ?, " +
          District.DISTRICT_NUMBER + " = ?, " +
          District.REP_FIRST_NAME + " = ?, " +
          District.REP_LAST_NAME + " = ?, " +
          District.REP_IMAGE_URL + " = ?, " +
          District.INFO + " = ? " +
          "WHERE " + District.DISTRICT_ID + " = ?";

  private static final String SQL_DELETE_DISTRICT =
      "DELETE FROM districts " +
          "WHERE " + District.DISTRICT_ID + " = ?";

  private static final String SQL_SELECT_SCRIPT =
      "SELECT * FROM district_scripts WHERE " +
          District.DISTRICT_ID + " = ? " +
          " ORDER BY " + ORDERING + " ASC";

  private static final String SQL_CREATE_SCRIPT =
      "INSERT INTO district_scripts (" +
          District.DISTRICT_ID + ", " +
          TalkingPoint.TALKING_POINT_ID + ", " +
          ORDERING + ") VALUES ";

  private static final String SQL_DELETE_SCRIPT =
      "DELETE FROM district_scripts " +
          "WHERE " + District.DISTRICT_ID + " = ?";


  @Context
  UriInfo uriInfo;


  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.SUPER_ADMIN_ROLE)
  public Response createDistrict(District district) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement insertStatement = conn.prepareStatement(SQL_CREATE_DISTRICT,
          Statement.RETURN_GENERATED_KEYS);
      int idx = 1;
      insertStatement.setString(idx++, district.getState());
      insertStatement.setInt(idx++, district.getNumber());
      insertStatement.setString(idx++, district.getRepFirstName());
      insertStatement.setString(idx++, district.getRepLastName());
      insertStatement.setString(idx++, district.getRepImageUrl());
      insertStatement.setString(idx++, district.getInfo());
      insertStatement.executeUpdate();

      // fetch the new District and return to client
      int districtId;
      ResultSet rs = insertStatement.getGeneratedKeys();
      if (rs.next()) {
        districtId = rs.getInt(1);
        rs.close();
      }
      else {
        throw new SQLException("Create of District failed, no ID obtained.");
      }

      District newDistrict = retrieveDistrictById(conn, districtId);
      URI location = uriInfo.getAbsolutePathBuilder().path(Integer.toString(newDistrict.getDistrictId())).build();
      return Response.created(location).entity(newDistrict).build();
    }
    finally {
      conn.close();
    }
  }


  @PUT
  @Path("{districtId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateDistrict(
      @PathParam("districtId") int districtId,
      District district)
      throws SQLException {

    // todo: ensure admin is for district

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement(SQL_UPDATE_DISTRICT);
      int idx = 1;
      statement.setString(idx++, district.getState());
      statement.setInt(idx++, district.getNumber());
      statement.setString(idx++, district.getRepFirstName());
      statement.setString(idx++, district.getRepLastName());
      statement.setString(idx++, district.getRepImageUrl());
      statement.setString(idx++, district.getInfo());
      statement.setInt(idx++, districtId);
      statement.executeUpdate();

      return Response.ok(retrieveDistrictById(conn, districtId)).build();
    }
    finally {
      conn.close();
    }
  }


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDistricts(
      @QueryParam("id") Integer id,
      @QueryParam("number") Integer number,
      @QueryParam("state") String state,
      @QueryParam("hydrated") Boolean hydratedParam)
      throws SQLException {

    // check params
    if (id != null && (state != null || number != null)) {
      throw new BadRequestException("Cannot specify both id query parameter and state/number");
    }
    if ((number != null && state == null) || (state != null && number == null) ) {
      throw new BadRequestException("Must specify both number and state parameter");
    }

    boolean hydrated = (hydratedParam != null) && hydratedParam;
    if (hydrated && (id == null && state == null)) {
      throw new BadRequestException("Hydrated parameter not supported when returning multiple results");
    }

    Connection conn = SQLHelper.getInstance().getConnection();
    try {

      PreparedStatement statement;
      StringBuilder query = new StringBuilder(SQL_SELECT_DISTRICT);
      if (id != null) {
        query.append(" WHERE " + District.DISTRICT_ID + " = ?");
        statement = conn.prepareStatement(query.toString());
        statement.setInt(1, id);
      }
      else if (state != null) {
        query.append(" WHERE " + District.STATE + " = ?");
        query.append(" AND " + District.DISTRICT_NUMBER + " = ?");
        statement = conn.prepareStatement(query.toString());
        statement.setString(1, state);
        statement.setInt(2, number);
      }
      else {
        statement = conn.prepareStatement(query.toString());
      }
      ResultSet rs = statement.executeQuery();

      List<District> districts = new ArrayList<>();
      if (hydrated) {
        if (rs.next()) {
          districts.add(getDistrictHydrated(conn, rs));
        }
      }
      else {
        while (rs.next()) {
          districts.add(new District(rs));
        }
      }
      return Response.ok(districts).build();
    }
    finally {
      conn.close();
    }
  }


  @GET
  @Path("{districtId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getById(@PathParam("districtId") int districtId)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      return Response.ok(retrieveDistrictById(conn, districtId)).build();
    }
    finally {
      conn.close();
    }
  }


  @DELETE
  @Path("{districtId}")
  public Response deleteDistrict(
      @PathParam("districtId") int districtID)
      throws SQLException {

    // todo: ensure admin is for district

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement delete = conn.prepareStatement(SQL_DELETE_DISTRICT);
      delete.setInt(1, districtID);
      delete.executeUpdate();
      return Response.noContent().build();
    }
    finally {
      conn.close();
    }
  }


  @PUT
  @Path("{districtId}/script")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createOrUpdateScript(
      @PathParam("districtId") int districtId,
      List<Integer> orderedTalkingPoints)
      throws SQLException {

    // todo: ensure admin is for district

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      // make sure each talking point is valid for this district
      Map<Integer,TalkingPoint> tpMap = TalkingPoints.getSelectedTalkingPoints(
          conn, orderedTalkingPoints);
      for (TalkingPoint tp : tpMap.values()) {
        if (tp.getScope() == Scope.district && !tp.getDistricts().contains(districtId)) {
          throw new BadRequestException(String.format(
              "Request includes talking point %d that is not valid for district %d",
              tp.getTalkingPointId(), districtId));
        }
        if (tp.getScope() == Scope.state) {
          District district = retrieveDistrictById(conn, districtId);
          if (!tp.getStates().contains(district.getState())) {
            throw new BadRequestException(String.format(
                "Request includes talking point %d that is not valid for state '%s'",
                tp.getTalkingPointId(), district.getState()));
          }
        }
      }

      conn.setAutoCommit(false);
      PreparedStatement delete = conn.prepareStatement(SQL_DELETE_SCRIPT);
      delete.setInt(1, districtId);
      int rowsDeleted = delete.executeUpdate();

      if (!orderedTalkingPoints.isEmpty()) {
        StringBuilder insert = new StringBuilder(SQL_CREATE_SCRIPT);
        int sequence = 1;
        for (Integer talkingPointId : orderedTalkingPoints) {
          insert.append("(").
              append(districtId).
              append(",").
              append(talkingPointId).
              append(",").
              append(sequence++).
              append("),");
        }
        insert.deleteCharAt(insert.length() - 1);
        conn.createStatement().executeUpdate(insert.toString());
      }
      conn.commit();

      URI location = uriInfo.getAbsolutePathBuilder().build();
      Response.ResponseBuilder builder = rowsDeleted == 0 ?
          Response.created(location).entity(orderedTalkingPoints) :
          Response.ok(orderedTalkingPoints).location(location);
      return builder.build();
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
  @Path("{districtId}/script")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getScript(@PathParam("districtId") int districtId)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      return Response.ok(getScriptTalkingPointIds(conn, districtId)).build();
    }
    finally {
      conn.close();
    }
  }


  @GET
  @Path("{districtId}/hydrated")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDistrictHydrated(@PathParam("districtId") int districtId)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      ResultSet rs = getResultSetForId(conn, districtId);
      return Response.ok(getDistrictHydrated(conn, rs)).build();
    }
    finally {
      conn.close();
    }
  }


  private DistrictHydrated getDistrictHydrated(
      Connection conn,
      ResultSet rs) throws SQLException {

    DistrictHydrated district = new DistrictHydrated(rs);

    List<Integer> orderedTalkingPointIds = getScriptTalkingPointIds(conn,
        district.getDistrictId());

    if (orderedTalkingPointIds.isEmpty()) {
      district.setScript(Collections.emptyList());
    }
    else {
      Map<Integer, TalkingPoint> tpMap = TalkingPoints.getSelectedTalkingPoints(
          conn, orderedTalkingPointIds);
      List<TalkingPoint> orderedTalkingPoints = new ArrayList<>();
      for (int id : orderedTalkingPointIds) {
        TalkingPoint tp = tpMap.get(id);
        if (tp != null) {
          orderedTalkingPoints.add(tp);
        }
      }
      district.setScript(orderedTalkingPoints);
    }

    district.setOffices(DistrictOffices.getDistrictOffices(conn, district.getDistrictId()));
    return district;
  }



  private District retrieveDistrictById(
      Connection conn,
      int districtId)
      throws SQLException {

    return new District(getResultSetForId(conn, districtId));
  }



  private ResultSet getResultSetForId(
      Connection conn,
      int districtId)
      throws SQLException {

    String whereClause = " WHERE " + District.DISTRICT_ID + " = ?";
    PreparedStatement statement = conn.prepareStatement(SQL_SELECT_DISTRICT + whereClause);
    statement.setInt(1, districtId);
    ResultSet rs = statement.executeQuery();
    if (!rs.next()) {
      throw new NotFoundException("No district found with ID '" + districtId + "'");
    }
    return rs;
  }



  private List<Integer> getScriptTalkingPointIds(
      Connection conn,
      int districtId)
      throws SQLException {

    PreparedStatement statement = conn.prepareStatement(SQL_SELECT_SCRIPT);
    statement.setInt(1, districtId);
    ResultSet rs = statement.executeQuery();

    List<Integer> orderedTalkingPoints = new ArrayList<>();
    while (rs.next()) {
      orderedTalkingPoints.add(rs.getInt(TalkingPoint.TALKING_POINT_ID));
    }
    return orderedTalkingPoints;
  }


}
