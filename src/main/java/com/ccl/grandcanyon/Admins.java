package com.ccl.grandcanyon;

import com.ccl.grandcanyon.senderservice.EmailSenderService;
import com.ccl.grandcanyon.auth.AuthenticationService;
import com.ccl.grandcanyon.auth.PasswordUtil;
import com.ccl.grandcanyon.types.*;
import com.fasterxml.jackson.databind.node.TextNode;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.*;
import java.net.URI;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;


/**
 * Class for CRUD on Grand Canyon administrators.
 */
@Path("/admins")
public class Admins {

  private static final String SQL_SELECT_ADMIN =
      "SELECT a.*, d.district_id FROM admins a " +
      "LEFT JOIN admins_districts AS d ON a.admin_id = d.admin_id";

  private static final String SQL_SELECT_DISTRICTS =
          "SELECT d.* FROM districts d " +
          "LEFT JOIN admins_districts ad ON ad.district_id = d.district_id " +
          "WHERE ad.admin_id = ?";

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

  private static final String SQL_INSERT_RESET_TOKEN =
      "INSERT INTO reset_tokens (" +
          PasswordResetToken.ADMIN_ID + ", " +
          PasswordResetToken.TOKEN + ", " +
          PasswordResetToken.EXPIRATION +
          ") VALUES (?, ?, ?)";

  private static final String SQL_SELECT_RESET_TOKEN =
      "SELECT * FROM reset_tokens WHERE " + PasswordResetToken.TOKEN + "= ?";

  private static final String SQL_DELETE_RESET_TOKEN =
      "DELETE FROM reset_tokens WHERE " + PasswordResetToken.TOKEN + "= ?";

  private final static String PASSWORD_RESET_TOKEN_LIFETIME = "passwordResetTokenLifetime";

  private static int passwordResetTokenLifetime;

  @Context
  UriInfo uriInfo;

  @Context
  ContainerRequestContext requestContext;


  static void init(Properties config) {
    passwordResetTokenLifetime = Integer.parseInt(config.getProperty(
        PASSWORD_RESET_TOKEN_LIFETIME, "10"));
  }


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

      PreparedStatement selectDistricts = conn.prepareStatement(SQL_SELECT_DISTRICTS);
      selectDistricts.setInt(1, adminId);

      ResultSet rs1 = selectDistricts.executeQuery();
      StringBuilder sb = new StringBuilder();
      while (rs1.next()) {
        District d = new District(rs1);
        sb.append(String.format("%s-%s ", d.getState(), d.getNumber()));
      }

      String newAdminEmailBody = String.format(
              "New admin. Email: %s, Username: %s, Districts: %s",
              newAdmin.getEmail(),
              newAdmin.getUserName(),
              sb.toString());

      EventAlertingService.getInstance().handleEvent("New Admin Sign Up", newAdminEmailBody);
      AdminWelcomeService.getInstance().handleNewAdmin(newAdmin);
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
  @RolesAllowed(GCAuth.ADMIN_ROLE)
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
      conn.setAutoCommit(false);
      TalkingPoints.clearTalkingPointsForAdmin(conn, adminId);
      PreparedStatement delete = conn.prepareStatement(SQL_DELETE_ADMIN);
      delete.setInt(1, adminId);
      delete.executeUpdate();
      conn.commit();
      return Response.noContent().build();
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


  /**
   * Change the password of an authenticated Admin.
   */
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

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      updatePassword(conn, passwordRequest.getNewPassword(), adminId);
      return Response.noContent().build();
    }
    finally {
      conn.close();
    }
  }


  /**
   * Request to reset a forgotten password.
   * @param emailAddress Email address to send a reset password link to.
   */
  @POST
  @Path("/password_reset_request")
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.ANONYMOUS)
  public Response passwordResetRequest(TextNode emailAddress)
    throws SQLException {
    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      String whereClause = " WHERE " + Admin.EMAIL + " = ?";
      PreparedStatement queryAdmin = conn.prepareStatement(SQL_SELECT_ADMIN + whereClause);
      queryAdmin.setString(1, emailAddress.asText());
      ResultSet rs = queryAdmin.executeQuery();
      Admin admin = null;
      if (rs.next()) {
        admin = new Admin(rs);
      }
      else {
        throw new NotFoundException("Unknown email address");
      }
      if (rs.next()) {
        // multiple admins with same email address: no-go
        throw new BadRequestException("Ambiguous email address");
      }

      // generate token and store as new password request
      UUID token = UUID.randomUUID();
      long expirationTime = System.currentTimeMillis() + (passwordResetTokenLifetime * 1000L * 60L);
      PreparedStatement insertToken = conn.prepareStatement(SQL_INSERT_RESET_TOKEN);
      int i = 1;
      insertToken.setInt(i++, admin.getAdminId());
      insertToken.setString(i++, token.toString());
      insertToken.setTimestamp(i++, new Timestamp(expirationTime));
      insertToken.executeUpdate();

      // send email to admin with link
      // TODO: replace this message body with HTML email template.
      EmailSenderService emailSender = EmailSenderService.getInstance();

      String resetUrl = emailSender.getAdminApplicationBaseUrl() + "/finish_password_reset?token=" + token;
      Message resetRequestMessage = new Message();
      resetRequestMessage.setSubject("Password Reset Requested");
      resetRequestMessage.setBody("If you requsted a password reset, visit the page at the following URL:  http://" + resetUrl +
          ".   If you did not request a password reset, please reply to this email to report the error.");

      // hack:  create a temp Caller in order to invoke email service
      Caller adminRecipient = new Caller();
      adminRecipient.setEmail(admin.getEmail());
      try {
        emailSender.getEmailDeliveryService().sendTextMessage(adminRecipient, resetRequestMessage);
      }
      catch (Exception e) {
        throw new ServerErrorException(String.format(
            "Failed to send password reset email to district administrator %s at email address %s: %s",
            admin.getUserName(), emailAddress, e.getMessage()), Response.Status.INTERNAL_SERVER_ERROR);
      }
    }
    finally {
      conn.close();
    }
    return Response.ok().build();
  }


  /**
   * Verify a password reset token.  This API should called after an email has been sent and the
   * Admin has navigated to the included URL.
   * @param tokenString the reset token string
   * @return properties of the Admin who requested the reset.
   */
  @POST
  @Path("/verify_reset_token")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.ANONYMOUS)
  public Response verifyResetToken(TextNode tokenString) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PasswordResetToken token = retrieveAndValidateResetToken(conn, tokenString.asText());
      Admin admin = retrieveById(conn, token.getAdminId());
      return Response.ok(admin).build();
    }
    finally {
      conn.close();
    }
  }


  /**
   * Reset an Admin password using a valid previously issued reset token.
   */
  @POST
  @Path("/reset_password")
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.ANONYMOUS)
  public Response resetPassword(PasswordResetRequest resetResponse) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PasswordResetToken resetToken = retrieveAndValidateResetToken(conn, resetResponse.getToken());
      updatePassword(conn, resetResponse.getPassword(), resetToken.getAdminId());
      deleteResetToken(conn, resetToken.getToken());
    }
    finally {
      conn.close();
    }
    return Response.ok().build();
  }


  private PasswordResetToken retrieveAndValidateResetToken(
      Connection conn,
      String tokenString) throws SQLException {

    PreparedStatement queryToken = conn.prepareStatement(SQL_SELECT_RESET_TOKEN);
    queryToken.setString(1, tokenString);
    ResultSet rs = queryToken.executeQuery();
    if (!rs.next()) {
      throw new NotFoundException("Invalid token");
    }
    PasswordResetToken token = new PasswordResetToken(rs);
    if (token.getExpiration().before(new Timestamp(System.currentTimeMillis()))) {
      deleteResetToken(conn, token.getToken());
      throw new BadRequestException("Expired token");
    }
    return token;
  }

  private void updatePassword(
      Connection conn,
      String password,
      int adminId) throws SQLException {

    String token = PasswordUtil.hash(password.toCharArray());
    PreparedStatement statement = conn.prepareStatement(
        "UPDATE admins SET " + Admin.TOKEN + " = ? " +
            "WHERE " + Admin.ADMIN_ID + " = ?");
    statement.setString(1, token);
    statement.setInt(2, adminId);
    statement.executeUpdate();
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


  private void deleteResetToken(
      Connection conn,
      String token) throws SQLException {

    PreparedStatement delete = conn.prepareStatement(SQL_DELETE_RESET_TOKEN);
    delete.setString(1, token);
    delete.executeUpdate();
  }

}
