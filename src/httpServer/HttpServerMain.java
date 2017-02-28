package httpServer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Project AmazingServer HTTP server starter class
 */
public class HttpServerMain {
	// Fields
	private static final int me_PORT= 8888;
	private static final String me_DIRECTORY = "www/";
	protected static final int BUFSIZE = 512; // Buffer size
	protected static final boolean SHOW_REQ_RES = true; // To show the request and response texts on the console
	private static boolean meIsPause; // Is under maintenance flag
	private ServerSocket meSock; // This server's socket

	public static void main(String[] args) {
//		// Next three lines are just to get the running file name (whether it's the '.class' or '.jar' file)
//		String curAppName = new File(HttpServerMain.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
//		if (!curAppName.toLowerCase().endsWith(".jar"))
//			curAppName = HttpServerMain.class.getName();
//		if (args.length < 1 || args.length > 4) // Check the arguments and print accordingly
//			printErrWarning("Usage: " + curAppName + " server_name port [messages-per-second] [buffer-size]\n", true);

		try {
			Path tmpRoot = Paths.get(me_DIRECTORY);
			//File tmpRoot = new File(me_DIRECTORY);
			if (!Files.exists(tmpRoot)) {
				Files.createDirectory(tmpRoot);
			} else if (Files.isRegularFile(tmpRoot)) {
				printErrWarning("A file named '" + me_DIRECTORY.substring(0, me_DIRECTORY.length() - 1) +
						"' exists where a folder with the same name should be. Please delete it.", true);
			}
			if (!Files.exists(Paths.get(tmpRoot.toString(), "put")))
				Files.createDirectory(Paths.get(tmpRoot.toString(), "put"));
			if (!Files.exists(Paths.get(tmpRoot.toString(), "post")))
				Files.createDirectory(Paths.get(tmpRoot.toString(), "post"));
		} catch (SecurityException | IOException e) {
			printErrWarning("Security restrictions prevent accessing or creating the HTTP server's root directory structure.", true);
		}


		HttpServerMain tmpSrv = new HttpServerMain();
		Thread tmpTh = new Thread(tmpSrv::waitPauseQuit); // Just a thread to quit the server by entering 'q' in the console
		//MyRequest req = new MyRequest(TEST_GET);

		System.out.println("HTTP server. Enter 'p' to toggle server pause (maintenance), or 'q' to quit..");
		tmpTh.start();
		tmpSrv.startListen(); // Start listening to connections
	}

	public HttpServerMain() {
		try {
			this.meSock = new ServerSocket(me_PORT);
		} catch (IOException e) {
			printErrWarning("Cannot open the socket. Socket could be reserved.", true);
		} catch (SecurityException e) {
			printErrWarning("Security violation does not allow opening the socket.", true);
		} catch (IllegalArgumentException e) {
			printErrWarning("Invalid port specified.", true);
		}
	}

	/**
	 * A method to make server start listening to client connections, and assigns a new thread for every connection.
	 */
	public void startListen() {
		while (!this.meSock.isClosed()) {
			Socket tmpSock = null;
			try {
				tmpSock = this.meSock.accept();
			} catch (SocketTimeoutException e) {
				printErrWarning("Connection with client timed out.", false);
			} catch (IOException e) {
				//printErrWarning("Wait for a client connection was interrupted.", false);
				System.out.println("Canceling the wait for connections. Exiting..."); // Normal exiting
			} catch (SecurityException e) {
				printErrWarning("Security violation does not allow accepting this new client.", false);
			}
			new Thread(new ClientConnection(tmpSock)).start(); // Kick-start new thread on every accepted socket
		}
	}

	// A private method to close the server by entering 'q'.
	private void waitPauseQuit() {
		try {
			int tmpIn = 0;
			while (tmpIn != 'q') {
				tmpIn = System.in.read();
				if (tmpIn == 'p') {
					meIsPause = !meIsPause;
					printErrWarning("System is " + (meIsPause ? "paused for maintenance." : "resuming from maintenance pause."), false);
				}
			}
			if (this.meSock != null && !this.meSock.isClosed())
				this.meSock.close();
			System.exit(0); // Exit normally
		} catch (IOException e) {/* Can be safely ignored */}
	}

	/**
	 * Checks if server is under maintenance (paused).
	 * @return	true if server is under maintenance
	 */
	protected static boolean getIsPause() {
		return meIsPause;
	}

	/**
	 * A method that returns the absolute path (as URL) of a relative URL
	 * @param relativeUrlPath
	 * @return
	 */
	protected static String getAbsoluteUrlPath(String relativeUrlPath) {
		String outURL = null;
		try {
			outURL = new File(me_DIRECTORY + relativeUrlPath).getCanonicalFile().toURI().getPath();
		} catch (IOException e) {}
		return outURL;
	}

	/**
	 * A method to print an error message to the error output stream and terminate execution if required.
	 * @param errWarningMsg			the error or warning message
	 * @param terminateExecution	true to terminate execution after displaying the message
	 */
	protected static void printErrWarning(String errWarningMsg, boolean terminateExecution) {
		// Here, I thought it's better to keep the error handling the same as in the provided
		// files (no exception throwing) to avoid confusion with accidental exceptions that
		// might happen when correcting.
		System.err.println((terminateExecution ? "Error:\n" : "Warning:\n") + errWarningMsg);
		if (terminateExecution)
			System.exit(1);
	}
}
