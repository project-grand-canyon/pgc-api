package com.ccl.grandcanyon;

/**
 * Authorization / Authentication constants.
 */
public abstract class GCAuth
{
  public static final String CURRENT_PRINCIPAL = "CurrentPrincipal";

  // role for operations that can be invoked only by Admins
  public static final String ADMIN_ROLE = "Admin";

  // role for operations that can be invoked only by root Admins
  public static final String SUPER_ADMIN_ROLE = "SuperAdmin";

  // "role" for operations that can be invoked without authentication
  public static final String ANONYMOUS = "Anonymous";

}
