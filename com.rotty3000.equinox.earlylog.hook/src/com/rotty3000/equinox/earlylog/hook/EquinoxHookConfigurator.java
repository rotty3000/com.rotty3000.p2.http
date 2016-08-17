/**
 * Copyright 2016 Raymond Augé
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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