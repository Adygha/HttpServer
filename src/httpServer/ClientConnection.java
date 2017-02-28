package httpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * A runnable that takes an accepted connection socket to take over the communication with client
 */
public class ClientConnection implements Runnable {
	// Fields
	private Socket meAccSock;

	public ClientConnection(Socket acceptSocket) {
		if (acceptSocket != null)
			this.meAccSock = acceptSocket;
	}

	@Override
	public void run() {
		if (this.meAccSock == null)
			return;
		System.out.println("Client connection accepted..");
		MyRequest req;
		MyResponse resp;

		try { // A try-catch just to anticipate '500 Internal Server Error'
			if (HttpServerMain.getIsPause()) {
				req = new MyRequest("PAUSE / HTTP/1.1"); // Imaginary request
				resp = new MyResponse(req);
				this.sendResponse(resp.getResponseBytes());
			} else {
				req = this.receiveRequest();
				resp = new MyResponse(req);
				this.sendResponse(resp.getResponseBytes());
			}
		} catch (Exception e) {
			req = new MyRequest("ERROR / HTTP/1.1"); // Imaginary request
			resp = new MyResponse(req);
			this.sendResponse(resp.getResponseBytes());
		}
		if (this.meAccSock != null && !this.meAccSock.isClosed()) {
			try {
				this.meAccSock.close();
			} catch (IOException e) {}
		}
		if (HttpServerMain.SHOW_REQ_RES) {
			System.out.println("----------------- Start Response String -----------------");	//
			System.out.println(new String(resp.getResponseBytes()));							// Printout the request string if required
			System.out.println("-----------------  End Response String  -----------------");	//
		}

		System.out.println("Client connection closed..");
	}

	private MyRequest receiveRequest() {
		if (this.meAccSock == null)
			return null;
		byte tmpBuf[] = null;
		int tmpNum = 0; // To get the number of bytes read
		ByteArrayOutputStream tmpMsg = new ByteArrayOutputStream(HttpServerMain.BUFSIZE); // To push the received buffer in if message is big.
		try {
			do { // A loop to receive
				tmpBuf = new byte[HttpServerMain.BUFSIZE];
				tmpNum = this.meAccSock.getInputStream().read(tmpBuf); // The actual receiving
				if (tmpNum > 0)
					tmpMsg.write(tmpBuf, 0, tmpNum); // Accumulate message
			//} while (!tmpMsg.toString().endsWith("\r\n\r\n") && tmpNum > -1); // In case of HTTP request end-chars or received '-1' (EOF) then the other side has closed the connection (according to: http://stackoverflow.com/questions/10240694/ )
			} while (!this.checkReceiveEnded(tmpMsg) && tmpNum > -1); // In case of HTTP request end-chars or received '-1' (EOF) then the other side has closed the connection (according to: http://stackoverflow.com/questions/10240694/ )
		} catch (IOException e) {
			HttpServerMain.printErrWarning("Connection timed out or terminated.", false);
			if (this.meAccSock != null && !this.meAccSock.isClosed()) {
				try {
					this.meAccSock.close();
				} catch (IOException e1) {/* Can be safely ignored */}
			}
		}
		if (HttpServerMain.SHOW_REQ_RES) {
			System.out.println("----------------- Start Request String -----------------");	//
			System.out.println(tmpMsg.toString());											// Printout the request string if required
			System.out.println("-----------------  End Request String  -----------------");	//
		}
		return new MyRequest(tmpMsg.toString());
	}

	private void sendResponse(byte[] respBytes) {
		try {
			//this.meAccSock.getOutputStream().write(HttpServerMain.TEST_200.getBytes());
			this.meAccSock.getOutputStream().write(respBytes);
		} catch (IOException e) {
			HttpServerMain.printErrWarning("Connection timed out or terminated.", false);
			if (this.meAccSock != null && !this.meAccSock.isClosed()) {
				try {
					this.meAccSock.close();
				} catch (IOException e1) {/* Can be safely ignored */}
			}
		}
	}

	// Checks if the received data is empty (it fails when uploading files big enough to be multi-part but this
	// assignment supposed to use small files)
	private boolean checkReceiveEnded(ByteArrayOutputStream byteStream) {
		String tmpStr = byteStream.toString();
		if (!tmpStr.contains("\r\n\r\n")) // If does not contain double CRLF then there is still data (but not the opposite)
			return false;
		if (tmpStr.contains("Content-Length:")) { // If there is a content-length then check if all content is there
			try {
				tmpStr = tmpStr.substring(tmpStr.indexOf("Content-Length:") + 15); // Cut the unnecessary part (15 is the length of "Content-Length:")
				int tmpLen = Integer.parseInt(tmpStr.substring(0, tmpStr.indexOf('\n')).trim()); // Get content-length
				tmpStr = tmpStr.substring(tmpStr.indexOf("\r\n\r\n") + 4); // Get only the content
				if (tmpStr.length() < tmpLen)
					return false; // In case not all content received
			} catch (IndexOutOfBoundsException | NumberFormatException e) { // These 2 exceptions will happen if not everything is received
				return false;
			}
		}
		return true;
	}
}
