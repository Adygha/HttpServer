/**
 *
 */
package httpServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import httpServer.MyRequest.PathType;
import httpServer.MyRequest.RequestType;

/**
 * A class that represents an HTTP response.
 */
public class MyResponse {
	// Constants
	// Ready-made base responses
	private static final String me_200_OK_STARTER = "HTTP/1.1 200 OK\r\nServer: AmazingServer\r\n";
	private static final String me_403_FORBIDDEN = "HTTP/1.1 403 Forbidden\r\nServer: AmazingServer\r\nContent-Length: 48\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n<html><body><h1>403 Forbidden</h1></body></html>";
	private static final String me_404_NOT_FOUND = "HTTP/1.1 404 Not Found\r\nServer: AmazingServer\r\nContent-Length: 48\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n<html><body><h1>404 Not found</h1></body></html>";
	private static final String me_500_INTERNAL_SERVER_ERROR = "HTTP/1.1 500 Internal Server Error\r\nServer: AmazingServer\r\nContent-Length: 60\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n<html><body><h1>500 Internal Server Error</h1></body></html>";
	// Ready-made additional responses
	private static final String me_201_CREATED_STARTER = "HTTP/1.1 201 Created\r\nServer: AmazingServer\r\nConnection: close\r\nLocation: ";
	private static final String me_204_NO_CONTENT = "HTTP/1.1 204 No Content\r\nConnection: close\r\n\r\n";
	private static final String me_400_BAD_REQUEST = "HTTP/1.1 400 Bad Request\r\nServer: AmazingServer\r\nContent-Length: 48\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n<html><body><h1>400 Bad request</h1></body></html>";
	private static final String me_405_METHOD_NOT_ALLOWED = "HTTP/1.1 405 Method Not Allowed\r\nServer: AmazingServer\r\nContent-Length: 57\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n<html><body><h1>405 Method Not Allowed</h1></body></html>";
	private static final String me_415_UNSUPPORTED_MEDIA_TYPE = "HTTP/1.1 415 Unsupported Media Type\r\nServer: AmazingServer\r\nContent-Length: 61\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n<html><body><h1>415 Unsupported Media Type</h1></body></html>";
	private static final String me_501_NOT_IMPLEMENTED = "HTTP/1.1 501 Not Implemented\r\nServer: AmazingServer\r\nContent-Length: 54\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n<html><body><h1>501 Not Implemented</h1></body></html>";
	private static final String me_503_SERVICE_UNAVAILABLE = "HTTP/1.1 503 Service Unavailable\r\nServer: AmazingServer\r\nContent-Length: 58\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n<html><body><h1>503 Service Unavailable</h1></body></html>";
	private static final String me_505_HTTP_NOT_SUPPORTED = "HTTP/1.1 505 HTTP Version Not Supported\r\nServer: AmazingServer\r\nContent-Length: 65\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n<html><body><h1>505 HTTP Version Not Supported</h1></body></html>";
	// Partials
	private static final String me_CONTENT_LENGTH = "Content-Length: ";
	private static final String me_HTML_CONTENT = "Content-Type: text/html\r\nConnection: close\r\n\r\n";
	private static final String me_PNG_CONTENT = "Content-Type: image/png\r\nConnection: close\r\n\r\n";
	private static final String me_TXT_CONTENT = "Content-Type: text/plain\r\nConnection: close\r\n\r\n";
	//private static final String me_OTHER_CONTENT = "Content-Type: application/octet-stream\r\nConnection: close\r\n\r\n"; // Best fit recommended (can be downloaded)
	// Fields
	private byte[] meRespBytes; // Will hold the response bytes
	private boolean meIsNewCopy; // To indicate if the new uploaded file should be created as a new copy (in case of POST)

	/**
	 * Constructor
	 * @param theRequest the request object to base the response on.
	 */
	public MyResponse(MyRequest theRequest) {
		switch (theRequest.getRequestType()) {
			case GET: // Here we handle GET request
				switch (theRequest.getPathType()) {
					case FILE:
						this.meRespBytes = this.create200Ok(theRequest);
						break;
					case FORBIDDEN:
					case DIRECTORY: // We will refuse display directory content for now
						this.meRespBytes = this.create403Forbidden();
						break;
					case NOT_EXIST:
						this.meRespBytes = this.create404NotFound();
				}
				break;
			case PUT: // Here we handle PUT request
				switch (theRequest.getPathType()) {
					case FILE: // This will overwrite the existing file
						this.meRespBytes = this.create204NoContent(theRequest);
						break;
					case FORBIDDEN:
					case DIRECTORY: // Writing to a directory can be considered as forbidden
						this.meRespBytes = this.create403Forbidden();
						break;
					case NOT_EXIST: // This is the normal case where a new file is created
						this.meRespBytes = this.create201Created(theRequest);
				}
				break;
			case POST: // Here we handle PUT request
				switch (theRequest.getPathType()){
					case FILE:
					case NOT_EXIST:
						updateFileWithPost(theRequest);
						break;
					case DIRECTORY:
					case FORBIDDEN:
						this.meRespBytes = this.create403Forbidden();
				}
				break;
			case NOT_ALLOWED:
				this.meRespBytes = this.create405NotAllowed();
				break;
			case WRONG_HTTP:
				this.meRespBytes = this.create505WrongHTTPVer();
				break;
			case NOT_IMP: // ....
				this.meRespBytes = this.create501NotImplemented();
				break;
			case ERROR:
				this.meRespBytes = this.create500InternalServerError();
				break;
			case PAUSE:
				this.meRespBytes = this.create503ServiceUnavailable();
				break;
			case BAD:
				this.meRespBytes = this.create400BadRequest();
		}
	}

	/**
	 * POST is only supported at a specified path
	 * @param theRequest
	 */
	private void updateFileWithPost(MyRequest theRequest) {
		if (theRequest.getRelativePath().startsWith("/post/") && theRequest.getPathType() == PathType.NOT_EXIST) {
			this.meRespBytes = this.create201Created(theRequest);
		} else if (theRequest.getRelativePath().startsWith("/post/") && theRequest.getPathType() == PathType.FILE) {
			if (theRequest.getAbsolutePath().endsWith("/post/post-test.txt")) { // POST to text file in this case
				this.meRespBytes = this.create204NoContent(theRequest);
			} else { // Otherwise, it is an upload POST (we won't append or overwrite the file, we'll create a new copy)
				this.meIsNewCopy = true;
				this.meRespBytes = this.create201Created(theRequest);
				this.meIsNewCopy = false;
			}
		} else {
			this.meRespBytes = this.create403Forbidden();
		}
	}

	/**
	 * Returns the response represented in bytes
	 * @return	the response's bytes
	 */
	public byte[] getResponseBytes() {
		return this.meRespBytes;
	}


	// vvvvvvvvvvvvvvvvvvvv Start Private Section vvvvvvvvvvvvvvvvvvvv //
	// Contains methods that creates resposes as bytes

	private byte[] create200Ok(MyRequest theRequest) { // OK Essential
		ByteArrayOutputStream outResp = new ByteArrayOutputStream(HttpServerMain.BUFSIZE); // Buffer-size is a good initial size
		try {
			outResp.write(me_200_OK_STARTER.getBytes());
			byte tmpFileData[] = Files.readAllBytes(new File(theRequest.getAbsolutePath()).toPath());
			outResp.write((me_CONTENT_LENGTH + tmpFileData.length + "\r\n").getBytes());
			if (theRequest.getAbsolutePath().toLowerCase().endsWith(".htm") || theRequest.getAbsolutePath().toLowerCase().endsWith(".html")) { // If html file
				outResp.write(me_HTML_CONTENT.getBytes());
			} else if (theRequest.getAbsolutePath().toLowerCase().endsWith(".png")) { // If png file
				outResp.write(me_PNG_CONTENT.getBytes());
			} else if (theRequest.getAbsolutePath().toLowerCase().endsWith(".txt")) {
				outResp.write(me_TXT_CONTENT.getBytes());
			} else { // Unknown file
				return create415UnsupportedMediaType();
				//outResp.write(me_OTHER_CONTENT.getBytes());
			}
			outResp.write(tmpFileData);
		} catch (IOException e) {
			return this.create403Forbidden(); // If 'IOException' thrown then reading is forbidden on the file (since it is already exists)
		}
		return outResp.toByteArray();
	}

	private byte[] create201Created(MyRequest theRequest) {
		ByteArrayOutputStream outResp = new ByteArrayOutputStream(HttpServerMain.BUFSIZE); // Buffer-size is a good initial size
		try {
			if (this.meIsNewCopy) { // In case of POST upload and a new copy is needed
				String tmpNew = theRequest.getAbsolutePath().substring(0, theRequest.getAbsolutePath().lastIndexOf('/') + 1) + "copy-" + UUID.randomUUID().toString() + "-" + theRequest.getAbsolutePath().substring(theRequest.getAbsolutePath().lastIndexOf('/') + 1);
				Files.write(new File(tmpNew).toPath(), theRequest.getPayloadData());
				outResp.write(me_201_CREATED_STARTER.getBytes());
				outResp.write((theRequest.getRelativePath().substring(0, theRequest.getRelativePath().lastIndexOf('/') + 1) + tmpNew.substring(tmpNew.lastIndexOf('/') + 1)).getBytes());
			} else { // Normal situation of POST or PUT
				Files.write(new File(theRequest.getAbsolutePath()).toPath(), theRequest.getPayloadData());
				outResp.write(me_201_CREATED_STARTER.getBytes());
				outResp.write(theRequest.getRelativePath().getBytes());
			}
			outResp.write("\r\n\r\n".getBytes());
		} catch (IOException e) {
			return this.create403Forbidden(); // If 'IOException' thrown then writing is forbidden on the file (since it is already exists)
		}
		return outResp.toByteArray();
	}

	private byte[] create204NoContent(MyRequest theRequest) {
		try {
			if (theRequest.getRequestType() == RequestType.PUT) { // If PUT then truncate (overwrite the file)
				Files.write(new File(theRequest.getAbsolutePath()).toPath(), theRequest.getPayloadData());
			} else { // Then it's POST (append to file)
				Files.write(new File(theRequest.getAbsolutePath()).toPath(), theRequest.getPayloadData(), StandardOpenOption.APPEND);
			}
		} catch (IOException e) {
			return this.create403Forbidden(); // If 'IOException' thrown then writing is forbidden on the file (since it is already exists)
		}
		return me_204_NO_CONTENT.getBytes();
	}

	private byte[] create403Forbidden() {
		return me_403_FORBIDDEN.getBytes();
	}

	private byte[] create404NotFound() {
		return me_404_NOT_FOUND.getBytes();
	}

	private byte[] create500InternalServerError() {
		return me_500_INTERNAL_SERVER_ERROR.getBytes();
	}

	private byte[] create400BadRequest() {
		return me_400_BAD_REQUEST.getBytes();
	}

	private byte[] create501NotImplemented() {
		return me_501_NOT_IMPLEMENTED.getBytes();
	}

	private byte[] create503ServiceUnavailable() {
		return me_503_SERVICE_UNAVAILABLE.getBytes();
	}

	private byte[] create405NotAllowed() { return me_405_METHOD_NOT_ALLOWED.getBytes(); }

	private byte[] create505WrongHTTPVer() { return me_505_HTTP_NOT_SUPPORTED.getBytes(); }

	private byte[] create415UnsupportedMediaType() { return me_415_UNSUPPORTED_MEDIA_TYPE.getBytes(); }

	// ^^^^^^^^^^^^^^^^^^^^  End Private Section  ^^^^^^^^^^^^^^^^^^^^ //
}
