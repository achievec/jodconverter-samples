package org.jodconverter.sample.webapp;

import javax.servlet.ServletContext;

import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;

/** Web context. */
@SuppressWarnings("PMD")
public class WebappContext {

  private static final String PARAMETER_OFFICE_PORT = "office.port";
  private static final String PARAMETER_OFFICE_HOME = "office.home";
  private static final String PARAMETER_OFFICE_PROFILE = "office.profile";
  private static final String PARAMETER_FILEUPLOAD_FILE_SIZE_MAX = "fileupload.fileSizeMax";

  private static final Logger LOGGER = LoggerFactory.getLogger(WebappContext.class);
  private static final String KEY = WebappContext.class.getName();

  private final ServletFileUpload fileUpload;
  private final OfficeManager officeManager;
  private final DocumentConverter documentConverter;

  /**
   * Creates a new WebappContext using the specified servlet context.
   *
   * @param servletContext the servlet context that contains properties used to create a JOD
   *     document converter.
   */
  public WebappContext(final ServletContext servletContext) {
    final DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
    final String fileSizeMax = servletContext.getInitParameter(PARAMETER_FILEUPLOAD_FILE_SIZE_MAX);
    fileUpload = new ServletFileUpload(fileItemFactory);
    if (fileSizeMax == null) {
      LOGGER.warn("max file upload size not set");
    } else {
      fileUpload.setFileSizeMax(Integer.parseInt(fileSizeMax));
      LOGGER.info("max file upload size set to {}", fileSizeMax);
    }

    final LocalOfficeManager.Builder builder = LocalOfficeManager.builder();
    final String officePortParam = servletContext.getInitParameter(PARAMETER_OFFICE_PORT);
    if (officePortParam != null) {
      builder.portNumbers(Integer.parseInt(officePortParam));
    }
    final String officeHomeParam = servletContext.getInitParameter(PARAMETER_OFFICE_HOME);
    builder.officeHome(officeHomeParam);
    final String officeProfileParam = servletContext.getInitParameter(PARAMETER_OFFICE_PROFILE);
    builder.templateProfileDir(officeProfileParam);

    officeManager = builder.build();
    documentConverter = LocalConverter.make(officeManager);
  }

  protected static void init(final ServletContext servletContext) throws OfficeException {
    final WebappContext instance = new WebappContext(servletContext);
    servletContext.setAttribute(KEY, instance);
    instance.officeManager.start();
  }

  protected static void destroy(final ServletContext servletContext) throws OfficeException {
    final WebappContext instance = get(servletContext);
    instance.officeManager.stop();
  }

  /**
   * Gets the WebappContext from the specified servlet context.
   *
   * @param servletContext the servlet context that contains the WebappContext.
   * @return the WebappContext.
   */
  public static WebappContext get(final ServletContext servletContext) {
    return (WebappContext) servletContext.getAttribute(KEY);
  }

  /**
   * Gets the object used to process file uploads.
   *
   * @return a ServletFileUpload.
   */
  public ServletFileUpload getFileUpload() {
    return fileUpload;
  }

  /**
   * Gets the document converter of the context.
   *
   * @return the context's document converter.
   */
  public OfficeManager getOfficeManager() {
    return officeManager;
  }

  /**
   * Gets the document converter of the context.
   *
   * @return the context's document converter.
   */
  public DocumentConverter getDocumentConverter() {
    return documentConverter;
  }
}
