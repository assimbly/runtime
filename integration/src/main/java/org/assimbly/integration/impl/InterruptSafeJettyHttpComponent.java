package org.assimbly.integration.impl;

import org.apache.camel.component.jetty12.JettyHttpComponent12;
import org.apache.camel.http.common.HttpConsumer;

/**
 * Workaround for CAMEL-26554 (fix expected in Camel 4.24.0): remove this class once runtime uses a Camel version with the fix.
 * <p>
 * When a flow stop times out, Camel's cancelled shutdown task stops the consumers on an interrupted thread.
 * Jetty fails to stop on such a thread, and camel-jetty then keeps the closed server registered for its port,
 * so every later flow on that port starts but accepts no connections.
 */
public class InterruptSafeJettyHttpComponent extends JettyHttpComponent12 {

	@Override
	public void disconnect(HttpConsumer consumer) throws Exception {
		// clear the interrupt flag while Jetty stops, and restore it for the rest of the shutdown
		boolean interrupted = Thread.interrupted();
		try {
			super.disconnect(consumer);
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
