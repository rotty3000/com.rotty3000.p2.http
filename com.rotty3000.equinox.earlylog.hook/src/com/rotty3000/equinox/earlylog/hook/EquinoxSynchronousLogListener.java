package com.rotty3000.equinox.earlylog.hook;

import org.eclipse.equinox.log.ExtendedLogEntry;
import org.eclipse.equinox.log.SynchronousLogListener;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;

import org.osgi.framework.Bundle;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;

public class EquinoxSynchronousLogListener implements SynchronousLogListener {

	@Override
	public void logged(LogEntry logEntry) {
		if (!(logEntry instanceof ExtendedLogEntry)) {
			return;
		}

		ExtendedLogEntry extendedLogEntry = (ExtendedLogEntry)logEntry;

		Object context = extendedLogEntry.getContext();

		if (context instanceof FrameworkLogEntry) {
			FrameworkLogEntry frameworkLogEntry = (FrameworkLogEntry)context;

			_log(
				frameworkLogEntry.getEntry(), frameworkLogEntry.getSeverity(),
				frameworkLogEntry.getMessage(), frameworkLogEntry.getContext(),
				frameworkLogEntry.getThrowable());

			FrameworkLogEntry[] childFrameworkLogEntries =
				frameworkLogEntry.getChildren();

			if ((childFrameworkLogEntries != null) &&
				(childFrameworkLogEntries.length > 0)) {

				for (FrameworkLogEntry childFrameworkLogEntry :
						childFrameworkLogEntries) {

					_log(
						childFrameworkLogEntry.getEntry(),
						childFrameworkLogEntry.getSeverity(),
						childFrameworkLogEntry.getMessage(),
						childFrameworkLogEntry.getContext(),
						childFrameworkLogEntry.getThrowable());
				}
			}

			return;
		}

		Bundle bundle = extendedLogEntry.getBundle();

		_log(
			bundle.getSymbolicName(), extendedLogEntry.getLevel(),
			extendedLogEntry.getMessage(), context,
			extendedLogEntry.getException());
	}

	private synchronized void _log(
		String category, int level, String message, Object context,
		Throwable throwable) {

		System.out.printf("[%s] %s %s %s\n", category, message, context, throwable);
	}

}