# Bit AI Step 7

Step 7 menambahkan BitRuntimeContext.

Bit dapat mengingat metadata non-rahasia tentang tool terakhir yang dibuka. Karena itu perintah seperti "yang tadi", "buka lagi", "coba lagi", dan "lanjutkan" dapat diarahkan kembali ke tool terakhir.

Yang disimpan hanya ID/nama tool, perintah terakhir (dipotong 500 karakter), dan status sederhana. Tidak ada token, password, credential, atau isi rahasia GitHub yang disimpan di konteks Bit.
