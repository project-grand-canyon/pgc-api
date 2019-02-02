package com.ccl.grandcanyon;

import javax.ws.rs.container.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;


@Provider
@PreMatching
public class CORSFilter implements ContainerRequestFilter, ContainerResponseFilter {

    /**
     * Method for ContainerRequestFilter.
     */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {

        // If it's a preflight request, we abort the request with
        // a 200 status, and the CORS headers are added in the
        // response filter method below.
        Boolean isPreflight = isPreflightRequest(request);
        if (isPreflight) {
            request.abortWith(Response.ok().build());
            return;
        }
    }

    /**
     * A preflight request is an OPTIONS request.
     */
    private static boolean isPreflightRequest(ContainerRequestContext request) {
        Boolean isPreflight = request.getHeaderString("Origin") != null &&
                request.getMethod().equalsIgnoreCase("OPTIONS");
        return isPreflight;
    }

    /**
     * Method for ContainerResponseFilter.
     */
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response)
            throws IOException {

        // if there is no Origin header, then it is not a
        // cross origin request. We don't do anything.
        if (request.getHeaderString("Origin") == null) {
            return;
        }

        // If it is a preflight request, then we add all
        // the CORS headers here.
        Boolean isPreflight = isPreflightRequest(request);
        if (isPreflight) {
            response.getHeaders().add("Access-Control-Allow-Credentials", "true");
            response.getHeaders().add("Access-Control-Allow-Methods",
                    "GET, POST, PUT, DELETE, OPTIONS, HEAD");
            response.getHeaders().add("Access-Control-Allow-Headers",
                    "content-type, authorization");
        }

        // Cross origin requests can be either simple requests
        // or preflight request. We need to add this header
        // to both type of requests. Only preflight requests
        // need the previously added headers.
        response.getHeaders().add("Access-Control-Allow-Origin", "*");
    }
}