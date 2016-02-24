package py4j.examples;

import py4j.ClientServer;
import py4j.GatewayServer;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by barthelemy on 2016-02-11.
 */
public class SingleThreadClientApplication {

	public static void main(String[] args) {
		GatewayServer.turnAllLoggingOn();
		Logger logger = Logger.getLogger("py4j");
		logger.setLevel(Level.ALL);
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINEST);
		logger.addHandler(handler);
		System.out.println("Starting");
		ClientServer clientServer = new ClientServer(null);
		IHello hello = (IHello) clientServer.getPythonServerEntryPoint
				(new Class[] {IHello.class});
		try {
			hello.sayHello();
			hello.sayHello(2, "Hello World");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// This is to test connecting again if there was no successful
		// initial connection.
//		try {
//			Thread.currentThread().sleep(5000);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		try {
//			hello.sayHello();
//			hello.sayHello(2, "Hello World");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		// This is to manually test reconnecting to the Python side after
		// graceful shutdown.
//		clientServer.shutdown();
//
//		try {
//			Thread.currentThread().sleep(1000);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		clientServer = new ClientServer(null);
//		hello = (IHello) clientServer.getPythonServerEntryPoint
//				(new Class[] {IHello.class});
//		hello.sayHello();
//		hello.sayHello(2, "Hello World");
	}
}