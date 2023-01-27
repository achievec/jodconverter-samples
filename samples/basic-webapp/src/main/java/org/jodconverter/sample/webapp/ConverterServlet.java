package org.jodconverter.sample.webapp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.util.FileUtils;

/** Converter servlet. */
public class ConverterServlet extends HttpServlet {
  private static final long serialVersionUID = -591469426224201748L;

  private static final Logger LOGGER = LoggerFactory.getLogger(ConverterServlet.class);

  @Override
  public void init() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Servlet {} has started", this.getServletName());
    }
  }

  @Override
  public void destroy() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Servlet {} has stopped", this.getServletName());
    }
  }

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {

    if (!ServletFileUpload.isMultipartContent(request)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only multipart requests are allowed");
      return;
    }

    final WebappContext webappContext = WebappContext.get(getServletContext());

    final FileItem uploadedFile;
    try {
      uploadedFile = getUploadedFile(webappContext.getFileUpload(), request);
    } catch (FileUploadException fileUploadException) {
      throw new ServletException(fileUploadException);
    }
    if (uploadedFile == null) {
      throw new ServletException("Uploaded file is null");
    }
    final String inputExtension = FileUtils.getExtension(uploadedFile.getName());

    final String baseName = Objects.requireNonNull(FileUtils.getBaseName(uploadedFile.getName()));
    final File inputFile = File.createTempFile(baseName, "." + inputExtension);
    FileUtils.deleteQuietly(inputFile);
    writeUploadedFile(uploadedFile, inputFile);

    final String outputExtension =
        Objects.requireNonNull(FileUtils.getExtension(request.getRequestURI()));
    final File outputFile = File.createTempFile(baseName, "." + outputExtension);
    FileUtils.deleteQuietly(outputFile);
    try {
      final DocumentConverter converter = webappContext.getDocumentConverter();
      final long startTime = System.currentTimeMillis();
      converter.convert(inputFile).to(outputFile).execute();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(
            String.format(
                "Successful conversion: %s [%db] to %s in %dms",
                inputExtension,
                inputFile.length(),
                outputExtension,
                System.currentTimeMillis() - startTime));
      }
      response.setContentType(
          Objects.requireNonNull(
                  converter.getFormatRegistry().getFormatByExtension(outputExtension))
              .getMediaType());
      response.setHeader(
          "Content-Disposition", "attachment; filename=" + baseName + "." + outputExtension);
      sendFile(outputFile, response);
    } catch (Exception exception) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(
            String.format(
                "Failed conversion: %s [%db] to %s; %s; input file: %s",
                inputExtension,
                inputFile.length(),
                outputExtension,
                exception,
                inputFile.getName()));
      }
      throw new ServletException("Conversion failed", exception);
    } finally {
      FileUtils.deleteQuietly(outputFile);
      FileUtils.deleteQuietly(inputFile);
    }
  }

  private void sendFile(final File file, final HttpServletResponse response) throws IOException {

    response.setContentLength((int) file.length());
    Files.copy(file.toPath(), response.getOutputStream());
  }

  private void writeUploadedFile(final FileItem uploadedFile, final File destinationFile)
      throws ServletException {

    try {
      uploadedFile.write(destinationFile);
    } catch (Exception exception) {
      throw new ServletException("Error writing uploaded file", exception);
    }
    uploadedFile.delete();
  }

  private FileItem getUploadedFile(
      final ServletFileUpload fileUpload, final HttpServletRequest request)
      throws FileUploadException {

    return fileUpload.parseRequest(request).stream()
        .filter(fileItem -> !fileItem.isFormField())
        .findFirst()
        .orElse(null);
  }
}
