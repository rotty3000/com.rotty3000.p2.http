package com.rotty3000.wake;

import org.eclipse.equinox.http.jetty.JettyCustomizer;
import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public static final Class<?> CLASS1 = JettyCustomizer.class;
	public static final Class<?> CLASS2 = ExtendedHttpService.class;

	@Override
	public void start(BundleContext context) throws Exception {
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}