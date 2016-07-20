package com.rotty3000.wake;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.util.tracker.BundleTracker;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		bt = new BundleTracker<Bundle>(context, Bundle.STARTING, null) {

			@Override
			public Bundle addingBundle(final Bundle bundle, BundleEvent event) {
				BundleStartLevel startLevel = bundle.adapt(BundleStartLevel.class);

				if (startLevel.isActivationPolicyUsed()) {
					try {
						bundle.start();
					}
					catch (BundleException e) {
						//
					}
				}

				return bundle;
			}

		};

		bt.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		bt.close();
	}

	private BundleTracker<Bundle> bt;

}