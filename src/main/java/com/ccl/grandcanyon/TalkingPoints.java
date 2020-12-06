package com.ccl.grandcanyon;


import com.ccl.grandcanyon.types.Admin;
import com.ccl.grandcanyon.types.ReviewStatus;
import com.ccl.grandcanyon.types.TalkingPoint;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/talkingpoints")
public class TalkingPoints {

  private enum TalkingPointAction {
    CREATE,
    MODIFY,
    DELETE,
    REVIEW
  }

  private static final String SQL_SELECT_TALKING_POINT =
      "SELECT tp.*, tps.district_id, tps.state " +
          "FROM talking_points tp " +
          "LEFT JOIN talking_points_scopes AS tps ON tps.talking_point_id = tp.talking_point_id";

  private static final String SQL_CREATE_TALKING_POINT =
      "INSERT INTO talking_points (" +
          TalkingPoint.CONTENT + ", " +
          TalkingPoint.ENABLED + ", " +
          TalkingPoint.SCOPE + ", " +
          TalkingPoint.THEME_ID + ", " +
          TalkingPoint.CREATED_BY + ", " +
          TalkingPoint.REFERENCE_URL + ", " +
          TalkingPoint.REVIEW_STATUS +
          ") VALUES (?, ?, ?, ?, ?, ?, ?)";

  private static final String SQL_INSERT_TALKING_POINT_STATES =
      "INSERT INTO talking_points_scopes (" +
          TalkingPoint.TALKING_POINT_ID + ", " +
          TalkingPoint.STATE + ") VALUES ";

  private static final String SQL_INSERT_TALKING_POINT_DISTRICTS =
      "INSERT INTO talking_points_scopes (" +
          TalkingPoint.TALKING_POINT_ID + ", " +
          TalkingPoint.DISTRICT_ID + ") VALUES ";

  private static final String SQL_UPDATE_TALKING_POINT =
      "UPDATE talking_points SET " +
          TalkingPoint.CONTENT + " = ?, " +
          TalkingPoint.ENABLED + " = ?, " +
          TalkingPoint.SCOPE + " = ?, " +
          TalkingPoint.THEME_ID + " = ?, " +
          TalkingPoint.REFERENCE_URL + " = ?, " +
          TalkingPoint.REVIEW_STATUS + " = ? " +
          "WHERE " + TalkingPoint.TALKING_POINT_ID + " = ?";

  private static final String SQL_DELETE_TALKING_POINT =
      "DELETE FROM talking_points " +
          "WHERE " + TalkingPoint.TALKING_POINT_ID + " = ?";

  private static final String SQL_DELETE_TALKING_POINT_SCOPES =
      "DELETE FROM talking_points_scopes " +
          "WHERE " + TalkingPoint.TALKING_POINT_ID + " = ?";

  private static final String SQL_CLEAR_ADMIN_TALKING_POINTS =
      "UPDATE talking_points SET " +
          TalkingPoint.CREATED_BY + " = NULL " +
          "WHERE " + TalkingPoint.CREATED_BY + " = ?";

  @Context
  UriInfo uriInfo;

  @Context
  ContainerRequestContext requestContext;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createTalkingPoint(TalkingPoint talkingPoint) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      checkPermissions(conn, talkingPoint, TalkingPointAction.CREATE);

      ReviewStatus reviewStatus;

      try {
        checkPermissions(conn, talkingPoint, TalkingPointAction.REVIEW);
        reviewStatus = ReviewStatus.promoted; // if the admin has review privileges, their talking point is automatically approved
      } catch (ForbiddenException e) {
        reviewStatus = ReviewStatus.waiting_review;
      }

      // wrap multiple inserts in a transaction
      conn.setAutoCommit(false);
      PreparedStatement insert = conn.prepareStatement(SQL_CREATE_TALKING_POINT,
          Statement.RETURN_GENERATED_KEYS);
      int idx = 1;
      insert.setString(idx++, talkingPoint.getContent());
      insert.setBoolean(idx++, talkingPoint.isEnabled());
      insert.setString(idx++, talkingPoint.getScope().name());
      insert.setInt(idx++, talkingPoint.getThemeId());
      insert.setInt(idx++, ((Admin)requestContext.getProperty(GCAuth.CURRENT_PRINCIPAL)).getAdminId());
      insert.setString(idx++, talkingPoint.getReferenceUrl());
      insert.setString(idx++, reviewStatus.name());
      insert.executeUpdate();

      int talkingPointId;
      ResultSet rs = insert.getGeneratedKeys();
      if (rs.next()) {
        talkingPointId = rs.getInt(1);
        rs.close();
      }
      else {
        throw new SQLException("Create of talking point failed, no ID obtained.");
      }

      insertScopes(conn, talkingPointId, talkingPoint);

      TalkingPoint newTalkingPoint = retrieveById(conn, talkingPointId);
      conn.commit();

      EventAlertingService.getInstance().handleEvent("New Talking Point", newTalkingPoint.getContent());

      URI location = uriInfo.getAbsolutePathBuilder().path(Integer.toString(newTalkingPoint.getTalkingPointId())).build();
      return Response.created(location).entity(newTalkingPoint).build();
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
  @Path("{talkingPointId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateTalkingPoint(
      @PathParam("talkingPointId") int talkingPointId,
      TalkingPoint talkingPoint)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      checkPermissions(conn, talkingPoint, TalkingPointAction.MODIFY);
      // use transaction
      conn.setAutoCommit(false);

      ReviewStatus reviewStatus;

      if (talkingPoint.getReviewStatus() == null || talkingPoint.getReviewStatus() == ReviewStatus.waiting_review) {
        reviewStatus = ReviewStatus.waiting_review;
      } else {
        checkPermissions(conn, talkingPoint, TalkingPointAction.REVIEW);
        reviewStatus = talkingPoint.getReviewStatus();
      }

      PreparedStatement update = conn.prepareStatement(SQL_UPDATE_TALKING_POINT);
      int idx = 1;
      update.setString(idx++, talkingPoint.getContent());
      update.setBoolean(idx++, talkingPoint.isEnabled());
      update.setString(idx++, talkingPoint.getScope().name());
      update.setInt(idx++, talkingPoint.getThemeId());
      update.setString(idx++, talkingPoint.getReferenceUrl());
      update.setString(idx++, reviewStatus.name());
      update.setInt(idx++, talkingPointId);
      update.executeUpdate();

      // replace scopes with updated set
      deleteScopes(conn, talkingPointId);
      insertScopes(conn, talkingPointId, talkingPoint);

      TalkingPoint updatedTalkingPoint = retrieveById(conn, talkingPointId);
      conn.commit();
      return Response.ok(updatedTalkingPoint).build();
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
  public Response getTalkingPoints() throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      List<TalkingPoint> talkingPoints = new ArrayList<>();
      // order by talking point ID to ensure that multiple rows belonging to the
      // same Talking Point are returned consecutively
      ResultSet rs = conn.createStatement().executeQuery(SQL_SELECT_TALKING_POINT +
          " ORDER BY tp." + TalkingPoint.TALKING_POINT_ID);
      while (rs.next()) {
        talkingPoints.add(new TalkingPoint(rs));
      }
      return Response.ok(talkingPoints).build();
    }
    finally {
      conn.close();
    }
  }


  @GET
  @Path("{talkingPointId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getById(
      @PathParam("talkingPointId") int talkingPointId)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      return Response.ok(retrieveById(conn, talkingPointId)).build();
    }
    finally {
      conn.close();
    }
  }


  @DELETE
  @Path("{talkingPointId}")
  public Response deleteTalkingPoint(
      @PathParam("talkingPointId") int talkingPointId)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      checkPermissions(conn, retrieveById(conn, talkingPointId), TalkingPointAction.DELETE);
      PreparedStatement delete = conn.prepareStatement(SQL_DELETE_TALKING_POINT);
      delete.setInt(1, talkingPointId);
      delete.executeUpdate();
      return Response.noContent().build();
    }
    finally {
      conn.close();
    }
  }


  public static Map<Integer, TalkingPoint> getSelectedTalkingPoints(
      Connection conn,
      List<Integer> talkingPointIds)
      throws SQLException {

    Map<Integer, TalkingPoint> map = new HashMap<>();
    StringBuilder query = new StringBuilder(SQL_SELECT_TALKING_POINT).
        append(" WHERE tp.").
        append(TalkingPoint.TALKING_POINT_ID).
        append(" IN (");

    for (Integer id : talkingPointIds) {
      query.append(id).append(",");
    }
    query.deleteCharAt(query.length()-1);
    query.append(") ORDER BY tp.").
        append(TalkingPoint.TALKING_POINT_ID);
    ResultSet rs = conn.createStatement().executeQuery(query.toString());
    while (rs.next()) {
      TalkingPoint tp = new TalkingPoint(rs);
      map.put(tp.getTalkingPointId(), tp);
    }
    return map;
  }


  public static void clearTalkingPointsForAdmin(
      Connection conn,
      int adminId)
      throws SQLException {

    PreparedStatement statement = conn.prepareStatement(SQL_CLEAR_ADMIN_TALKING_POINTS);
    statement.setInt(1, adminId);
    statement.executeUpdate();
  }


  private TalkingPoint retrieveById(
      Connection conn,
      int talkingPontId) throws SQLException {

    String whereClause = " WHERE tp." + TalkingPoint.TALKING_POINT_ID + " = ?";
    PreparedStatement statement = conn.prepareStatement(SQL_SELECT_TALKING_POINT + whereClause);
    statement.setInt(1, talkingPontId);
    ResultSet rs = statement.executeQuery();
    if (!rs.next()) {
      throw new NotFoundException("No talking point found with ID '" + talkingPontId + "'");
    }
    return new TalkingPoint(rs);
  }



  private void insertScopes(
      Connection conn,
      int talkingPointId,
      TalkingPoint talkingPoint) throws SQLException {

    if (!talkingPoint.getDistricts().isEmpty()) {
      StringBuilder districtInsert = new StringBuilder(SQL_INSERT_TALKING_POINT_DISTRICTS);
      for (int districtId : talkingPoint.getDistricts()) {
        districtInsert.append("(").
            append(talkingPointId).
            append(",").
            append(districtId).
            append("),");
      }
      districtInsert.deleteCharAt(districtInsert.length()-1);
      conn.createStatement().executeUpdate(districtInsert.toString());
    }

    if (!talkingPoint.getStates().isEmpty()) {
      StringBuilder stateInsert = new StringBuilder(SQL_INSERT_TALKING_POINT_STATES);
      for (String state : talkingPoint.getStates()) {
        stateInsert.append("(").
            append(talkingPointId).
            append(",'").
            append(state).
            append("'),");
      }
      stateInsert.deleteCharAt(stateInsert.length()-1);
      conn.createStatement().executeUpdate(stateInsert.toString());
    }
  }


  private void deleteScopes(
      Connection conn,
      int talkingPointId)
      throws SQLException {

    PreparedStatement delete = conn.prepareStatement(SQL_DELETE_TALKING_POINT_SCOPES);
    delete.setInt(1, talkingPointId);
    delete.executeUpdate();
  }

  private void checkPermissions(
      Connection conn,
      TalkingPoint talkingPoint,
      TalkingPointAction action) throws SQLException {

    // check admin permission to create this talking point
    Admin currentUser = (Admin)requestContext.getProperty(GCAuth.CURRENT_PRINCIPAL);

    if (currentUser == null) {
      throw new ForbiddenException(String.format(
              "Only admins are permitted to %s talking points", action));
    }

    if (currentUser.isRoot()) {
      // Root can do everything
      return;
    }

    switch (action) {
      case REVIEW:
        throw new ForbiddenException(String.format("Only super admins are permitted to review talking points"));
      case CREATE:
        // No-op: any admin can create a talking point at any scope, for any district or state
        return;
      case MODIFY:
      case DELETE:

        PreparedStatement statement = conn.prepareStatement(SQL_SELECT_TALKING_POINT +
                " WHERE tp." + TalkingPoint.TALKING_POINT_ID + " = ?");
        statement.setInt(1, talkingPoint.getTalkingPointId());
        ResultSet rs = statement.executeQuery();
        if (rs.next()) {
          TalkingPoint talkingPointDao = new TalkingPoint(rs);
          if (talkingPointDao.getCreatedBy() == currentUser.getAdminId()) {
            return;
          }
        }
        throw new ForbiddenException(String.format(
                "Admins are permitted to %s only those talking points they have created", action));
    }
  }
}
