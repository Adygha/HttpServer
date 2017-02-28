package httpServer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import httpServer.HttpServerMain;

/**
 * A class that represents an HTTP request.
 */
public class MyRequest {

	/**
	 * An enumeration to specify the type of the HTTP requests that this server can handle
	 */
	public static enum RequestType {

		/**
		 * Specify a request that caused and internal server error.
		 */
		ERROR,

		/**
		 * Specify when a request comes when the server is under maintenance (paused).
		 */
		PAUSE,

		/**
		 * Specify a bad structured request.
		 */
		BAD,

		/**
		 * We designated DELETE as not allowed, and the rest not implemented
		 */
		NOT_ALLOWED,

		/**
		 * Specify a known but not-implemented HTTP request (HEAD, etc.. and
		 * will result in a '501 Not Implemented' response).
		 */
		NOT_IMP,

		/**
		 * If wrong HTTP version
		 */
		WRONG_HTTP,

//		/**
//		 * Media type not supported. .JPG etc
//		 */
//		MEDIA_UNSUPPORTED,

		/**
		 * Specify an HTTP GET request.
		 */
		GET,

		/**
		 * Specify an HTTP PUT request.
		 */
		PUT,

		/**
		 * Specify an HTTP GET request.
		 */
		POST
	}

	/**
	 * An enumeration to determine if the provided URL (path) by the request is a folder, file, or non-existing path.
	 */
	public static enum PathType {

		/**
		 * Specifies if the URL leads to not existing file/directory.
		 */
		NOT_EXIST,

		/**
		 * Specifies if the URL leads to forbidden file/directory.
		 */
		FORBIDDEN,

		/**
		 * Specifies if the URL leads to existing directory.
		 */
		DIRECTORY,

		/**
		 * Specifies if the URL leads to existing file.
		 */
		FILE
	}

	// Fields
	private RequestType meReqType;
	private String meRelPath;
	private String meAbsPath;
	private PathType mePathType;
	private byte[] mePayloadData;

	/**
	 * Constructor.
	 * @param reqString the entire string of the request
	 */
	public MyRequest(String reqString) {
		String tmpArr[] = reqString.split(" ", 3);

		//Cut the HTTP version part and check if it's our supported version
		String httpVerCheck[] = tmpArr[2].split("\n");
		if(!"HTTP/1.1".equals(httpVerCheck[0].trim())){ // Check for HTTP version (only 1.1 is accepted)
			if (httpVerCheck[0].trim().startsWith("HTTP/")) {
				this.meReqType = RequestType.WRONG_HTTP; // If valid HTTP but wrong version
				return;
			}
			this.meReqType = RequestType.BAD;  // We were left with invalid HTTP request then
			return;
		}

		//System.out.println(tmpArr[0]+", "+tmpArr[1]+", "+tmpArr[2].split("\n"));
		this.meRelPath = tmpArr[1]; // Keep original path as it is, in case it is needed
		this.meAbsPath = HttpServerMain.getAbsoluteUrlPath(this.meRelPath);
		switch (tmpArr[0]) { // Classify the request type
			case "ERROR":
				this.meReqType = RequestType.ERROR;
				return;
			case "PAUSE":
				this.meReqType = RequestType.PAUSE;
				return;
			case "GET":
				this.meReqType = RequestType.GET;
				break;
			case "PUT":
				this.meReqType = RequestType.PUT;
				this.setPayloadData(reqString);
				break;
			case "POST":
				this.meReqType = RequestType.POST;
				this.setPayloadData(reqString);
				break;
			case "DELETE":
				this.meReqType = RequestType.NOT_ALLOWED;
				break;
			case "OPTIONS":
			case "HEAD":
			case "TRACE":
			case "CONNECT":
				this.meReqType = RequestType.NOT_IMP;
				break;
			default:
				this.meReqType = RequestType.BAD;
		}
		this.checkPaths();
	}

	// To check and classify the path type, and change the absolute path to
	// add 'index.htm' or 'index.html' if exist (only in case of GET)
	private void checkPaths() { // Using both Paths and Files in this method gives more control to catch the forbidden situations
		if (this.meAbsPath.startsWith(HttpServerMain.getAbsoluteUrlPath("/"))) {
			Path tmpPath = Paths.get(new File(this.meAbsPath).getAbsolutePath()); // To get specific OS path
			if (Files.exists(tmpPath)) {
				if (Files.isDirectory(tmpPath)) {
					 // The next check is better than using 'startsWith' to avoid files like 'index.htmAA'
					if (this.meReqType == RequestType.GET && Files.exists(Paths.get(tmpPath.toString(), "index.htm"))) {
						this.meAbsPath += this.meAbsPath.endsWith("/") ? "index.htm" : "/index.htm"; // Just in case
						this.checkPaths();
						return;
					} else if (this.meReqType == RequestType.GET && Files.exists(Paths.get(tmpPath.toString(), "index.html"))) {
						this.meAbsPath += this.meAbsPath.endsWith("/") ? "index.html" : "/index.html"; // Just in case
						this.checkPaths();
						return;
					}
					this.mePathType = PathType.DIRECTORY;
				} else if (!tmpPath.toFile().canRead()) { // There seem to be a bug (http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6203387) that prevents the detection
					this.mePathType = PathType.FORBIDDEN;
				} else {
					this.mePathType = PathType.FILE;
				}
			} else if (!Files.notExists(tmpPath)) { // If 'exists' and 'notExists' return 'false' then the access to file is forbidden
				this.mePathType = PathType.FORBIDDEN;
			} else {
				this.mePathType = PathType.NOT_EXIST;
			}
		} else {
			this.mePathType = PathType.FORBIDDEN;
		}
	}

	// Privately set the payload data from the request string
	private void setPayloadData(String reqString) {
		if (reqString.contains("boundary=")) { // To extract according the boundary
			String tmpStr = reqString.substring(reqString.indexOf("boundary=") + 9); // Extract partially
			String tmpBnd = "\r\n--" + tmpStr.substring(0, tmpStr.indexOf("\r\n")); // Get the boundary value
			tmpStr = tmpStr.substring(tmpStr.indexOf(tmpBnd)  + tmpBnd.length(), tmpStr.lastIndexOf(tmpBnd)); // Extract between bounds
			tmpStr = tmpStr.substring(tmpStr.indexOf("\r\n\r\n") + 4); // Final extract
			if (this.getRequestType() == RequestType.POST) { // If it is POST then get the file name (depending on 'name' parameter)
				String tmpFile = reqString.split("name=\"")[1].split("\";")[0];
				//this.meRelPath = this.meRelPath.substring(0, this.meRelPath.lastIndexOf('/') + 1) + tmpFile;
				//this.meAbsPath = this.getAbsolutePath().substring(0, this.getAbsolutePath().lastIndexOf('/') + 1) + tmpFile;
				this.meRelPath = this.meRelPath.substring(0, this.meRelPath.lastIndexOf('/') + 1) + tmpFile;
				this.meAbsPath = HttpServerMain.getAbsoluteUrlPath(this.meRelPath);
			}
			this.mePayloadData = tmpStr.getBytes();
		} else { // Then it's POST (small texts)
			this.mePayloadData = reqString.substring(reqString.indexOf("\r\n\r\n") + 4).replaceAll("(?<!\r)\n", "\r\n").getBytes();//Replace LF with CRLF for Windows readability
		}
	}
	
	/**
	 * Returns the request's payload data if applicable for the request type.
	 * @param reqString	the request's string
	 * @return			the request's payload data
	 */
	public byte[] getPayloadData() {
		return this.mePayloadData;
	}

	/**
	 * Returns the type of this HTTP request object.
	 * @return	the type of this HTTP request object
	 */
	public RequestType getRequestType() {
		return this.meReqType;
	}

	/**
	 * Returns the original relative path of this HTTP request object (in case it is required and applicable for the request).
	 * @return	the original path of this HTTP request object if exists
	 */
	public String getRelativePath() {
		return this.meRelPath;
	}

	/**
	 * Returns the full path (in the server's machine) of this HTTP request object (in case it is applicable for the request).
	 * @return the the full path (in the server's machine) of this HTTP request object if exists
	 */
	public String getAbsolutePath() {
		return this.meAbsPath;
	}

	/**
	 * Get the type of the path (NOT_EXIST, FORBIDDEN, DIRECTORY, or FILE) of this HTTP request object (in case
	 * it is applicable for the request).
	 * @return	type of the path of this HTTP request object if exists
	 */
	public PathType getPathType() {
		return this.mePathType;
	}
}
