package com.mozy.mobile.android.web.containers;
import java.io.File;

public class SyzygyFileUploadState extends FileUploadState {
	public SyzygyFileUploadState(File file, String fileName, long fileLength, FileUploadState originFile) {
		super(file, fileName, fileLength);
		// TODO Auto-generated constructor stub
		this.finalFileSize = fileLength;
		this.originFile = originFile;
	}
	private FileUploadState originFile;
	public long finalFileSize;
	
	@Override
	public void setLinkToCloud(String linkToCloud) {
		super.setLinkToCloud(linkToCloud);
		this.originFile.linkToCloud = linkToCloud;
	}
	
	@Override
	public long getFinalFileSize() {
		return this.finalFileSize;
	}
}
