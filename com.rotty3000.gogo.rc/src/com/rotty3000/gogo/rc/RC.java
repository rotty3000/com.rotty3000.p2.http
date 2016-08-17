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

package com.rotty3000.gogo.rc;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(
	property = {
		"osgi.command.scope=rc",
		"osgi.command.function=rc"
	},
	service = Object.class
)
public class RC {

	@Activate
	public void activate(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public void rc(long[] bundleIds) {
		if (bundleIds.length == 0) {
			System.out.println("No bundle ids provided!");

			return;
		}

		for (long bundleId : bundleIds) {
			processBundle(bundleId);
		}
	}

	private void processBundle(long bundleId) {
		Bundle bundle = bundleContext.getBundle(bundleId);

		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);

		System.out.println(bundle);

		System.out.println("  Capabilities:");
		List<BundleWire> providedWires = bundleWiring.getProvidedWires(null);

		for (Capability capability : bundleWiring.getCapabilities(null)) {
			System.out.println("    " + capability.toString());

			boolean found = false;
			for (BundleWire wire : providedWires) {
				if (capability.equals(wire.getCapability())) {
					found = true;
					System.out.println("     -> " + wire.getRequirer().getBundle());
					break;
				}
			}
			if (!found) {
				System.out.println("      -> no requirers");
			}
		}

		System.out.println("  Requirements:");
		List<BundleWire> requiredWires = bundleWiring.getRequiredWires(null);

		for (Requirement requirement : bundleWiring.getRequirements(null)) {
			System.out.println("    " + requirement.toString());

			boolean found = false;
			for (BundleWire wire : requiredWires) {
				if (requirement.equals(wire.getRequirement())) {
					found = true;
					System.out.println("      <- " + wire.getProvider().getBundle());
				}
			}
			if (!found) {
				System.out.println("      <- no providers");
			}
		}
	}

	private BundleContext bundleContext;

}