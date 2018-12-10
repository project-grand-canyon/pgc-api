package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.District;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Path("/districts")
public class Districts {

  private static final String SQL_SELECT_DISTRICT = "SELECT * FROM districts";

  private static final String SQL_CREATE_DISTRICT =
      "INSERT INTO districts (" +
          District.STATE + ", " +
          District.DISTRICT_NUMBER + ", " +
          District.REPRESENTATIVE + ", " +
          District.INFO +
          ") VALUES (?, ?, ?, ?)";

  private static final String SQL_UPDATE_DISTRICT =
      "UPDATE districts SET " +
          District.STATE + " = ?, " +
          District.DISTRICT_NUMBER + " = ?, " +
          District.REPRESENTATIVE + " = ?, " +
          District.INFO + " = ? " +
          "WHERE " + District.DISTRICT_ID + " = ?";

  private static final String SQL_DELETE_DISTRICT =
      "DELETE FROM districts " +
          "WHERE " + District.DISTRICT_ID + " = ?";

  @Context
  UriInfo uriInfo;


  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createDistrict(District district) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement insertStatement = conn.prepareStatement(SQL_CREATE_DISTRICT,
          Statement.RETURN_GENERATED_KEYS);
      insertStatement.setString(1, district.getState());
      insertStatement.setInt(2, district.getNumber());
      insertStatement.setString(3, district.getRepresentative());
      insertStatement.setString(4, district.getInfo());
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

      District newDistrict = retrieveById(conn, districtId);
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

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement(SQL_UPDATE_DISTRICT);
      statement.setString(1, district.getState());
      statement.setInt(2, district.getNumber());
      statement.setString(3, district.getRepresentative());
      statement.setString(4, district.getInfo());
      statement.setInt(5, districtId);
      statement.executeUpdate();

      return Response.ok(retrieveById(conn, districtId)).build();
    }
    finally {
      conn.close();
    }
  }


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDistricts() throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      List<District> districts = new ArrayList<>();
      ResultSet rs = conn.createStatement().executeQuery(SQL_SELECT_DISTRICT);
      while (rs.next()) {
        districts.add(new District(rs));
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
      return Response.ok(retrieveById(conn, districtId)).build();
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


  private District retrieveById(
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
    return new District(rs);
  }


}
