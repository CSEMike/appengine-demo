package com.google.appengine.demo;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

@SuppressWarnings("serial")
public class UploadDownloadServlet extends HttpServlet {
	public byte[] readAll(InputStream input) throws IOException {
		/// Create a buffer to read at most 2k of data at a time.
	    byte[] out = new byte[2048];
	    int read;
	    ByteArrayOutputStream output = new ByteArrayOutputStream();
	    // Accumulate data into output until input.read() returns -1, indicating
	    // that the end of the stream has been reached.
	    while ((read = input.read(out)) != -1) {
	        output.write(out, 0, read);
	    }
	    // Return a raw byte array containing the result.
	    return output.toByteArray();
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		// Fully read the POST data.
		byte [] image = readAll(req.getInputStream());
		
		if (image.length > 1024*50) {
			throw new IOException("Image too large!");
		}
		
		// Create an entity to store the image.
		Entity entity = new Entity(KeyFactory.createKey("images", "shared-image"));
		// Many key-value pairs can be added here, but we only need the 
		// image bytes. We wrap this in a Text object since App Engine
		// has a limit on String storage.
		entity.setProperty("content", new Text(new String(image)));
		
		// Put the entity in the data store.
		DatastoreService datastore = 
				DatastoreServiceFactory.getDatastoreService();
		datastore.put(entity);
		
		// Notify the client of success.
		resp.setContentType("text/plain");
		resp.getWriter().println("Accepted POST of " + image.length);
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		// Read the image data from the datastore.
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Key imageKey = KeyFactory.createKey("images", "shared-image");
		String imageString = "";
		try {
			Entity entity = datastore.get(imageKey);
			Text textProperty = (Text)entity.getProperty("content");
			imageString = textProperty.getValue();
		} catch (EntityNotFoundException e) {
			// Swallow this exception and write nothing -- there's no shared
			// image.
		}
		resp.setContentType("text/plain");
		resp.getWriter().println(imageString);
	}
}
