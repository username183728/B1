# Bit AI Step 6

Bit sekarang memakai BitToolRegistry untuk mencocokkan pertanyaan dengan homeTools. Registry hanya memuat ID/nama/alias tool. Token GitHub tidak masuk ke registry atau Bit.

Contoh: "Upload ZIP ini ke GitHub" -> githubzip -> openTool("githubzip") -> GitHub Publisher.
Pertanyaan yang tidak cocok tetap memakai Google fallback.
