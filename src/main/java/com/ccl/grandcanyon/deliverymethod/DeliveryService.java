package com.ccl.grandcanyon.deliverymethod;

import com.ccl.grandcanyon.types.Caller;

import java.util.Properties;

public interface DeliveryService {

  void init(Properties configuration);

  boolean send(Caller caller, String trackingId) throws Exception;
}
