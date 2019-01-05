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

    Response.ResponseBuilder builder;
    if (ex instanceof WebApplicationException) {
      builder = Response.fromResponse(((WebApplicationException)ex).getResponse());
    }
    else if (ex instanceof SQLNonTransientException) {
      builder = Response.status(Response.Status.BAD_REQUEST);
    }
    else {
      builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
    }

    // add JSON response body
    String json;
    try {
      json = mapper.writeValueAsString(new ErrorInfo(ex.getMessage()));
    }
    catch (JsonProcessingException e) {
      json = "{\"message\":\"Unexpected error formatting exception: " + e.getMessage() + "\"}";
    }
    return builder.entity(json).type(MediaType.APPLICATION_JSON_TYPE).build();
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

