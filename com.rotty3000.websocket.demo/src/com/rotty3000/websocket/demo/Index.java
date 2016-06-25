package com.rotty3000.websocket.demo;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import aQute.bnd.annotation.headers.RequireCapability;

@Component(
	immediate = true,
	property = {
		HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN + "= ",
		HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN + "=/",
		HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN + "=/index.html",
		HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX + "=/META-INF/resources/index.html",
		"service.ranking:Integer=100"
	},
	service = Object.class
)
@RequireCapability(
	filter = "(&(osgi.implementation=osgi.http)(version>=1.0.0)(!(version>=2.0.0)))",
	ns = "osgi.implementation"
)
public class Index {
}