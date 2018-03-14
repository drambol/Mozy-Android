#include "syzygyUtil.h"

#include "syzygyvbi/disk_format.h"
#include "syzygyvbi/wire_format.h"
#include "syzygyvbi/syzygy_stream.h"
#include "syzygyvbi/segmenter.h"
#include "syzygyvbi/segmenterfbi.h"
#include "syzygyvbi/common/syzygy_exception.h"

#include <android/log.h>
#include <fstream>

int restore(std::string inpath, std::string outpath, std::string passphrase)
{
	int ret = 0;
	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI","%s: %d enter", __FUNCTION__, __LINE__);

	std::ifstream in(inpath.c_str());
	std::ofstream out(outpath.c_str());

	try {
		syzygy::WirePatchReader wpr(in, std::string(passphrase.c_str(), 48));
		syzygy::FilePatchTrailer toc = wpr.readHeader();


		for (int i = 0; i < toc.chunk_size(); i++) {
			; //TODO: support reference area
		}

		syzygy::CompressedChunk chunk_info;
		Mordor::Buffer decoded;

		while (wpr.readChunk(toc.encryption(), chunk_info, decoded)) {
			out.write(decoded.buf(), decoded.readAvailable());
			decoded.clear();
		}
	} catch (syzygy::SyzygyException e) {
		__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI","%s: %d cannot decrypt, err_code = %d", __FUNCTION__, __LINE__, e.err_code);
		ret = e.err_code;
	} catch (...) {
		__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI","%s: %d cannot decrypt", __FUNCTION__, __LINE__);
		ret = syzygy::UNKNOWN_ERROR;
	}


	in.close();
	out.close();

	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI","%s: %d exit", __FUNCTION__, __LINE__);
	return ret;
}


int baseline(std::string inpath, std::string outpath, std::string passphrase)
{
	int ret = 0;
	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI","%s: %d enter", __FUNCTION__, __LINE__);

	std::ifstream in(inpath.c_str());
	std::ofstream out(outpath.c_str());

	try {
		VBI::Segmenter *seg = new VBI::SegmenterFBI;
		syzygy::DiskPatchWriter dpw(out,
		                syzygy::Preferences(syzygy::COMPRESS_GZIP, syzygy::ENCRYPT_AES_TETHYS, std::string(passphrase.c_str(), 32))
		    );
		syzygy::SignatureStream ss(seg);
		ss.proposeChunkSize(8192); //TODO: get the number from para which is read from config
		ss.patchWriter(&dpw);
		char buf[2048];
		while (in.read(buf, sizeof (buf))) {
			size_t read = in.gcount();
			while (read > 0) {
				read -= ss.write(buf + sizeof(buf) - read, read);
			}
		}
		ss.write(buf, in.gcount());
		ss.close();
		delete seg;
	} catch (syzygy::SyzygyException e) {
		__android_log_print(ANDROID_LOG_DEBUG,
				"SyzygyVbiAPI","%s: %d cannot create syzygy file, err_code = %d", __FUNCTION__, __LINE__, e.err_code);
		ret = e.err_code;
	} catch (...) {
		__android_log_print(ANDROID_LOG_DEBUG,
				"SyzygyVbiAPI","%s: %d cannot create syzygy file", __FUNCTION__, __LINE__);
		ret = syzygy::UNKNOWN_ERROR;
	}


	in.close();
	out.close();

	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI","%s: %d exit", __FUNCTION__, __LINE__);
	return ret;
}

std::string compressUserKey(const std::string& key)
{
	return compressUserKeyV3(key, 48);
}
