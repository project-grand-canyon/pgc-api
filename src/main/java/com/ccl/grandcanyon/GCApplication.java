package com.ccl.grandcanyon;

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;



@ApplicationPath("api")
public class GCApplication extends ResourceConfig {

  public GCApplication() {

    register(Callers.class);
    register(JacksonJsonProvider.class);
    register(SQLExceptionMapper.class);
  }

}
