package com.google.appengine.demo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.labs.repackaged.com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@SuppressWarnings("serial")
public class RealtimeServlet extends HttpServlet {
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
    ChannelService channelService = ChannelServiceFactory.getChannelService();
	  
		// Get the user ID making this request.
	  UserService userService = UserServiceFactory.getUserService();
	  String currentUserId = userService.getCurrentUser().getUserId();
		
	  // Read the POST data fully.
		String post = new String(UploadDownloadServlet.readAll(
				req.getInputStream()));
		
		// Parse JSON.
		HashMap<String, String> message = new Gson().fromJson(
				post, new TypeToken<Map<String, String>>() {}.getType());
		
		// If this is a request for a new channel, we need to create
		// a new record for this channel.
		if (message.get("message").equals("newchannel")) {
		  
			// Create a new channel.
			Key surfaceKey = KeyFactory.createKey("s", currentUserId);
			String keyString = KeyFactory.keyToString(surfaceKey);
			String clientId = Double.toString(Math.random());
			String token = channelService.createChannel(clientId);
			
			// Write it to the datastore for this user.
			DatastoreService datastore = 
					DatastoreServiceFactory.getDatastoreService();
			Entity entity = new Entity(surfaceKey);
			entity.setProperty("clientId1", clientId);
			entity.setProperty("token1", token);
			datastore.put(entity);
			
			// Encode the key and channel id as JSON and send to the client.
			Map<String, String> responseMap = Maps.newHashMap();
			responseMap.put("token", token);
			responseMap.put("key", keyString);			
			resp.getWriter().println(new Gson().toJson(responseMap));
			
		} else if (message.get("message").equals("join")) {
		  
			String keyString = message.get("key");
			String clientId = Double.toString(Math.random());
			String token = channelService.createChannel(clientId);
			
			// Associate the second channel with the surface key.
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Key surfaceKey = KeyFactory.stringToKey(keyString);
			try {
				Entity entity = datastore.get(surfaceKey);
	      entity.setProperty("token2", token);
				entity.setProperty("clientId2", clientId);
				datastore.put(entity);
			} catch (EntityNotFoundException e) {
				throw new IOException(e);
			}
			
			// Return the client's newly created channel.
      Map<String, String> responseMap = Maps.newHashMap();
      responseMap.put("token", token);
      resp.getWriter().println(new Gson().toJson(responseMap));
      
		} else if (message.get("message").equals("draw")) {
		  
		  // Load the other participant, if any, associated with this
		  // surface and relay the stroke.
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Key surfaceKey = KeyFactory.stringToKey(message.get("key"));
      try {
        Entity entity = datastore.get(surfaceKey);
        String c1 = (String) entity.getProperty("clientId1");
        String c2 = (String) entity.getProperty("clientId2");
        
        if (c2 != null) {
          String outChannel;
          // Whichever client sent us this message, relay it to the other one.
          if (entity.getProperty("token1").equals(message.get("token"))) {
            outChannel = c2;
          } else {
            outChannel = c1;
          }
          
          // This was already JSON-encoded by the first client, so we
          // can send it directly.
          channelService.sendMessage(
              new ChannelMessage(outChannel, message.get("stroke")));
        } else {
          // There's only one participant, no need to forward.
        }
      } catch (EntityNotFoundException e) {
        throw new IOException(e);
      }
		}
	}
}
