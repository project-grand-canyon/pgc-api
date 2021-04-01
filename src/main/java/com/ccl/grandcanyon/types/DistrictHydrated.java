package com.ccl.grandcanyon.types;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class DistrictHydrated extends District {

  private List<DistrictOffice> offices;
  private List<Request> requests;

  public DistrictHydrated(ResultSet rs) throws SQLException {

    super(rs);
  }

  public List<DistrictOffice> getOffices() {
    return offices;
  }

  public void setOffices(List<DistrictOffice> offices) {
    this.offices = offices;
  }

  public List<Request> getRequests() {
    return requests;
  }

  public void setRequests(List<Request> requests) {
    this.requests = requests;
  }
}
