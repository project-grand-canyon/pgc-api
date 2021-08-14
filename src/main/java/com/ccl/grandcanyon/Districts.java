package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.*;

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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Path("/districts")
public class Districts {
  private static final Logger logger = Logger.getLogger(Callers.class.getName());

  private static final String ORDERING = "ordering";

  private static final String SQL_SELECT_DISTRICT = "SELECT d.*, ct.target_district_id, ct.percentage "
      + "FROM districts d " + "LEFT JOIN call_targets AS ct ON ct.district_id = d.district_id";

  private static final String SQL_CREATE_DISTRICT = "INSERT INTO districts (" + District.STATE + ", "
      + District.DISTRICT_NUMBER + ", " + District.REP_FIRST_NAME + ", " + District.REP_LAST_NAME + ", "
      + District.REP_IMAGE_URL + ", " + District.INFO + ", " + District.PARTY + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

  private static final String SQL_UPDATE_DISTRICT = "UPDATE districts SET " + District.STATE + " = ?, "
      + District.DISTRICT_NUMBER + " = ?, " + District.REP_FIRST_NAME + " = ?, " + District.REP_LAST_NAME + " = ?, "
      + District.REP_IMAGE_URL + " = ?, " + District.INFO + " = ?, " + District.STATUS + " = ?, " + District.TIME_ZONE
      + " = ?, " + District.DELEGATE_SCRIPT + " = ?, " +  District.PARTY + " = ? " + "WHERE " + District.DISTRICT_ID + " = ?";

  private static final String SQL_DELETE_DISTRICT = "DELETE FROM districts " + "WHERE " + District.DISTRICT_ID + " = ?";

  private static final String SQL_SELECT_SCRIPT = "SELECT * FROM district_scripts WHERE " + District.DISTRICT_ID
      + " = ? " + " ORDER BY " + ORDERING + " ASC";

  private static final String SQL_CREATE_SCRIPT = "INSERT INTO district_scripts (" + District.DISTRICT_ID + ", "
      + TalkingPoint.TALKING_POINT_ID + ", " + ORDERING + ") VALUES ";

  private static final String SQL_DELETE_SCRIPT = "DELETE FROM district_scripts " + "WHERE " + District.DISTRICT_ID
      + " = ?";

  private static final String SQL_UPDATE_SCRIPT_MODIFIED_TIME = "UPDATE districts set " + District.SCRIPT_MODIFIED_TIME
      + " = ? " + "WHERE " + District.DISTRICT_ID + " = ?";

  private static final String SQL_INSERT_CALL_TARGETS = "INSERT into call_targets (" + CallTarget.DISTRICT_ID + ", "
      + CallTarget.TARGET_DISTRICT_ID + ", " + CallTarget.PERCENTAGE + ") VALUES ";

  private static final String SQL_DELETE_CALL_TARGETS = "DELETE from call_targets where " + CallTarget.DISTRICT_ID
      + " = ?";
  


  @Context
  UriInfo uriInfo;

  @Context
  ContainerRequestContext requestContext;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.SUPER_ADMIN_ROLE)
  public Response createDistrict(District district) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      conn.setAutoCommit(false);
      PreparedStatement insertStatement = conn.prepareStatement(SQL_CREATE_DISTRICT, Statement.RETURN_GENERATED_KEYS);
      int idx = 1;
      insertStatement.setString(idx++, district.getState());
      insertStatement.setInt(idx++, district.getNumber());
      insertStatement.setString(idx++, district.getRepFirstName());
      insertStatement.setString(idx++, district.getRepLastName());
      insertStatement.setString(idx++, district.getRepImageUrl());
      insertStatement.setString(idx++, district.getInfo());
      insertStatement.setString(idx++, district.getParty().toString());
      insertStatement.executeUpdate();

      // fetch the new District and return to client
      int districtId;
      ResultSet rs = insertStatement.getGeneratedKeys();
      if (rs.next()) {
        districtId = rs.getInt(1);
        rs.close();
      } else {
        throw new SQLException("Create of District failed, no ID obtained.");
      }

      insertCallTargets(conn, districtId, district);

      District newDistrict = retrieveDistrictById(conn, districtId);
      conn.commit();

      URI location = uriInfo.getAbsolutePathBuilder().path(Integer.toString(newDistrict.getDistrictId())).build();
      return Response.created(location).entity(newDistrict).build();
    } catch (SQLException e) {
      conn.rollback();
      throw e;
    } finally {
      conn.setAutoCommit(true);
      conn.close();
    }
  }

  @PUT
  @Path("{districtId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateDistrict(@PathParam("districtId") int districtId, District district) throws SQLException {

    Admin currentUser = (Admin) requestContext.getProperty(GCAuth.CURRENT_PRINCIPAL);
    if (!currentUser.isRoot() && !currentUser.getDistricts().contains(districtId)) {
      throw new ForbiddenException("Not an administrator for district " + districtId);
    }

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      District oldDistrict = retrieveDistrictById(conn, districtId);

      conn.setAutoCommit(false);

      Boolean isIndicatingScriptDelegation = district.getDelegateScript() != null;
      Boolean isRequestingScriptDelegation = isIndicatingScriptDelegation && district.getDelegateScript() == true;
      Boolean delegateScript = isRequestingScriptDelegation
          || (!isIndicatingScriptDelegation && oldDistrict.getDelegateScript());

      PreparedStatement statement = conn.prepareStatement(SQL_UPDATE_DISTRICT);
      int idx = 1;
      statement.setString(idx++, district.getState());
      statement.setInt(idx++, district.getNumber());
      statement.setString(idx++, district.getRepFirstName());
      statement.setString(idx++, district.getRepLastName());
      statement.setString(idx++, district.getRepImageUrl());
      statement.setString(idx++, district.getInfo());
      String status = district.getStatus() == null ? null : district.getStatus().name();
      statement.setString(idx++, status);
      statement.setString(idx++, district.getTimeZone() == null ? oldDistrict.getTimeZone() : district.getTimeZone());
      statement.setBoolean(idx++, delegateScript);
      statement.setString(idx++, district.getParty().toString());
      statement.setInt(idx, districtId);
      statement.executeUpdate();

      deleteCallTargets(conn, districtId);
      insertCallTargets(conn, districtId, district);

      District updatedDistrict = retrieveDistrictById(conn, districtId);
      conn.commit();

      // alert callers about status change only after commit
      Status oldStatus = oldDistrict.getStatus();
      Status newStatus = updatedDistrict.getStatus();
      if (oldStatus != newStatus) {
        logger.info("Status for district with id " + districtId + " changed from " + oldStatus.toString() + " to "
            + newStatus.toString());
        DistrictStatusChangeService.getInstance().handleStatusChange(districtId, oldStatus, newStatus);
      }

      return Response.ok(updatedDistrict).build();
    } catch (SQLException e) {
      conn.rollback();
      throw e;
    } finally {
      conn.setAutoCommit(true);
      conn.close();
    }
  }

  @GET
  @RolesAllowed(GCAuth.ANONYMOUS)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDistricts(@QueryParam("id") Integer id, @QueryParam("number") Integer number,
      @QueryParam("state") String state, @QueryParam("hydrated") Boolean hydratedParam) throws SQLException {

    // check params
    if (id != null && (state != null || number != null)) {
      throw new BadRequestException("Cannot specify both id query parameter and state/number");
    }
    if ((number != null && state == null) || (state != null && number == null)) {
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
        query.append(" WHERE d." + District.DISTRICT_ID + " = ?");
        statement = conn.prepareStatement(query.toString());
        statement.setInt(1, id);
      } else if (state != null) {
        query.append(" WHERE d." + District.STATE + " = ?");
        query.append(" AND d." + District.DISTRICT_NUMBER + " = ?");
        statement = conn.prepareStatement(query.toString());
        statement.setString(1, state);
        statement.setInt(2, number);
      } else {
        statement = conn.prepareStatement(query.toString());
      }
      ResultSet rs = statement.executeQuery();

      List<District> districts = new ArrayList<>();
      if (hydrated) {
        if (rs.next()) {
          districts.add(getDistrictHydrated(conn, rs));
        }
      } else {
        while (rs.next()) {
          districts.add(new District(rs));
        }
      }
      return Response.ok(districts).build();
    } finally {
      conn.close();
    }
  }

  @GET
  @Path("{districtId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getById(@PathParam("districtId") int districtId) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      return Response.ok(retrieveDistrictById(conn, districtId)).build();
    } finally {
      conn.close();
    }
  }

  @DELETE
  @Path("{districtId}")
  public Response deleteDistrict(@PathParam("districtId") int districtId) throws SQLException {

    Admin currentUser = (Admin) requestContext.getProperty(GCAuth.CURRENT_PRINCIPAL);
    if (!currentUser.isRoot() && !currentUser.getDistricts().contains(districtId)) {
      throw new ForbiddenException("Not an administrator for district " + districtId);
    }

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement delete = conn.prepareStatement(SQL_DELETE_DISTRICT);
      delete.setInt(1, districtId);
      delete.executeUpdate();
      return Response.noContent().build();
    } finally {
      conn.close();
    }
  }

  @PUT
  @Path("{districtId}/script")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createOrUpdateScript(@PathParam("districtId") int districtId, List<Integer> orderedTalkingPoints)
      throws SQLException {

    Admin currentUser = (Admin) requestContext.getProperty(GCAuth.CURRENT_PRINCIPAL);
    if (!currentUser.isRoot() && !currentUser.getDistricts().contains(districtId)) {
      throw new ForbiddenException("Not an administrator for district " + districtId);
    }

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      // create sure each talking point is valid for this district
      Map<Integer, TalkingPoint> tpMap = TalkingPoints.getSelectedTalkingPoints(conn, orderedTalkingPoints);
      for (TalkingPoint tp : tpMap.values()) {
        if (tp.getScope() == Scope.district && !tp.getDistricts().contains(districtId)) {
          throw new BadRequestException(
              String.format("Request includes talking point %d that is not valid for district %d",
                  tp.getTalkingPointId(), districtId));
        }
        if (tp.getScope() == Scope.state) {
          District district = retrieveDistrictById(conn, districtId);
          if (!tp.getStates().contains(district.getState())) {
            throw new BadRequestException(
                String.format("Request includes talking point %d that is not valid for state '%s'",
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
          insert.append("(").append(districtId).append(",").append(talkingPointId).append(",").append(sequence++)
              .append("),");
        }
        insert.deleteCharAt(insert.length() - 1);
        conn.createStatement().executeUpdate(insert.toString());
      }

      // update script-modified time on district
      PreparedStatement updateTime = conn.prepareStatement(SQL_UPDATE_SCRIPT_MODIFIED_TIME);
      updateTime.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
      updateTime.setInt(2, districtId);
      updateTime.executeUpdate();

      conn.commit();

      URI location = uriInfo.getAbsolutePathBuilder().build();
      Response.ResponseBuilder builder = rowsDeleted == 0 ? Response.created(location).entity(orderedTalkingPoints)
          : Response.ok(orderedTalkingPoints).location(location);
      return builder.build();
    } catch (SQLException e) {
      conn.rollback();
      throw e;
    } finally {
      conn.setAutoCommit(true);
      conn.close();
    }

  }

  @GET
  @Path("{districtId}/script")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getScript(@PathParam("districtId") int districtId) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      return Response.ok(getScriptTalkingPointIds(conn, districtId)).build();
    } finally {
      conn.close();
    }
  }

  @GET
  @Path("{districtId}/hydrated")
  @RolesAllowed(GCAuth.ANONYMOUS)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDistrictHydrated(@PathParam("districtId") int districtId) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      ResultSet rs = getResultSetForId(conn, districtId);
      return Response.ok(getDistrictHydrated(conn, rs)).build();
    } finally {
      conn.close();
    }
  }

  private DistrictHydrated getDistrictHydrated(Connection conn, ResultSet rs) throws SQLException {
    
    DistrictHydrated district = new DistrictHydrated(rs);

    List<Integer> orderedTalkingPointIds = getScriptTalkingPointIds(conn, district.getDistrictId());

    if (orderedTalkingPointIds.isEmpty()) {
      district.setScript(Collections.emptyList());
    } else {
      Map<Integer, TalkingPoint> tpMap = TalkingPoints.getSelectedTalkingPoints(conn, orderedTalkingPointIds);
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
    district.setRequests(Requests.getRequestsForDistrict(conn, district.getDistrictId()));
    return district;
  }

  private void insertCallTargets(Connection conn, int districtId, District district) throws SQLException {

    if (district.getCallTargets() == null || district.getCallTargets().isEmpty()) {
      CallTarget target = new CallTarget();
      target.setTargetDistrictId(districtId);
      target.setPercentage(100);
      district.setCallTargets(Collections.singletonList(target));
    } else {
      int total = 0;
      for (CallTarget target : district.getCallTargets()) {
        total += target.getPercentage();
      }
      if (total != 100) {
        throw new BadRequestException("Percentage total for call targets must be 100.");
      }
    }

    StringBuilder callTargetInsert = new StringBuilder(SQL_INSERT_CALL_TARGETS);
    for (CallTarget target : district.getCallTargets()) {
      if (target.getTargetDistrictId() == 0) {
        target.setTargetDistrictId(districtId);
      }
      callTargetInsert.append("(").append(districtId).append(",").append(target.getTargetDistrictId()).append(",")
          .append(target.getPercentage()).append("),");
    }
    callTargetInsert.deleteCharAt(callTargetInsert.length() - 1);
    conn.createStatement().executeUpdate(callTargetInsert.toString());
  }

  private void deleteCallTargets(Connection conn, int districtId) throws SQLException {

    PreparedStatement delete = conn.prepareStatement(SQL_DELETE_CALL_TARGETS);
    delete.setInt(1, districtId);
    delete.executeUpdate();
  }

  public DistrictHydrated retrieveDistrictHydratedById(Connection conn, int districtId) throws SQLException {
    return getDistrictHydrated(conn, getResultSetForId(conn, districtId));
  }

  static District retrieveDistrictById(Connection conn, int districtId) throws SQLException {

    return new District(getResultSetForId(conn, districtId));
  }

  static private ResultSet getResultSetForId(Connection conn, int districtId) throws SQLException {

    String whereClause = " WHERE d." + District.DISTRICT_ID + " = ?";
    PreparedStatement statement = conn.prepareStatement(SQL_SELECT_DISTRICT + whereClause);
    statement.setInt(1, districtId);
    ResultSet rs = statement.executeQuery();
    if (!rs.next()) {
      throw new NotFoundException("No district found with ID '" + districtId + "'");
    }
    return rs;
  }

  static List<District> retrieveSenatorDistrictsByState(Connection conn, String state) throws SQLException {
    List<District> districts = retrieveDistrictsByState(conn, state);
    return districts.stream().filter(District::isSenatorDistrict).collect(Collectors.toList());
  }

  static List<District> retrieveDistrictsByState(Connection conn, String state) throws SQLException {
    List<District> districts = new ArrayList<>();
    ResultSet rs = getResultSetForState(conn, state);
    while (rs.next()) {
      districts.add(new District(rs));
    }

    return districts;
  }

  static private ResultSet getResultSetForState(Connection conn, String state) throws SQLException {
    String whereClause = " WHERE d." + District.STATE + " = ?";
    PreparedStatement statement = conn.prepareStatement(SQL_SELECT_DISTRICT + whereClause);
    statement.setString(1, state);
    ResultSet rs = statement.executeQuery();
    if (!rs.next()) {
      throw new NotFoundException("No district found with state '" + state + "'");
    }
    rs.previous();
    return rs;
  }

  private List<Integer> getScriptTalkingPointIds(Connection conn, int districtId) throws SQLException {

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
