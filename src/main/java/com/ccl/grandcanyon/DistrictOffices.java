package com.ccl.grandcanyon;



import com.ccl.grandcanyon.types.DistrictOffice;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Path("/districtoffices")
public class DistrictOffices {

  private static final String SQL_SELECT_DISTRICT_OFFICE =
      "SELECT * from district_offices";

  private static final String SQL_CREATE_DISTRICT_OFFICE =
      "INSERT INTO district_offices (" +
          DistrictOffice.DISTRICT_ID + ", " +
          DistrictOffice.ADDRESS_LINE1 + ", " +
          DistrictOffice.ADDRESS_LINE2 + ", " +
          DistrictOffice.CITY + ", " +
          DistrictOffice.STATE + ", " +
          DistrictOffice.COUNTRY + ", " +
          DistrictOffice.ZIPCODE + ", " +
          DistrictOffice.PHONE + ", " +
          DistrictOffice.EMAIL + ", " +
          DistrictOffice.OPENS_AT + ", " +
          DistrictOffice.CLOSES_AT +
          ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private static final String SQL_UPDATE_DISTRICT_OFFICE =
      "UPDATE district_offices SET " +
          DistrictOffice.DISTRICT_ID + " = ?, " +
          DistrictOffice.ADDRESS_LINE1 + " = ?, " +
          DistrictOffice.ADDRESS_LINE2 + " = ?, " +
          DistrictOffice.CITY + " = ?, " +
          DistrictOffice.STATE + " = ?, " +
          DistrictOffice.COUNTRY + " = ?, " +
          DistrictOffice.ZIPCODE + " = ?, " +
          DistrictOffice.PHONE + " = ?, " +
          DistrictOffice.EMAIL + " = ?, " +
          DistrictOffice.OPENS_AT + " = ?, " +
          DistrictOffice.CLOSES_AT + " = ? " +
          "WHERE " + DistrictOffice.DISTRICT_OFFICE_ID + " = ?";

  private static final String SQL_DELETE_DISTRICT_OFFICE =
      "DELETE from district_offices " +
          "WHERE " + DistrictOffice.DISTRICT_OFFICE_ID + " = ?";


  @Context
  UriInfo uriInfo;


  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createOffice(DistrictOffice office) throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement insertStatement = conn.prepareStatement(SQL_CREATE_DISTRICT_OFFICE,
          Statement.RETURN_GENERATED_KEYS);
      insertStatement.setInt(1, office.getDistrictId());
      insertStatement.setString(2, office.getAddress().getAddressLine1());
      insertStatement.setString(3, office.getAddress().getAddressLine2());
      insertStatement.setString(4, office.getAddress().getCity());
      insertStatement.setString(5, office.getAddress().getState());
      insertStatement.setString(6, office.getAddress().getCountry());
      insertStatement.setString(7, office.getAddress().getZipCode());
      insertStatement.setString(8, office.getPhone());
      insertStatement.setString(9, office.getEmail());
      insertStatement.setTime(10, office.getOpensAt());
      insertStatement.setTime(11, office.getClosesAt());
      insertStatement.executeUpdate();

      int districtOfficeId;
      ResultSet rs = insertStatement.getGeneratedKeys();
      if (rs.next()) {
        districtOfficeId = rs.getInt(1);
      }
      else {
        throw new SQLException("Create of District Office failed, no ID obtained.");
      }

      DistrictOffice newOffice = retrieveById(conn, districtOfficeId);
      URI location = uriInfo.getAbsolutePathBuilder().path(Integer.toString(
          newOffice.getDistrictOfficeId())).build();
      return Response.created(location).entity(newOffice).build();
    }
    finally {
      conn.close();
    }
  }


  @PUT
  @Path("{officeId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateOffice(
      @PathParam("officeId") int officeId,
      DistrictOffice office)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement update = conn.prepareStatement(SQL_UPDATE_DISTRICT_OFFICE);
      update.setInt(1, office.getDistrictId());
      update.setString(2, office.getAddress().getAddressLine1());
      update.setString(3, office.getAddress().getAddressLine2());
      update.setString(4, office.getAddress().getCity());
      update.setString(5, office.getAddress().getState());
      update.setString(6, office.getAddress().getCountry());
      update.setString(7, office.getAddress().getZipCode());
      update.setString(8, office.getPhone());
      update.setString(9, office.getEmail());
      update.setTime(10, office.getOpensAt());
      update.setTime(11, office.getClosesAt());
      update.setInt(12, officeId);
      update.executeUpdate();

      return Response.ok(retrieveById(conn, officeId)).build();
    }
    finally {
      conn.close();
    }
  }


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getOffices() throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      List<DistrictOffice> offices = new ArrayList<>();
      ResultSet rs = conn.createStatement().executeQuery(SQL_SELECT_DISTRICT_OFFICE);
      while (rs.next()) {
        offices.add(new DistrictOffice(rs));
      }
      return Response.ok(offices).build();
    }
    finally {
      conn.close();
    }
  }


  @GET
  @Path("{officeId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getById(@PathParam("officeId") int officeId)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      return Response.ok(retrieveById(conn, officeId)).build();
    }
    finally {
      conn.close();
    }
  }


  @DELETE
  @Path("{officeId}")
  public Response deleteOffice(@PathParam("officeId") int officeId)
      throws SQLException {

    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      PreparedStatement delete = conn.prepareStatement(SQL_DELETE_DISTRICT_OFFICE);
      delete.setInt(1, officeId);
      delete.executeUpdate();
      return Response.noContent().build();
    }
    finally {
      conn.close();
    }
  }


  private DistrictOffice retrieveById(
      Connection conn,
      int districtOfficeId)
      throws SQLException {

    String whereClause = " WHERE " + DistrictOffice.DISTRICT_OFFICE_ID + " = ?";
    PreparedStatement statement = conn.prepareStatement(
        SQL_SELECT_DISTRICT_OFFICE + whereClause);
    statement.setInt(1, districtOfficeId);
    ResultSet rs = statement.executeQuery();
    if (!rs.next()) {
      throw new NotFoundException("No district office found with ID '"
          + districtOfficeId + "'");
    }
    return new DistrictOffice(rs);
  }


}
