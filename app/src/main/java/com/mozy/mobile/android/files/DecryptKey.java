package com.mozy.mobile.android.files;


public class DecryptKey
{
        private byte [] m_key;
        private String m_scheme;
        
        
        public DecryptKey()
        {
            m_key    = null;
            m_scheme  = null;
        }

        public DecryptKey(final byte [] key, final String scheme)
        {
            m_key    = key;
            m_scheme  = scheme;
        }
    
        
        public byte[] get_key() {
            return m_key;
        }

        public void set_key(byte[] m_key) {
            this.m_key = m_key;
        }

        public String get_scheme() {
            return m_scheme;
        }

        public void set_scheme(String m_scheme) {
            this.m_scheme = m_scheme;
        }
}
