package com.ccl.grandcanyon;

import com.ccl.grandcanyon.auth.AuthFilter;
import com.ccl.grandcanyon.auth.AuthResponseFilter;
import io.sentry.Sentry;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;



@ApplicationPath("api")
public class GCApplication extends ResourceConfig {

  public GCApplication() {

    Sentry.init(); // Exception logging for monitoring the api

    register(CORSFilter.class);
    register(AuthFilter.class);
    register(AuthResponseFilter.class);

    register(Callers.class);
    register(Districts.class);
    register(Themes.class);
    register(DistrictOffices.class);
    register(Calls.class);
    register(Admins.class);
    register(Reminders.class);
    register(Requests.class);
    register(Login.class);
    register(Stats.class);

    register(JacksonJsonProvider.class);
    register(JSONExceptionMapper.class);
  }

}
