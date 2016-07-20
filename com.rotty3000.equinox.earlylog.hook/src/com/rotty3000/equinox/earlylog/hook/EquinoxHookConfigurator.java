package com.rotty3000.equinox.earlylog.hook;

import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.osgi.internal.hookregistry.ActivatorHookFactory;
import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class EquinoxHookConfigurator
	implements ActivatorHookFactory, BundleActivator, HookConfigurator {

	public EquinoxHookConfigurator() {
		_equinoxSynchronousLogListener = new EquinoxSynchronousLogListener();
	}

	@Override
	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addActivatorHookFactory(this);
	}

	@Override
	public BundleActivator createActivator() {
		return this;
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		ServiceReference<ExtendedLogReaderService> serviceReference =
			bundleContext.getServiceReference(ExtendedLogReaderService.class);

		if (serviceReference != null) {
			ExtendedLogReaderService extendedLogReaderService =
				bundleContext.getService(serviceReference);

			extendedLogReaderService.addLogListener(
				_equinoxSynchronousLogListener);
		}
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		ServiceReference<ExtendedLogReaderService> serviceReference =
			bundleContext.getServiceReference(ExtendedLogReaderService.class);

		ExtendedLogReaderService extendedLogReaderService =
			bundleContext.getService(serviceReference);

		extendedLogReaderService.removeLogListener(
			_equinoxSynchronousLogListener);
	}

	private final EquinoxSynchronousLogListener _equinoxSynchronousLogListener;

}