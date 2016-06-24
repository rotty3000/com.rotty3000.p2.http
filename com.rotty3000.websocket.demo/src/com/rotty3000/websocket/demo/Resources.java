package com.rotty3000.websocket.demo;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@Component(
	immediate = true,
	property = {
		HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN + "=/res/*",
		HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX + "=/META-INF/resources"
	},
	service = Object.class
)
public class Resources {
}