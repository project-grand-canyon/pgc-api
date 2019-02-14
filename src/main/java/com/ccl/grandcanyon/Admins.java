package com.ccl.grandcanyon;

import com.ccl.grandcanyon.auth.AuthenticationService;
import com.ccl.grandcanyon.auth.PasswordUtil;
import com.ccl.grandcanyon.types.Admin;
import com.ccl.grandcanyon.types.ChangePasswordRequest;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.*;
import java.net.URI;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Class for CRUD on Grand Canyon administrators.
 */
@Path("/admins")
public class Admins {

  private static final String SQL_SELECT_ADMIN =
      "SELECT a.*, d.district_id FROM admins a " +
      "LEFT JOIN admins_districts AS d ON a.admin_id = d.admin_id";

  private static final String SQL_CREATE_ADMIN =
      "INSERT INTO admins (" +
          Admin.USER_NAME + ", " +
          Admin.IS_ROOT + ", " +
          Admin.TOKEN + ", " +
          Admin.EMAIL + ", " +
          Admin.LOGIN_ENABLED +
          ") VALUES (?, ?, ?, ?, ?)";

  private static final String SQL_INSERT_ADMIN_DISTRICTS =
      "INSERT INTO admins_districts (" +
          Admin.ADMIN_ID + ", " +
          Admin.DISTRICT_ID + ") VALUES ";

  private static final String SQL_UPDATE_ADMIN =
      "UPDATE admins SET " +
          Admin.USER_NAME + " = ?, " +
          Admin.IS_ROOT + " = ?, " +
          Admin.TOKEN + " = ?, " +
          Admin.EMAIL + " = ?, " +
          Admin.LOGIN_ENABLED + " = ? " +
          "WHERE " + Admin.ADMIN_ID + " = ?";

  private static final String SQL_UPDATE_ADMIN_EXCEPT_PASSWORD =
      "UPDATE admins SET " +
          Admin.USER_NAME + " = ?, " +
          Admin.IS_ROOT + " = ?, " +
          Admin.EMAIL + " = ?, " +
          Admin.LOGIN_ENABLED + " = ? " +
          "WHERE " + Admin.ADMIN_ID + " = ?";

  private static final String SQL_DELETE_ADMIN_DISTRICTS =
      "DELETE FROM admins_districts " +
          "WHERE " + Admin.ADMIN_ID + " = ?";

  private static final String SQL_DELETE_ADMIN =
      "DELETE FROM admins " +
          "WHERE " + Admin.ADMIN_ID + " = ?";


  @Context
  UriInfo uriInfo;

  @Context
  ContainerRequestContext requestContext;


  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.ANONYMOUS)
  public Response createAdmin(Admin admin) throws SQLException {

    if (admin.isLoginEnabled()) {
      // creating an active Admin requires the super-admin role
      Admin activeUser = (Admin)requestContext.getProperty(GCAuth.CURRENT_PRINCIPAL);
      if (activeUser == null || !activeUser.isRoot()) {
        throw new ForbiddenException("Operation requires super-admin privilege.");
      }
    }

    if (admin.getPassword() == null) {
      throw new BadRequestException("Admin users must have a password.");
    }

    // todo: password requirements?
    String token = PasswordUtil.hash(admin.getPassword().toCharArray());

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      // use transaction for multiple inserts
      conn.setAutoCommit(false);

      PreparedStatement insert = conn.prepareStatement(SQL_CREATE_ADMIN, Statement.RETURN_GENERATED_KEYS);
      insert.setString(1, admin.getUserName());
      insert.setBoolean(2, admin.isRoot());
      insert.setString(3, token);
      insert.setString(4, admin.getEmail());
      insert.setBoolean(5, admin.isLoginEnabled());
      insert.executeUpdate();

      // return new Admin object
      int adminId;
      ResultSet rs = insert.getGeneratedKeys();
      if (rs.next()) {
        adminId = rs.getInt(1);
        rs.close();
      }
      else {
        throw new SQLException("Create of Admin failed, no ID obtained.");
      }

      insertDistricts(conn, adminId, admin);
      Admin newAdmin = retrieveById(conn, adminId);
      conn.commit();

      URI location = uriInfo.getAbsolutePathBuilder().path(Integer.toString(adminId)).build();
      return Response.created(location).entity(newAdmin).build();
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
  @Path("{adminId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.SUPER_ADMIN_ROLE)
  public Response udpateAdmin(
      @PathParam("adminId") int adminId,
      Admin admin)
      throws SQLException {

    String updateSQL;
    String token = null;
    if (admin.getPassword() == null) {
      updateSQL = SQL_UPDATE_ADMIN_EXCEPT_PASSWORD;
    }
    else {
      updateSQL = SQL_UPDATE_ADMIN;
      token = PasswordUtil.hash(admin.getPassword().toCharArray());
    }

    Connection conn = SQLHelper.getInstance().getConnection();
    try {

      Admin previousValue = retrieveById(conn, adminId);
      // use transaction
      conn.setAutoCommit(false);

      PreparedStatement update = conn.prepareStatement(updateSQL);
      int pos = 1;
      update.setString(pos++, admin.getUserName());
      update.setBoolean(pos++, admin.isRoot());
      if (token != null) {
        update.setString(pos++, token);
      }
      update.setString(pos++, admin.getEmail());
      update.setBoolean(pos++, admin.isLoginEnabled());
      update.setInt(pos++, adminId);
      update.executeUpdate();

      // replace districts with updated set
      deleteDistricts(conn, adminId);
      insertDistricts(conn, adminId, admin);

      Admin updatedAdmin = retrieveById(conn, adminId);
      conn.commit();
      return Response.ok(updatedAdmin).build();
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
  @RolesAllowed(GCAuth.SUPER_ADMIN_ROLE)
  public Response getAdmins() throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      List<Admin> admins = new ArrayList<>();
      // order by adminId to ensure that multiple rows belong to the
      // same Admin are returned consecutively
      ResultSet rs = conn.createStatement().executeQuery(SQL_SELECT_ADMIN +
          " ORDER BY a." + Admin.ADMIN_ID);
      while (rs.next()) {
        admins.add(new Admin(rs));
      }
      return Response.ok(admins).build();
    }
    finally {
      conn.close();
    }
  }


  @GET
  @Path("{adminId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getById(
      @PathParam("adminId") int adminId)
      throws SQLException {

    Admin currentUser = (Admin)requestContext.getProperty(GCAuth.CURRENT_PRINCIPAL);
    if (!currentUser.isRoot() && currentUser.getAdminId() != adminId) {
      throw new ForbiddenException("Not permitted to retrieve admins other than self");
    }
    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      return Response.ok(retrieveById(conn, adminId)).build();
    }
    finally {
      conn.close();
    }
  }


  @DELETE
  @Path("{adminId}")
  @RolesAllowed(GCAuth.SUPER_ADMIN_ROLE)
  public Response deleteAdmin(
      @PathParam("adminId") int adminId)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement delete = conn.prepareStatement(SQL_DELETE_ADMIN);
      delete.setInt(1, adminId);
      delete.executeUpdate();
      return Response.noContent().build();
    }
    finally {
      conn.close();
    }
  }


  @PUT
  @Path("{adminId}/password")
  public Response changePassword(
      @PathParam("adminId") int adminId,
      ChangePasswordRequest passwordRequest)
      throws SQLException {

    Admin activeUser = (Admin)requestContext.getProperty(GCAuth.CURRENT_PRINCIPAL);
    if (activeUser.getAdminId() != adminId) {
      throw new ForbiddenException("Change password API is self-service only");
    }

    // validate current password
    AuthenticationService.getInstance().authenticate(
        activeUser.getUserName(), passwordRequest.getCurrentPassword());

    String token = PasswordUtil.hash(passwordRequest.getNewPassword().toCharArray());
    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement(
          "UPDATE admins SET " + Admin.TOKEN + " = ? " +
          "WHERE " + Admin.ADMIN_ID + " = ?");
      statement.setString(1, token);
      statement.setInt(2, adminId);
      statement.executeUpdate();
      return Response.noContent().build();
    }
    finally {
      conn.close();
    }
  }


  public static Admin getAdminByName(String userName) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      String whereClause = " WHERE " + Admin.USER_NAME + " = ?";
      PreparedStatement statement = conn.prepareStatement(SQL_SELECT_ADMIN + whereClause);
      statement.setString(1, userName);
      ResultSet rs = statement.executeQuery();
      return rs.next() ? new Admin(rs) : null;
    }
    finally {
      conn.close();
    }
  }


  private Admin retrieveById(
      Connection conn,
      int adminId)
      throws SQLException {

    String whereClause = " WHERE a." + Admin.ADMIN_ID + " = ?";
    PreparedStatement statement = conn.prepareStatement(SQL_SELECT_ADMIN + whereClause);
    statement.setInt(1, adminId);
    ResultSet rs = statement.executeQuery();
    if (!rs.next()) {
      throw new NotFoundException("No admin found with ID '" + adminId + "'");
    }
    return new Admin(rs);
  }

  private void insertDistricts(
      Connection conn,
      int adminId,
      Admin admin) throws SQLException {

    if (!admin.getDistricts().isEmpty()) {
      StringBuilder districtInsert = new StringBuilder(SQL_INSERT_ADMIN_DISTRICTS);
      for (int districtId : admin.getDistricts()) {
        districtInsert.append("(").
            append(adminId).
            append(",").
            append(districtId).
            append("),");
      }
      districtInsert.deleteCharAt(districtInsert.length()-1);
      conn.createStatement().executeUpdate(districtInsert.toString());
    }
  }


  private void deleteDistricts(
      Connection conn,
      int adminId) throws SQLException {

    PreparedStatement delete = conn.prepareStatement(SQL_DELETE_ADMIN_DISTRICTS);
    delete.setInt(1, adminId);
    delete.executeUpdate();
  }

}
