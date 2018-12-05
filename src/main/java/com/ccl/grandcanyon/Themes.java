package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.Theme;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Path("/themes")
public class Themes {

  private static final String SQL_SELECT_THEME = "SELECT * FROM themes";

  private static final String SQL_CREATE_THEME =
      "INSERT INTO themes (" +
          Theme.THEME_NAME +
          ") VALUES (?)";

  private static final String SQL_UPDATE_THEME =
      "UPDATE themes SET " +
          Theme.THEME_NAME + " = ? " +
          "WHERE " + Theme.THEME_ID + " = ?";

  @Context
  UriInfo uriInfo;



  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createTheme(Theme theme) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement insertStatement = conn.prepareStatement(SQL_CREATE_THEME);
      insertStatement.setString(1, theme.getName());
      insertStatement.executeUpdate();

      // fetch the new Theme object and return to client
      String whereClause = " WHERE " + Theme.THEME_NAME + " = ?";
      PreparedStatement fetchStatement = conn.prepareStatement(SQL_SELECT_THEME + whereClause);
      fetchStatement.setString(1, theme.getName());
      ResultSet rs = fetchStatement.executeQuery();
      rs.first();
      Theme newTheme = new Theme(rs);
      URI location = uriInfo.getAbsolutePathBuilder().path(Integer.toString(newTheme.getThemeId())).build();
      return Response.created(location).entity(newTheme).build();
    }
    finally {
      conn.close();
    }
  }


  @PUT
  @Path("{themeId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateTheme(
      @PathParam("themeId") int themeId,
      Theme theme)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement(SQL_UPDATE_THEME);
      statement.setString(1, theme.getName());
      statement.setInt(2, themeId);
      statement.executeUpdate();

      return Response.ok(retrieveById(conn, themeId)).build();
    }
    finally {
      conn.close();
    }
  }


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getThemes() throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      List<Theme> themes = new ArrayList<>();
      ResultSet rs = conn.createStatement().executeQuery(SQL_SELECT_THEME);
      while (rs.next()) {
        themes.add(new Theme(rs));
      }
      return Response.ok(themes).build();
    }
    finally {
      conn.close();
    }
  }


  @GET
  @Path("{themeId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getById(@PathParam("themeId") int themeId) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      return Response.ok(retrieveById(conn, themeId)).build();
    }
    finally {
      conn.close();
    }
  }


  private Theme retrieveById(
      Connection conn,
      int themeId) throws SQLException {

    String whereClause = " WHERE " + Theme.THEME_ID + " = ?";
    PreparedStatement statement = conn.prepareStatement(SQL_SELECT_THEME + whereClause);
    statement.setInt(1, themeId);
    ResultSet rs = statement.executeQuery();
    if (!rs.next()) {
      throw new NotFoundException("No theme found with ID '" + themeId + "'");
    }
    return new Theme(rs);
  }
}
