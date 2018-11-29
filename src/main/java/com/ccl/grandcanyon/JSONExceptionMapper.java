package com.ccl.grandcanyon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.sql.SQLNonTransientException;

/**
 * Return exceptions to caller in JSON format.
 */
public class JSONExceptionMapper implements ExceptionMapper<Exception> {

  private static ObjectMapper mapper = new ObjectMapper();

  @Override
  public Response toResponse(Exception ex) {

    int status;
    if (ex instanceof WebApplicationException) {
      status = ((WebApplicationException)ex).getResponse().getStatus();
    }
    else if (ex instanceof SQLNonTransientException) {
      status = Response.Status.BAD_REQUEST.getStatusCode();
    }
    else {
      status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }
    String json;
    try {
      json = mapper.writeValueAsString(new ErrorInfo(ex.getMessage()));
    }
    catch (JsonProcessingException e) {
      json = "{\"message\":\"Unexpected error formatting exception: " + e.getMessage() + "\"}";
    }
    return Response.status(status).entity(json).type(MediaType.APPLICATION_JSON_TYPE).build();
  }

  class ErrorInfo {
    String message;

    ErrorInfo(String msg) {
      message = msg;
    }

    public String getMessage() {
      return message;
    }
  }
}

