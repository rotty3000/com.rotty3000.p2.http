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

package com.rotty3000.equinox.runtime.augments;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.eclipse.osgi.container.ModuleRevisionBuilder.GenericInfo;
import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.internal.hookregistry.StorageHookFactory;
import org.eclipse.osgi.internal.hookregistry.StorageHookFactory.StorageHook;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.lib.hex.Hex;

@SuppressWarnings("restriction")
public class AugmentationHookFactory
    extends StorageHookFactory<Object, Object, AugmentationHookFactory.AugmentationHook>
    implements HookConfigurator {

    public class AugmentationHook extends StorageHook<Object, Object> {

        @SuppressWarnings({"rawtypes", "unchecked"})
        public AugmentationHook(Generation generation, Class factoryClass) {
            super(generation, factoryClass);

            if (generation.isDirectory()) {
                return;
            }

            try {
                MessageDigest messageDigest = MessageDigest.getInstance(SHA_256);

                try (DigestInputStream digestInputStream = new DigestInputStream(
                    new FileInputStream(generation.getContent()), messageDigest)) {

                    byte[] bytes = new byte[1024^2];
                    while ((digestInputStream.read(bytes)) != -1);
                }

                _sha256 = Hex.toHexString(messageDigest.digest());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public ModuleRevisionBuilder adaptModuleRevisionBuilder(
            ModuleEvent operation, Module origin,
            ModuleRevisionBuilder originalBuilder) {

            ModuleRevisionBuilder newBuilder = new ModuleRevisionBuilder();

            newBuilder.setSymbolicName(originalBuilder.getSymbolicName());
            newBuilder.setTypes(originalBuilder.getTypes());
            newBuilder.setVersion(originalBuilder.getVersion());

            boolean hasContent = false;

            for (GenericInfo info : originalBuilder.getCapabilities()) {
                Map<String, Object> infoAttributes = info.getAttributes();
                Map<String, String> infoDirectives = info.getDirectives();

                if (info.getNamespace().equals("osgi.content")) {
                	hasContent = true;

                    infoAttributes = new TreeMap<>(info.getAttributes());

                    infoAttributes.put("osgi.content", _sha256);
                }

                for (Iterator<Capability> ci = _rc.capabilities.iterator();
                		ci.hasNext();){

                    Capability capability = ci.next();

                    if (capability.getNamespace().equals(info.getNamespace())) {
                    	ci.remove();

                        infoAttributes = new TreeMap<>(info.getAttributes());
                        infoDirectives = new TreeMap<>(info.getDirectives());

                        infoAttributes.putAll(capability.getAttributes());
                        infoDirectives.putAll(capability.getDirectives());
                    }
                }

                newBuilder.addCapability(
                    info.getNamespace(), infoDirectives, infoAttributes);
            }

            for (Iterator<Capability> ci = _rc.capabilities.iterator();
            		ci.hasNext();){

                Capability capability = ci.next();

                newBuilder.addCapability(
                    capability.getNamespace(), capability.getDirectives(),
                    capability.getAttributes());
            }

            if (!hasContent) {
                newBuilder.addCapability(
					"osgi.content", Collections.emptyMap(),
					Collections.singletonMap("osgi.content", _sha256));
            }

            for (GenericInfo info : originalBuilder.getRequirements()) {
                Map<String, Object> infoAttributes = info.getAttributes();
                Map<String, String> infoDirectives = info.getDirectives();

                for (Iterator<Requirement> ri = _rc.requirements.iterator();
                		ri.hasNext();){

                    Requirement requirement = ri.next();

                    if (requirement.getNamespace().equals(info.getNamespace())) {
                    	ri.remove();

                        infoAttributes = new TreeMap<>(info.getAttributes());
                        infoDirectives = new TreeMap<>(info.getDirectives());

                        infoAttributes.putAll(requirement.getAttributes());
                        infoDirectives.putAll(requirement.getDirectives());
                    }
                }

                newBuilder.addRequirement(
                    info.getNamespace(), infoDirectives, infoAttributes);
            }

            for (Iterator<Requirement> ri = _rc.requirements.iterator();
                    ri.hasNext();){

                Requirement requirement = ri.next();

                newBuilder.addRequirement(
                    requirement.getNamespace(), requirement.getDirectives(),
                    requirement.getAttributes());
            }

            return newBuilder;
        }

        @Override
        public void initialize(Dictionary<String, String> manifest)
            throws BundleException {

            String symbolicName = manifest.get(Constants.BUNDLE_SYMBOLICNAME);

            _rc = _augmentsMap.get(symbolicName);

            if (_rc == null) {
            	_rc = new RCPair();
            }
        }

        @Override
        public void load(Object loadContext, DataInputStream is)
            throws IOException {

            // Do nothing
        }

        @Override
        public void save(Object saveContext, DataOutputStream os)
            throws IOException {

            // Do nothing
        }

        private RCPair _rc;
        private String _sha256;

    }

    public AugmentationHookFactory() throws IOException {
    	Class<?> clazz = getClass();
    	ClassLoader classLoader = clazz.getClassLoader();

    	Enumeration<URL> resources = classLoader.getResources(
    		"META-INF/augments.properties");

        for (URL url : Collections.list(resources)) {
        	loadAugments(url);
        }
    }

    @Override
    public void addHooks(HookRegistry hookRegistry) {
        hookRegistry.addStorageHookFactory(this);
    }

    @Override
    public int getStorageVersion() {
        return 0;
    }

    @Override
    protected AugmentationHook createStorageHook(Generation generation) {
        return new AugmentationHook(generation, AugmentationHookFactory.class);
    }

    private void loadAugments(URL url) throws IOException {
        Properties properties = new Properties();

        try (InputStream inputStream = url.openStream()) {
            properties.load(inputStream);

            processKeys(properties);
        }
    }

    private void processAugmentProperty(String property) {
        Parameters header = new Parameters(property);

        for (String symbolicName : header.keySet()) {
            Attrs attrs = header.get(symbolicName);

            processEntry(symbolicName, attrs);
        }
    }

    private void processCapabilities(RCPair pair, Attrs attrs) {
        String capabilityString = attrs.get("capability:");

        if (capabilityString == null) {
        	return;
        }

        Parameters parameters = new Parameters(capabilityString);

        for (String namespace : parameters.keySet()) {
            CapReqBuilder capReqBuilder = new CapReqBuilder(
                namespace, parameters.get(namespace));

            capReqBuilder.setResource(NULL_RESOURCE);

            pair.capabilities.add(capReqBuilder.buildCapability());
        }
	}

	private void processKeys(Properties properties) {
		Enumeration<Object> keys = properties.keys();

	    for (Object keyObject : Collections.list(keys)) {
	        String key = (String)keyObject;

	        if (!key.equals("-augment") && !key.startsWith("-augment.")) {
	            continue;
	        }

	        processAugmentProperty(properties.getProperty(key));
	    }
	}

	private void processEntry(String symbolicName, Attrs attrs) {
	    RCPair pair = _augmentsMap.get(symbolicName);

	    if (pair == null) {
	        pair = new RCPair();

	        _augmentsMap.put(symbolicName, pair);
	    }

	    processCapabilities(pair, attrs);
	    processRequirements(pair, attrs);
	}

	private void processRequirements(RCPair pair, Attrs attrs) {
	    String requirmentString = attrs.get("requirement:");

	    if (requirmentString == null) {
	    	return;
	    }

	    Parameters parameters = new Parameters(requirmentString);

	    for (String namespace : parameters.keySet()) {
	    	CapReqBuilder capReqBuilder = new CapReqBuilder(
	    			namespace, parameters.get(namespace));

	    	capReqBuilder.setResource(NULL_RESOURCE);

	    	pair.requirements.add(capReqBuilder.buildRequirement());
	    }
	}

	class RCPair {

        List<Capability> capabilities = new ArrayList<>();
        List<Requirement> requirements = new ArrayList<>();

    }

    static class NullResource implements Resource {

        @Override
        public List<Capability> getCapabilities(String namespace) {
            return null;
        }

        @Override
        public List<Requirement> getRequirements(String namespace) {
            return null;
        }

    }

    private static final NullResource NULL_RESOURCE = new NullResource();

    private static final String SHA_256 = "SHA-256";

    private final Map<String, RCPair> _augmentsMap = new HashMap<>();

}
