package com.ccl.grandcanyon;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLNonTransientException;

public class SQLExceptionMapper implements ExceptionMapper<SQLException> {

  @Override
  public Response toResponse(SQLException exception) {

    Response.Status status = exception instanceof SQLNonTransientException ?
        Response.Status.BAD_REQUEST :
        Response.Status.INTERNAL_SERVER_ERROR;
    return Response.status(status.getStatusCode(), exception.getMessage()).build();
  }
}
