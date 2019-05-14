package com.ccl.grandcanyon.deliverymethod;

import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.District;

import java.util.Properties;

public interface DeliveryService {

  void init(Properties configuration);

  boolean sendRegularCallInReminder(Caller caller, District district, String trackingId) throws Exception;
}
