package com.ccl.grandcanyon.deliverymethod;

import com.ccl.grandcanyon.types.Admin;
import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.Message;

import java.util.Properties;

public interface DeliveryService {

  void init(Properties configuration);

  boolean sendTextMessage(Admin admin, Message message) throws Exception;

  boolean sendTextMessage(Caller caller, Message message) throws Exception;

  boolean sendHtmlMessage(Caller caller, Message message) throws Exception;
}
