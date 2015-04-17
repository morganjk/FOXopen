package net.foxopen.fox.command.util;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoResult;
import net.foxopen.fox.download.DownloadLinkXDoResult;
import net.foxopen.fox.download.DownloadMode;
import net.foxopen.fox.download.DownloadParcel;
import net.foxopen.fox.download.DownloadServlet;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.ResponseOverride;
import net.foxopen.fox.thread.storage.TempResource;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;

import java.sql.Blob;
import java.sql.Clob;

/**
 * GeneratorDestination for a file download, of either a BLOB or CLOB query. The download may be served as a popup or
 * directly as the main response. This handles the download's filename, content type and download mode, etc.
 */
public class DownloadGeneratorDestination
implements GeneratorDestination {

  private final String mFileNameXPath;
  private final String mContentType;
  private final String mDispositionAttachmentXPath;
  private final String mServeAsResponseXPath;
  private final String mExpires;

  public DownloadGeneratorDestination(String pFileName, String pContentType, String pDispositionAttachmentXPath, String pServeAsResponseXPath, String pExpires) {
    mFileNameXPath = pFileName;
    mContentType = pContentType;
    mDispositionAttachmentXPath = XFUtil.nvl(pDispositionAttachmentXPath, "false()");
    mServeAsResponseXPath = XFUtil.nvl(pServeAsResponseXPath, "false()");
    mExpires = pExpires;
  }

  @Override
  public void generateToWriter(ActionRequestContext pRequestContext, WriterGenerator pGenerator) {

    TempResource<Clob> lClobTempResource = pRequestContext.getTempResourceProvider().getClobTempResource();
    GeneratorDestinationUtils.clobWriteToWFSL(pRequestContext, pGenerator, lClobTempResource);

    addDownloadResult(pRequestContext, lClobTempResource);
  }

  @Override
  public void generateToOutputStream(ActionRequestContext pRequestContext, OutputStreamGenerator pGenerator) {

    TempResource<Blob> lBlobTempResource = pRequestContext.getTempResourceProvider().getBlobTempResource();
    GeneratorDestinationUtils.blobWriteToWFSL(pRequestContext, pGenerator, lBlobTempResource);

    addDownloadResult(pRequestContext, lBlobTempResource);
  }

  @Override
  public void generateToDOM(ActionRequestContext pRequestContext, DOMGenerator pGenerator) {
    //A DOM is not a valid download destination - consumers may wish to serialise a DOM to a Writer/OutputStream using DOM methods
    throw new ExInternal("Generate to DOM not currently supported by Downloads");
  }

  private String getFileName(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    try {
      return lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mFileNameXPath);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate filename XPath for download attribute", e);
    }
  }

  /**
   * Adds an appropriate XDoResult to the given request context based on the disposition XPaths defined in this object.
   * Only invoke this method if you have not used one of the generateToXXX methods to perform download generation
   * (those methods invoke this one automatically). Use this signature if the filename can be generated by this object.
   * @param pRequestContext
   * @param pWFSL WFSL to be downloaded.
   */
  public void addDownloadResult(ActionRequestContext pRequestContext, WorkingFileStorageLocation pWFSL) {
    String lFileName = getFileName(pRequestContext);
    DownloadParcel lDownloadParcel = pRequestContext.getDownloadManager().addDownload(pWFSL, lFileName, mContentType);
    addDownloadResult(pRequestContext, lDownloadParcel, lFileName);
  }

  /**
   * Adds an appropriate XDoResult to the given request context based on the disposition XPaths defined in this object.
   * Only invoke this method if you have not used one of the generateToXXX methods to perform download generation
   * (those methods invoke this one automatically).
   * @param pRequestContext
   * @param pDownloadParcel Externally created DownloadParcel to be downloaded.
   * @param pFileName Filename as determined externally.
   */
  public void addDownloadResult(ActionRequestContext pRequestContext, DownloadParcel pDownloadParcel, String pFileName) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    boolean lServeAsResponse;
    DownloadMode lDownloadMode;
    try {
      lDownloadMode = lContextUElem.extendedXPathBoolean(lContextUElem.attachDOM(), mDispositionAttachmentXPath) ? DownloadMode.ATTACHMENT : DownloadMode.INLINE;
      lServeAsResponse = lContextUElem.extendedXPathBoolean(lContextUElem.attachDOM(), mServeAsResponseXPath);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate XPath for download attribute", e);
    }

    //Add an appropriate response- either an override or a download link popup
    XDoResult lXDoResult;
    if(lServeAsResponse) {
      //Ask the download servlet to serve the download to the response immediately rather than waiting for a separate GET request
      lXDoResult = new ResponseOverride(DownloadServlet.streamDownloadToResponse(pRequestContext.getFoxRequest(), pRequestContext.getContextUCon(), pDownloadParcel, lDownloadMode));
    }
    else {
      lXDoResult = new DownloadLinkXDoResult(pDownloadParcel.getParcelId(), pRequestContext.getDownloadManager().generateURL(pRequestContext, pDownloadParcel, lDownloadMode), pFileName);
    }

    pRequestContext.addXDoResult(lXDoResult);
  }
}
