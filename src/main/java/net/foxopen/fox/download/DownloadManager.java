package net.foxopen.fox.download;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.filetransfer.UploadInfo;
import net.foxopen.fox.module.UploadedFileInfo;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.storage.FileStorageLocation;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;

import java.util.List;


/**
 * Class for creating and storing DownloadParcels and managing donwload URLs.
 */
public interface DownloadManager {

  /**
   * Generates the download URL for a parcel. The URL will not include the download mode - if this signature is used,
   * it is the consumer's responsibility to add this manually.
   * @param pRequestContext For URI generation.
   * @param pDownloadParcel Parcel to generate URL for.
   * @return Download URL for the given parcel.
   */
  String generateURL(RequestContext pRequestContext, DownloadParcel pDownloadParcel);

  /**
   * Generates a download URL for the given parcel, including the download mode.
   * @param pRequestContext For URI generation.
   * @param pDownloadParcel Parcel to generate URL for.
   * @param pDownloadMode Download mode of the generated URL.
   * @return Download URL for the given parcel.
   */
  String generateURL(RequestContext pRequestContext, DownloadParcel pDownloadParcel, DownloadMode pDownloadMode);

  /**
   * Gets the DownloadParcel corresponding to the given ID. If the parcel cannot be found, an error is raised.
   * @param pParcelId
   * @return
   */
  DownloadParcel getDownloadParcel(String pParcelId);

  UploadedFileInfo addFileDownload(RequestContext pRequestContext, FileStorageLocation pFileStorageLocation, DOM pUploadTargetDOM, ContextUElem pContextUElem);

  UploadedFileInfo addFileDownload(RequestContext pRequestContext, WorkingFileStorageLocation pWFSL, String pUploadContainerDOMRef, UploadInfo pUploadInfo);

  DownloadParcel addDownload(WorkingFileStorageLocation pWFSL, String pFilename, String pContentType);

  DownloadParcel addQueryDownload(ExecutableQuery pExecutableQuery, String pFilename);

  DownloadParcel addZipQueryDownload(List<ExecutableQuery> pExecutableQueryList, String pFilename, int pZipCompressionLevel);

}
