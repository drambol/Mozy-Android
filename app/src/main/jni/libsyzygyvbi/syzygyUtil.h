#ifndef __syzygy__util__
#define __syzygy__util__

#include <string>

int restore(std::string inpath, std::string outpath, std::string passphrase);

int baseline(std::string inpath, std::string outpath, std::string passphrase);

std::string compressUserKey(const std::string& key);

#endif
